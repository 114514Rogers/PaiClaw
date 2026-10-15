# PaiFlow 面试题口述版

## 问题一：PaiFlow 的工作流程是怎样的？

PaiFlow 整体是一个前后端分离的架构，分成四层：前端编排层、Console Hub 网关层、Workflow 引擎层，以及底层的基础设施。我从用户视角把整个链路串一遍。

用户在前端页面上，通过一个基于 ReactFlow 的可视化编辑器来拖拽节点、连线，比如拖一个 LLM 节点、一个 TTS 节点、一个条件判断节点，把它们连起来就形成了一个 DAG。编排完成后，用户点击运行，前端会发一个 `POST /workflow/chat` 请求到 Console Hub，Hub 是一个 Spring Boot 服务跑在 8080 端口，它负责鉴权、校验，然后把请求转发给真正的 Workflow 引擎，引擎跑在 7880 端口。

引擎收到请求后，核心逻辑在 `WorkflowEngine.execute()` 这个方法里。它首先会做一个校验，用 Kahn 算法做拓扑排序来检测有没有环，同时检查是不是有合法的 START 和 END 节点。校验通过后，它会调用 `buildNodeExecuteChain()` 从边列表构建出邻接表，把每个节点的前驱节点、后继节点、以及异常分支节点都建立好关系。

然后就进入执行阶段了。引擎从 START 节点开始，递归地调用 `executeNode()`。每个节点执行前会先检查它所有的前驱节点是不是都完成了，如果完成了，就通过策略模式拿到对应的 `NodeExecutor` 来执行。比如 LLM 节点就用 `LLMNodeExecutor`，它会先用 `VariableTemplateRender` 把 prompt 模板里的 `{{nodeId.varName}}` 替换成上游节点的实际输出，然后调用大模型服务拿结果。再比如插件节点，会路由到具体的插件实现，像 TTS 语音合成就会走 `SmartTTSIntegration`，通过 WebSocket 连接语音服务，把文本转成音频再上传到 MinIO。

节点之间的数据传递靠的是一个叫 `VariablePool` 的结构，本质上是一个 `Map<String, Map<String, Object>>`，key 是节点 ID，value 是这个节点的输出变量。下游节点通过模板语法引用上游的输出，引擎在执行前会自动解析和替换。

整个执行过程是通过 SSE（Server-Sent Events）实时推流给前端的。引擎里有一个 `WorkflowMsgCallback`，它会在工作流开始、每个节点开始执行、节点产出中间结果（比如 LLM 逐 token 输出）、节点完成、工作流结束这些时机，通过 `SseEmitter` 把事件推给前端，前端就能实时展示每个节点的运行状态和输出内容。

另外如果工作流里有并行分支，引擎还有一个 `ParallelWorkflowEngine`，它用 `CachedThreadPool` 配合 `CompletableFuture` 来并行执行多个分支，同时用 `TransmittableThreadLocal` 保证线程间的上下文传递不丢失。

---

## 问题二：高并发时可能存在什么问题？

这个问题我从几个维度来说。

**第一个是内存层面的风险。** 刚才提到的 `VariablePool`，它在代码里其实是一个全局共享的静态 `ConcurrentHashMap`，所有并发执行的工作流的中间变量都往里面堆。代码里甚至有一条 FIXME 注释写着"这里需要调整为实例级别，全局共享会存在 OOM 风险"。如果同时有大量工作流在跑，每个工作流有十几个节点，每个节点都往这个 map 里写输出，而且没有任何淘汰策略，内存迟早撑爆。另外工作流里如果有 TTS 这种音频合成节点，音频数据是在内存里用 `ByteArrayOutputStream` 拼装的，一个音频可能几兆，并发一多这部分内存占用也很可观。

**第二个是线程资源层面。** `ParallelWorkflowEngine` 用的是 `Executors.newCachedThreadPool()`，这个线程池理论上可以无限创建线程，没有上限。虽然 `AsyncUtil` 里配了 max=50，但它用的是 `SynchronousQueue`，不会缓冲任务，超出的任务要么立即创建新线程，要么走 `CallerRunsPolicy` 由调用方线程执行。高并发下线程数会飙升，每个线程占 1MB 左右的栈内存，几百个线程就是几百 MB。

**第三个是连接层面。** 每个用户执行工作流都会建立一个 SSE 长连接，工作流引擎那边超时设的是 10 分钟，Console Hub 那边更长，设的是 30 分钟。每个 SSE 连接都会占一个 Tomcat 线程和一个 TCP 连接。如果同时有几百个用户在看工作流执行结果，Tomcat 的连接池很快就会耗尽，后续请求就进不来了。

**第四个是下游服务的压力。** 工作流里的 LLM 节点会调大模型 API，这些 API 本身有限流，高并发时如果没有做背压控制，大量请求打到大模型服务，要么被限流报错，要么响应变慢拖垮整条链路。

---

## 问题三：音频文件占内存，并发高了怎么处理？

这个问题的核心是大二进制数据不应该长时间驻留在 JVM 堆内存里。我先说一下现在代码里是怎么做的，然后说优化方案。

现在的实现是这样的，以 `SmartTTSIntegration` 为例，它通过 WebSocket 连接语音合成服务，服务端会分多个 chunk 把音频数据返回来，每个 chunk 是 base64 编码的。客户端这边用一个自定义的 `ByteArrayOutputStream` 来接收，初始容量 1024 字节，容量不够了就翻倍扩容。等所有 chunk 都收完了，调用 `toByteArray()` 拿到完整的音频字节数组，再一次性上传到 MinIO。代码大概长这样：

```java
// 当前实现：所有 chunk 在内存中拼装
ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream(1024);
// WebSocket 每收到一个 chunk
audioBuffer.write(decodedChunkBytes);
// 最终拿到完整音频
byte[] fullAudio = audioBuffer.toByteArray();
// 上传到 MinIO
s3Client.uploadObject(objectKey, "audio/mpeg", fullAudio);
```

这个方案在并发低的时候没问题，但你想，假设一个音频平均 2MB，同时有 100 个 TTS 任务在跑，光音频数据就要占 200MB 堆内存，而且 `toByteArray()` 还会 copy 一份，峰值可能翻倍到 400MB，再加上 GC 压力，很容易触发 Full GC 甚至 OOM。

**我的优化思路是让音频数据"流过去"而不是"攒起来"。**

**首选方案是用管道流直接边收边传到 MinIO。** 其实项目里的 `S3ClientUtil` 已经提供了接受 `InputStream` 的上传方法，objectSize 传 -1 的时候 MinIO SDK 会自动按 5MB 分片上传（见 `S3ClientUtil` 第 224 行）。现在 TTS 调的是 `byte[]` 版本（第 211 行），只要换成流式版本就行。具体做法是用 `PipedOutputStream` 和 `PipedInputStream`，WebSocket 接收线程往 pipe 里写，另一个线程从 pipe 里读并上传到 MinIO。数据像水管一样流过去，既不落盘也不驻留内存，改动量很小：

```java
// 优化：管道流，边收边传 MinIO
PipedOutputStream pipedOut = new PipedOutputStream();
PipedInputStream pipedIn = new PipedInputStream(pipedOut);

// 上传线程：从 pipe 读，流式传 MinIO（objectSize=-1 自动分片）
CompletableFuture<String> uploadFuture = CompletableFuture.supplyAsync(() -> {
    return s3ClientUtil.uploadObject(objectKey, "audio/mpeg", pipedIn);
}, executor);

// WebSocket 接收线程：每收到一个 chunk 直接写进 pipe
websocket.onMessage((chunk) -> {
    pipedOut.write(Base64.getDecoder().decode(chunk));
});
// 合成结束后关闭写端
pipedOut.close();

// 等待上传完成，拿到 URL
String audioUrl = uploadFuture.get(30, TimeUnit.SECONDS);
```

这个方案的好处是内存里始终只有一个 chunk 的大小（通常几 KB），整个音频数据从 WebSocket 到 MinIO 是流式的，中间不会堆积。而且 `S3ClientUtil` 现有的 `uploadObject(objectKey, contentType, inputStream)` 方法不用改一行，直接复用。

**如果管道流在某些场景不好控制生命周期，退一步也可以用临时文件。** 把 `ByteArrayOutputStream` 换成 `FileOutputStream`，chunk 落盘而不是留在堆里，合成完后再用 `FileInputStream` 流式上传到 MinIO，上传完删掉临时文件。内存占用同样降到单个 chunk 级别，只是多了一次磁盘 IO。

**另外不管用哪个方案，TTS 这种重资源操作都应该加并发限流。** 用一个 `Semaphore` 控制同时执行的 TTS 任务数，比如最多 20 个，超出的排队等待。这样从源头控制住内存总量，避免突发流量打挂服务：

```java
private static final Semaphore TTS_SEMAPHORE = new Semaphore(20);

public Map<String, Object> call(NodeState nodeState, Map<String, Object> inputs) {
    TTS_SEMAPHORE.acquire();
    try {
        return doTtsSynthesis(nodeState, inputs);
    } finally {
        TTS_SEMAPHORE.release();
    }
}
```

总结一下就是三板斧：**管道流对接 MinIO 消除内存驻留，信号量限流控制并发上限，再加上监控告警兜底。** 项目里 `S3ClientUtil` 已经具备流式上传能力，核心改动就是把 TTS 的 `byte[]` 上传换成 `InputStream` 上传，成本很低但效果立竿见影。
