# PaiFlow 项目面试题 120 道

> 针对「派派工作流」项目（AI Agent 工作流编排平台）的面试题，涵盖项目理解、技术实现、架构设计等多个维度。

---

## 一、项目整体理解（10 道）

### 1. 请用通俗的语言介绍一下 PaiFlow 这个项目是做什么的？解决了什么问题？

**考察点**：项目理解、业务价值表达

**参考答案**：
PaiFlow 是一个 AI 工作流编排平台，简单说就是让用户通过"拖拖拽拽"的方式，把多个 AI 能力串起来，自动完成一些复杂的任务。

举个具体例子：我们有个"AI 播客生成"的场景。用户只需要输入一段文字，系统会自动调用大模型把它改写成适合口播的风格，然后再调用语音合成服务生成音频。以前这个过程需要人工一步步操作，现在配置好工作流后，一键就能完成。

它解决的核心问题是：**降低 AI 应用的开发门槛**。业务人员不需要写代码，只要在可视化界面上编排节点，就能快速搭建 AI 应用。

---

### 2. 你能画一下 PaiFlow 的整体架构图吗？各个服务之间是怎么协作的？

**考察点**：系统架构、服务间调用关系

**参考答案**：
整体是微服务架构，主要有这几个服务：

```
用户 → Nginx → Console Frontend (React)
              ↓
         Console Hub (Spring Boot, 8080端口)
              ↓
         Workflow Engine (Java/Python, 7880端口)
              ↓
    ┌─────────┼─────────┐
    ↓         ↓         ↓
  LLM API   AI Tools   Link插件
(DeepSeek)  (语音合成)  (外部工具)
```

协作流程是这样的：
1. 用户在前端设计工作流，保存到后端
2. 执行时，Console Hub 把请求转发给 Workflow Engine
3. Workflow Engine 按照 DAG 顺序执行各个节点
4. 遇到 LLM 节点就调大模型 API，遇到插件节点就调 AI Tools 或 Link
5. 执行结果通过 SSE 实时推送给前端

数据存储用 PostgreSQL 存工作流定义，Redis 做缓存，MinIO 存音频文件这些大对象。

---

### 3. 工作流引擎在整个系统中扮演什么角色？为什么要单独做一个工作流引擎？

**考察点**：核心模块理解、架构决策

**参考答案**：
工作流引擎是整个系统的"大脑"，它负责解析用户设计的工作流，然后按照正确的顺序执行每个节点。

单独做一个工作流引擎主要是这几个考虑：

1. **职责分离**：Console Hub 负责用户管理、权限这些，工作流引擎专注于流程调度，各司其职
2. **可扩展性**：工作流执行是 CPU 密集型的，单独部署可以独立扩容
3. **技术栈灵活**：我们同时维护了 Java 和 Python 两个版本，可以根据场景选择
4. **复用性**：工作流引擎的能力可以给其他业务系统复用，不只是 PaiFlow 一个产品

其实就像把"调度"这件事抽象出来，做成一个通用的服务。

---

### 4. 你简历上说项目"将本地环境搭建时间从小时级降低至 10 分钟"，具体是怎么做到的？

**考察点**：DevOps 实践、Docker Compose 编排

**参考答案**：
之前搭环境痛苦在于：要装 MySQL、Redis、MinIO，还要配置各种连接参数，经常有人环境配错导致启动不了。

我做的优化主要是：

1. **统一用 Docker Compose 编排**：写了一个 docker-compose.yml，把所有服务都容器化，一条命令 `docker compose up -d` 就能拉起整个环境

2. **解决依赖顺序问题**：比如 Workflow 服务依赖 MySQL，我配置了健康检查，确保 MySQL 真正可用后才启动 Workflow，避免连接失败

3. **预置初始化数据**：把数据库初始化脚本挂载进去，首次启动自动建表、导入测试数据

4. **环境变量模板化**：提供 `.env.example` 文件，复制一份改改就能用，不用到处找配置项

最后效果就是：新同事拉代码后，复制 .env 文件，跑一条命令，泡杯咖啡回来环境就好了。

---

### 5. PaiFlow 支持哪些类型的节点？你能举例说明每种节点的作用吗？

**考察点**：业务理解、节点类型设计

**参考答案**：
目前支持这几种核心节点类型：

1. **Start 节点**：工作流的入口，定义用户输入的参数，比如"请输入要转换的文本"

2. **LLM 节点**：调用大模型，支持 DeepSeek、通义千问这些。可以配置 System Prompt 设定角色，User Prompt 是具体任务

3. **Plugin 节点**：调用外部工具，比如超拟人语音合成、图片识别。通过 Link 连接器去调用这些 API

4. **End 节点**：工作流的出口，定义最终输出什么，可以是文本、音频 URL 等

5. **条件分支节点**（规划中）：根据上一步结果决定走哪个分支，比如情感分析结果是正面就走 A 流程，负面走 B 流程

每种节点都有对应的 NodeExecutor 实现类，通过策略模式来支持扩展。

---

### 6. 如果让你给一个完全不懂技术的人介绍这个项目，你会怎么说？

**考察点**：表达能力、业务价值提炼

**参考答案**：
你可以把它想象成一个"AI 流水线工厂"。

以前你想让 AI 帮你做事，比如写一篇文章然后读出来，你得先打开一个 AI 写作工具，写完复制出来，再打开一个语音合成工具，粘贴进去，生成音频，下载下来。

PaiFlow 就是把这些步骤自动化了。你在一个画布上，把"写文章"和"语音合成"这两个积木块连起来，以后每次只要输入一个主题，系统就自动帮你完成整个流程，最后直接给你一段音频。

对企业来说，它的价值是：**不用养一个开发团队，业务人员自己就能搭建 AI 应用**。想改流程？拖拖拽拽就行，不用等排期。

---

### 7. 项目中"AI 播客生成"这个场景的完整链路是什么样的？

**考察点**：端到端流程理解

**参考答案**：
这是我们的核心场景，完整链路是这样的：

1. **用户输入**：在前端输入一段文字，比如一篇技术文章

2. **Start 节点**：接收用户输入，存到变量池

3. **LLM 节点（内容改写）**：
   - 调用 DeepSeek 大模型
   - System Prompt 设定为"你是一个播客主播"
   - 把原文改写成口语化、适合朗读的风格
   - 输出存到变量池

4. **Plugin 节点（语音合成）**：
   - 调用讯飞的超拟人语音合成 API
   - 把改写后的文本转成音频
   - 音频文件上传到 MinIO，返回 URL

5. **End 节点**：把音频 URL 作为最终输出

6. **结果返回**：通过 SSE 实时推送给前端，用户可以直接播放

整个过程大概 10-30 秒，取决于文本长度。LLM 节点是流式输出的，用户能看到内容一个字一个字地出来。

---

### 8. 你在这个项目中最有成就感的一件事是什么？为什么？

**考察点**：个人贡献、技术成长

**参考答案**：
最有成就感的是**从零搭建了 Java 版的工作流引擎**。

项目原来只有 Python 版本，但团队 Java 技术栈更熟，维护起来有些吃力。我主动提出用 Java 重写核心的工作流引擎部分。

这个过程中我深入理解了：
- DAG 的拓扑排序和遍历算法
- 怎么设计一个可扩展的节点执行器架构
- Spring AI 框架的源码和使用姿势
- SSE 流式响应的各种坑

最后的成果是：Java 版本的性能和 Python 版本持平，但代码可读性更好，团队维护起来更顺手。而且我还做了双版本共存的方案，可以一键切换，保证了迁移过程的平滑。

这件事让我从"会用框架"变成了"能设计框架"，是一个质的提升。

---

### 9. 项目开发过程中遇到的最大挑战是什么？你是怎么解决的？

**考察点**：问题解决能力

**参考答案**：
最大的挑战是 **SSE 流式响应的消息乱序问题**。

问题背景：LLM 输出是一个字一个字流式返回的，我们要实时推送给前端。但在高并发场景下，前端收到的消息顺序是乱的，"你好世界"可能显示成"好你界世"。

排查过程：
1. 一开始以为是前端问题，但前端同事说他是按收到顺序渲染的
2. 加日志发现，服务端发送时顺序是对的，但到达顺序不对
3. 最后定位到是多线程往 SseEmitter 写数据导致的

解决方案：
设计了一个**双队列架构**。一个队列负责接收 LLM 返回的 token，另一个队列负责排序后发送。用一个独立的消费者线程来保证发送的有序性。

最后效果：消息投递可靠性达到 99.99%，彻底解决了乱序问题。

这个经历让我学到：并发问题一定要从数据流的角度去分析，找到"竞争"发生在哪里。

---

### 10. 如果现在让你重新设计这个系统，有哪些地方你会做得不一样？

**考察点**：反思能力、架构视野

**参考答案**：
回头看，有几个地方可以做得更好：

1. **断点续传做得更完善**：现在工作流中断后要从头执行，如果借鉴 LangGraph 的 Checkpointer 设计，把每个节点执行后的状态持久化，就能支持从断点恢复

2. **DSL 设计更规范**：前端传过来的 JSON 格式是历史原因形成的，有些字段命名不一致。如果重新设计，会先定义好 DSL 的 Schema，用 JSON Schema 做校验

3. **引入事件溯源**：现在的日志是散的，如果用事件溯源的思路，把每个节点的输入输出都作为事件存下来，排查问题和做数据分析都会方便很多

4. **考虑用 LangGraph4J**：我们自研的引擎虽然够用，但在循环、人工介入这些高级特性上还不够完善。如果业务有需求，直接用 LangGraph4J 可能更省事

当然，这些都是事后诸葛亮，当时的技术选型在当时的上下文下是合理的。

---

## 二、工作流引擎 / DAG 调度（15 道）

### 11. 什么是 DAG？为什么工作流引擎要用 DAG 来表示？

**考察点**：数据结构基础、DAG 特性

**参考答案**：
DAG 是 Directed Acyclic Graph，有向无环图。"有向"是说边有方向，从 A 指向 B 表示 A 执行完才能执行 B；"无环"是说不能有循环依赖，A→B→C→A 这种是不允许的。

工作流用 DAG 表示的好处：

1. **天然表达依赖关系**：节点之间谁先谁后一目了然
2. **支持并行执行**：没有依赖关系的节点可以同时跑，提高效率
3. **容易做拓扑排序**：能算出一个合法的执行顺序
4. **避免死循环**：无环的特性保证工作流一定能执行完

在我们代码里，节点就是 Node 对象，边就是 Edge 对象，存的是 source 和 target 两个节点 ID。

---

### 12. 你的 WorkflowEngine 是怎么解析 DSL（JSON）并构建执行链路的？

**考察点**：DSL 解析、链路构建逻辑

**参考答案**：
整个过程分三步：

**第一步：反序列化**
前端传过来的是 JSON，我用 FastJSON 直接反序列化成 WorkflowDSL 对象，里面包含 nodes 列表和 edges 列表。

**第二步：构建节点索引**
遍历 nodes 列表，建一个 `Map<String, Node>`，key 是节点 ID，方便后面快速查找。同时找出 Start 节点作为入口。

**第三步：根据边构建邻接关系**
遍历 edges 列表，对于每条边：
- 把 target 节点加到 source 节点的 nextNodes 列表
- 把 source 节点加到 target 节点的 preNodes 列表
- 如果是异常分支的边，加到 failNodes 列表

最后返回 Start 节点，从它开始就能顺着 nextNodes 遍历整个图了。

---

### 13. 代码中 `buildNodeExecuteChain` 方法的作用是什么？请解释一下实现思路。

**考察点**：源码理解

**参考答案**：
这个方法的作用是**把扁平的节点列表和边列表，转换成一个可执行的链路结构**。

核心代码逻辑是这样的：

```java
// 1. 建立节点索引，找到起始节点
Map<String, Node> nodeMap = new HashMap<>();
for (Node node : workflowDSL.getNodes()) {
    if (node.getNodeType() == NodeTypeEnum.START) {
        startNode = node;
    }
    nodeMap.put(node.getId(), node);
}

// 2. 根据边构建邻接关系
for (Edge edge : workflowDSL.getEdges()) {
    Node sourceNode = nodeMap.get(edge.getSource());
    Node targetNode = nodeMap.get(edge.getTarget());
    
    // 判断是正常分支还是异常分支
    if (handle.startsWith("normal_one_of")) {
        sourceNode.getNextNodes().add(targetNode);
    } else if (handle.startsWith("intent_chain")) {
        sourceNode.getFailNodes().add(targetNode);
    }
    
    targetNode.getPreNodes().add(sourceNode);
}
```

执行时从 startNode 开始，递归执行 nextNodes 里的节点就行了。

---

### 14. 工作流中如何处理节点之间的依赖关系？如果节点 C 依赖节点 A 和 B，怎么保证 A、B 都执行完才执行 C？

**考察点**：依赖管理、执行顺序控制

**参考答案**：
我们用的是**前置节点检查**的方式。

每个节点有一个 preNodes 列表，存的是它依赖的所有前置节点。执行节点 C 之前，会先检查：

```java
if (!CollectionUtils.isEmpty(node.getPreNodes())) {
    for (Node preNode : node.getPreNodes()) {
        if (!preNode.getStatus().executed()) {
            // 前置节点没执行完，先执行它
            executeNode(preNode, variablePool, callback);
        }
    }
}
```

这是一个递归的过程。当执行到 C 时，发现 A 没执行完，就先去执行 A；A 执行完回来，发现 B 没执行完，再去执行 B；都执行完了，才真正执行 C。

节点的 status 字段标记了执行状态：PENDING、RUNNING、SUCCESS、ERROR、SKIP。通过 `executed()` 方法判断是否已经执行过。

---

### 15. 你是怎么实现并行分支的？比如节点 A 执行完后，节点 B 和 C 可以同时执行。

**考察点**：并行执行、BFS 调度

**参考答案**：
并行执行的实现思路是：**用线程池 + BFS 广度优先遍历**。

当节点 A 执行完后，它的 nextNodes 可能有多个（B 和 C）。我们用 ConcurrentLinkedQueue 管理就绪节点：

```java
Queue<Node> readyQueue = new ConcurrentLinkedQueue<>();
readyQueue.addAll(currentNode.getNextNodes());

// 并行执行
List<CompletableFuture<Void>> futures = new ArrayList<>();
while (!readyQueue.isEmpty()) {
    Node node = readyQueue.poll();
    futures.add(CompletableFuture.runAsync(() -> {
        executeNode(node, variablePool, callback);
    }, threadPool));
}

// 等待所有并行节点完成
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

这样 B 和 C 就会被同时提交到线程池执行。等它们都完成后，再继续执行后续节点。

实际代码里我们做了简化，用的是递归调用，并行度受限于线程池大小。

---

### 16. 简历上提到"通过 ConcurrentLinkedQueue 管理就绪节点"，为什么用这个数据结构？

**考察点**：并发数据结构选型

**参考答案**：
选 ConcurrentLinkedQueue 主要考虑三点：

1. **线程安全**：多个线程可能同时往队列里加节点或取节点，普通的 LinkedList 会有并发问题

2. **无界队列**：工作流的节点数量不固定，用无界队列不用担心容量问题

3. **非阻塞**：它是基于 CAS 实现的，入队出队操作不会阻塞，性能好

相比 LinkedBlockingQueue，ConcurrentLinkedQueue 不支持阻塞等待，但我们的场景不需要阻塞——没有就绪节点时，说明工作流结束或者在等待其他节点，不需要阻塞等待。

相比 ArrayBlockingQueue，它是无界的，不用担心队列满了。

---

### 17. 如果工作流中存在环（循环依赖），你的引擎能检测出来吗？怎么检测？

**考察点**：环检测、拓扑排序

**参考答案**：
目前代码里有基础的校验，但环检测还没做得很完善。如果要检测，有两种经典方法：

**方法一：拓扑排序**
用 Kahn 算法，每次删除入度为 0 的节点。如果最后还有节点没被删除，说明存在环。

```java
// 计算每个节点的入度
Map<String, Integer> inDegree = new HashMap<>();
// 不断删除入度为0的节点
// 如果最后 inDegree 不为空，说明有环
```

**方法二：DFS 染色**
遍历时给节点染色：白色（未访问）、灰色（访问中）、黑色（已完成）。如果遇到灰色节点，说明形成了环。

```java
if (color.get(node) == GRAY) {
    throw new CyclicDependencyException("检测到循环依赖");
}
```

我们计划在 `verifyWorkflow` 方法里加上这个检测，在执行前就报错，而不是等到运行时死循环。

---

### 18. 节点执行失败时，你是怎么处理的？支持哪些错误处理策略？

**考察点**：异常处理、ErrorStrategyEnum

**参考答案**：
我们定义了三种错误处理策略，通过 ErrorStrategyEnum 枚举表示：

1. **ERR_INTERUPT（中断）**：节点失败后，整个工作流停止，向上抛异常。适合关键节点，比如付款失败就不能继续发货

2. **ERR_CODE（错误码）**：节点失败后，用配置好的默认值作为输出，继续执行后续节点。适合非关键节点，比如日志记录失败不影响主流程

3. **ERR_CONDITION（错误分支）**：节点失败后，不走正常的 nextNodes，改走 failNodes 里的异常处理分支。适合需要特殊处理的场景，比如调用外部 API 失败就走降级逻辑

具体走哪个策略，是在节点配置的 retryConfig.errorStrategy 字段里指定的。

---

### 19. 代码中的 `NodeStatusEnum` 有哪些状态？状态之间是怎么流转的？

**考察点**：状态机设计

**参考答案**：
NodeStatusEnum 定义了节点的生命周期状态：

```
PENDING（待执行）
   ↓
RUNNING（执行中）
   ↓
┌──┴──┐
↓     ↓
SUCCESS  ERROR
   ↓
SKIP（跳过）
MARK（标记待定）
```

状态流转规则：
- 初始状态是 PENDING
- 开始执行时变成 RUNNING
- 执行成功变成 SUCCESS，失败变成 ERROR
- 如果节点在异常分支上，但正常分支执行了，会被标记为 SKIP
- MARK 是中间状态，表示"可能执行也可能跳过"，等前置节点都完成后再决定

有个 `executed()` 方法判断是否已执行完：SUCCESS、ERROR、SKIP 都算执行完。

---

### 20. 什么是"断点续传"？你的工作流引擎是怎么实现的？

**考察点**：状态持久化、恢复机制

**参考答案**：
断点续传是指：工作流执行到一半中断了（比如服务器重启），下次可以从中断的地方继续执行，不用从头开始。

说实话，我们目前的实现还比较简单，没有做完整的断点续传。现在的做法是：
- 每个节点执行完会通过回调记录日志
- 节点状态存在内存里（Node 对象的 status 字段）
- 如果服务重启，状态就丢了，只能重新执行

如果要做完整的断点续传，需要：
1. 每个节点执行完后，把状态和 VariablePool 持久化到数据库
2. 重启后，根据 workflowId 查出上次的状态
3. 跳过已经成功的节点，从第一个未完成的节点继续

这块是后续优化的方向，可以参考 LangGraph4J 的 Checkpointer 设计。

---

### 21. 你说"核心调度延时低至 10ms 级"，这个是怎么测出来的？有什么优化手段？

**考察点**：性能优化、指标度量

**参考答案**：
测量方法是在关键路径上加埋点：

```java
long start = System.currentTimeMillis();
// 执行调度逻辑
long cost = System.currentTimeMillis() - start;
log.info("Node scheduling cost: {}ms", cost);
```

10ms 指的是纯调度的时间，不包括节点实际执行的时间（比如调用 LLM 可能要几秒）。

优化手段主要有：

1. **减少对象创建**：复用 Node 对象，不要每次都 new
2. **用 Map 缓存**：节点查找用 HashMap，O(1) 复杂度
3. **避免不必要的同步**：用无锁的 ConcurrentLinkedQueue
4. **预编译正则**：VariableTemplateRender 里的正则表达式预编译后复用
5. **延迟初始化**：不需要的资源用到时再加载

最大的优化其实是算法层面：用 BFS 而不是 DFS，可以更快找到可并行的节点。

---

### 22. 工作流的输入输出是怎么在节点之间传递的？VariablePool 的设计思路是什么？

**考察点**：变量池设计、数据传递

**参考答案**：
VariablePool 是一个全局的变量存储池，所有节点的输入输出都通过它传递。

数据结构是一个两层 Map：
```java
Map<String, Map<String, Object>> pool;
// 第一层 key 是节点 ID
// 第二层 key 是变量名
// value 是变量值
```

工作流程：
1. Start 节点执行时，把用户输入存到 `pool["start"]["input"]`
2. LLM 节点需要输入时，从配置里看它引用的是哪个节点的哪个变量，比如 `start.input`
3. 调用 `variablePool.get("start", "input")` 取值
4. LLM 节点执行完，把输出存到 `pool["llm-1"]["output"]`
5. 下一个节点同样的方式取用

设计思路是**解耦**：节点之间不直接传递数据，而是通过变量池中转。这样节点可以独立测试，也支持一个变量被多个节点引用。

用 FastJSON 做深拷贝，避免一个节点修改数据影响其他节点。

---

### 23. `AbstractNodeExecutor` 中的模板方法模式是怎么应用的？

**考察点**：设计模式、代码复用

**参考答案**：
模板方法模式的核心是：**父类定义算法骨架，子类实现具体步骤**。

在 AbstractNodeExecutor 里，`execute()` 方法定义了节点执行的标准流程：

```java
public NodeRunResult execute(NodeState nodeState) {
    // 1. 检查重试配置
    // 2. 如果需要超时控制，调用 doExecuteWithTimeout()
    // 3. 处理重试逻辑
    return doExecute(nodeState);
}

protected NodeRunResult doExecute(NodeState nodeState) {
    // 1. 发送节点开始事件
    callback.onNodeStart(...);
    // 2. 解析输入参数
    Map<String, Object> inputs = resolveInputs(node, variablePool);
    // 3. 调用子类实现的具体执行逻辑（抽象方法）
    NodeRunResult result = executeNode(nodeState, inputs);
    // 4. 存储输出到变量池
    storeOutputs(node, result.getOutputs(), variablePool);
    // 5. 发送节点结束事件
    return result;
}

// 子类必须实现这个方法
protected abstract NodeRunResult executeNode(NodeState nodeState, Map<String, Object> inputs);
```

子类比如 LLMNodeExecutor、PluginNodeExecutor 只需要实现 `executeNode()` 方法，专注于自己的业务逻辑，不用关心日志、重试、超时这些通用逻辑。

---

### 24. 如果一个节点执行超时了，你是怎么处理的？

**考察点**：超时控制、TimeLimiter

**参考答案**：
超时控制的实现在 `doExecuteWithTimeout()` 方法里，用的是 Guava 的 TimeLimiter 思路：

```java
protected NodeRunResult doExecuteWithTimeout(NodeState nodeState, RetryConfig retryConfig) {
    if (retryConfig.timeOutEnabled()) {
        try {
            return AsyncUtil.callWithTimeLimit(
                retryConfig.toMillis(), 
                TimeUnit.MILLISECONDS,
                () -> this.doExecute(nodeState)
            );
        } catch (TimeoutException e) {
            NodeRunResult result = new NodeRunResult();
            result.setError(new NodeCustomException(ErrorCode.TIMEOUT_ERROR));
            return errorResponse(nodeState, result);
        }
    }
    return this.doExecute(nodeState);
}
```

实现原理：
1. 把节点执行逻辑提交到一个单独的线程
2. 主线程用 `Future.get(timeout)` 等待
3. 如果超时了，Future.get() 抛出 TimeoutException
4. 捕获异常后，返回超时错误，走错误处理策略

超时时间是在节点配置的 `retryConfig.timeout` 字段里设置的，单位是毫秒。

---

### 25. 你是怎么实现节点的重试机制的？支持哪些退避策略？

**考察点**：重试策略、指数退避

**参考答案**：
重试逻辑在 AbstractNodeExecutor 的 `execute()` 方法里：

```java
if (retryConfig.getShouldRetry()) {
    while (true) {
        NodeRunResult res = this.doExecuteWithTimeout(nodeState, retryConfig);
        if (res.getStatus().isSuccess()) {
            return res;
        }
        if (executeTime > retryConfig.getMaxRetries()) {
            return res;  // 超过最大重试次数，返回失败
        }
        executeTime = node.getExecutedCount().addAndGet(1);
        // 这里可以加退避等待
    }
}
```

目前支持的配置项：
- `shouldRetry`：是否开启重试
- `maxRetries`：最大重试次数
- `timeout`：单次执行超时时间

退避策略我们目前实现得比较简单，主要是固定间隔重试。如果要做完善的话，可以支持：
- **固定间隔**：每次重试等待固定时间，比如 1 秒
- **线性退避**：第 1 次等 1 秒，第 2 次等 2 秒，第 3 次等 3 秒
- **指数退避**：第 1 次等 1 秒，第 2 次等 2 秒，第 3 次等 4 秒

指数退避适合调用外部 API 的场景，避免雪崩。

---

## 二点五、插件与工具调用 / Link 模块（5 道）

### 25.1 Link 模块在整个系统中的作用是什么？为什么要单独抽出来？

**考察点**：模块设计、职责分离

**参考答案**：
Link 模块是工作流引擎和外部工具之间的"桥梁"，专门负责对接第三方 API 和内部 AI 能力。

**它的核心职责**：
1. **工具注册管理**：存储工具的 OpenAPI Schema，知道每个工具怎么调用
2. **统一调用协议**：不管是讯飞的 TTS、还是第三方天气 API，都通过统一的接口调用
3. **鉴权处理**：自动处理 API Key、Bearer Token 等认证方式
4. **参数转换**：把工作流传过来的参数转成目标 API 需要的格式

**为什么单独抽出来**：

```
Workflow Engine  ──→  Link  ──→  讯飞 TTS
                       │
                       ├──→  天气 API  
                       │
                       └──→  其他工具
```

1. **解耦**：工作流引擎不需要知道具体怎么调用每个工具
2. **可扩展**：新增工具只需要在 Link 里注册，不用改工作流引擎
3. **复用**：Link 可以给多个业务系统使用
4. **独立演进**：工具调用逻辑可以独立升级

---

### 25.2 你是怎么解析 OpenAPI Schema 的？怎么知道一个工具该怎么调用？

**考察点**：OpenAPI 规范、Schema 解析

**参考答案**：
OpenAPI（以前叫 Swagger）是描述 REST API 的标准格式。我们用它来定义工具的调用方式。

**一个工具的 OpenAPI Schema 长这样**：
```json
{
  "openapi": "3.0.0",
  "info": {"title": "TTS 服务"},
  "servers": [{"url": "http://core-aitools:18668"}],
  "paths": {
    "/api/tts/synthesize": {
      "post": {
        "operationId": "synthesize_speech",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "properties": {
                  "text": {"type": "string", "description": "要合成的文本"},
                  "voice": {"type": "string", "description": "音色"}
                }
              }
            }
          }
        }
      }
    }
  }
}
```

**解析流程**：

```java
// 1. 从数据库查工具的 Schema
ToolEntity tool = toolCrudService.getById(toolId);
Map<String, Object> openApiSchema = JSON.parseObject(tool.getOpenApiSchema());

// 2. 获取服务地址
String serverUrl = openApiSchema.getServers().get(0).getUrl();

// 3. 根据 operationId 找到对应的 path 和 method
for (Entry<String, Object> path : openApiSchema.getPaths()) {
    for (Entry<String, Object> method : path.getValue()) {
        if (operationId.equals(method.get("operationId"))) {
            // 找到了！记录 method（POST）和 path（/api/tts/synthesize）
        }
    }
}

// 4. 组装请求，调用目标 API
httpExecutor.doCall(serverUrl + path, method, params);
```

这样，新增一个工具只需要写一份 OpenAPI Schema，不用写代码。

---

### 25.3 工具调用的鉴权是怎么处理的？支持哪些认证方式？

**考察点**：API 鉴权、安全设计

**参考答案**：
不同的第三方 API 有不同的认证方式，我们在 Link 里统一处理。

**支持的认证方式**：

1. **API Key（Header 方式）**
```java
// Schema 里定义
"securitySchemes": {
  "apiKey": {"type": "apiKey", "in": "header", "name": "X-API-Key"}
}

// 调用时自动加上
headers.put("X-API-Key", "sk-xxx");
```

2. **API Key（Query 方式）**
```java
// 参数里带上
url = url + "?api_key=sk-xxx";
```

3. **Bearer Token**
```java
headers.put("Authorization", "Bearer " + token);
```

**实现逻辑**：

```java
private void processAuthentication(Map<String, Object> operationIdSchema, 
                                   Map<String, Object> headers,
                                   Map<String, Object> query) {
    String securityType = operationIdSchema.get("security_type");
    
    // 从 Redis 获取工具的认证配置
    Map<String, Object> authInfo = redisService.getToolConfig(toolId).get("authentication");
    
    if ("apiKey".equals(type)) {
        String location = securityScheme.get("in");  // header 或 query
        if ("header".equals(location)) {
            headers.putAll(authInfo.get("apiKey"));
        } else {
            query.putAll(authInfo.get("apiKey"));
        }
    } else if ("bearer".equals(type)) {
        headers.put("Authorization", "Bearer " + authInfo.get("bearerToken"));
    }
}
```

认证信息存在 Redis 里，配置和代码分离，改密钥不用重启服务。

---

### 25.4 讯飞超拟人语音合成（TTS）工具是怎么对接的？整个调用链路是什么样的？

**考察点**：具体工具对接、链路理解

**参考答案**：
讯飞 TTS 是我们最常用的工具，用来把文本转成音频。

**调用链路**：

```
PluginNode 
    ↓
Link.run(toolId="tts", params={text: "你好"})
    ↓
ToolExecutionService.httpRun()
    ↓ (解析 OpenAPI Schema，拼装请求)
HttpExecutor.doCall("http://core-aitools:18668/api/tts", POST, {text: "你好"})
    ↓
AI Tools 服务（讯飞 SDK 封装）
    ↓
讯飞 TTS API
    ↓ (返回音频流)
上传到 MinIO
    ↓
返回音频 URL
```

**代码层面**：

```java
// 1. PluginNodeExecutor 调用 Link
public class PluginNodeExecutor extends AbstractNodeExecutor {
    @Override
    protected NodeRunResult executeNode(NodeState state, Map<String, Object> inputs) {
        // 获取工具 ID 和参数
        String toolId = nodeParam.get("toolId");
        Map<String, Object> params = buildParams(inputs);
        
        // 调用 Link
        Object result = linkClient.run(toolId, params);
        return NodeRunResult.success(result);
    }
}

// 2. Link 执行工具调用
public HttpToolRunResponse httpRun(HttpToolRunRequest request) {
    // 查工具 Schema
    ToolSchemaResult schema = getToolSchema(toolId, operationId);
    
    // 处理鉴权
    processAuthentication(schema, headers, query);
    
    // 发送 HTTP 请求
    String result = httpExecutor.doCall(serverUrl, method, params);
    
    return buildResponse(result);
}
```

AI Tools 服务里封装了讯飞 SDK，处理了流式合成、音频拼接这些复杂逻辑。

---

### 25.5 如果要新增一个第三方工具（比如天气 API），需要做哪些事情？

**考察点**：扩展性、实操能力

**参考答案**：
新增工具不需要写代码，只需要三步：

**第一步：编写 OpenAPI Schema**

```json
{
  "openapi": "3.0.0",
  "info": {"title": "天气查询服务", "version": "1.0.0"},
  "servers": [{"url": "https://api.weather.com"}],
  "paths": {
    "/v1/weather": {
      "get": {
        "operationId": "get_weather",
        "summary": "查询城市天气",
        "parameters": [
          {
            "name": "city",
            "in": "query",
            "required": true,
            "schema": {"type": "string"},
            "description": "城市名称"
          }
        ],
        "security": [{"apiKey": []}]
      }
    }
  },
  "components": {
    "securitySchemes": {
      "apiKey": {"type": "apiKey", "in": "header", "name": "X-API-Key"}
    }
  }
}
```

**第二步：在数据库注册工具**

```sql
INSERT INTO tools_schema (tool_id, app_id, version, open_api_schema) 
VALUES ('tool@weather', '680ab54f', 'V1.0', '上面的JSON');
```

**第三步：配置认证信息**

```java
// 存到 Redis
redisService.setToolConfig("tool@weather", "V1.0", Map.of(
    "authentication", Map.of("apiKey", Map.of("X-API-Key", "your-api-key"))
));
```

**然后就能在工作流里用了**：

在可视化编辑器里拖一个 Plugin 节点，选择"天气查询"工具，配置输入参数（city），连到前面的节点，搞定。

这种"配置化"的设计让运营同学也能接入新工具，不用等开发排期。

---

## 三、Spring AI 框架（12 道）

### 26. Spring AI 是什么？它解决了什么问题？

**考察点**：框架理解

**参考答案**：
Spring AI 是 Spring 官方出的 AI 应用开发框架，可以理解为"Spring Boot 版的 LangChain"。

它解决的核心问题是：**统一不同大模型的调用方式**。

以前调 OpenAI 要用 OpenAI 的 SDK，调通义千问要用阿里的 SDK，代码写法完全不一样。Spring AI 抽象了一层 ChatModel 接口，不管底层是哪个模型，上层代码都一样：

```java
ChatResponse response = chatModel.call(new Prompt("你好"));
```

换模型只需要改配置，代码不用动。

另外它还提供了 RAG、Function Calling、向量数据库这些 AI 应用常用的能力，而且和 Spring 生态无缝集成，对 Java 开发者很友好。

---

### 27. 你是怎么用 Spring AI 对接 DeepSeek 的？配置了哪些参数？

**考察点**：实际使用经验

**参考答案**：
DeepSeek 的 API 是兼容 OpenAI 格式的，所以用 Spring AI 的 OpenAI 模块就能对接。

主要配置：

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
          max-tokens: 4096
```

关键参数说明：
- `base-url`：改成 DeepSeek 的地址，不能用默认的 OpenAI 地址
- `model`：DeepSeek 的模型名，比如 deepseek-chat、deepseek-coder
- `temperature`：控制输出的随机性，0 最确定，1 最随机
- `max-tokens`：最大输出长度

代码里直接注入 ChatModel 就能用：

```java
@Autowired
private ChatModel chatModel;

public String chat(String message) {
    return chatModel.call(message);
}
```

---

### 28. Spring AI 中的 `ChatModel` 和 `ChatClient` 有什么区别？你用的是哪个？

**考察点**：API 理解

**参考答案**：
**ChatModel** 是底层接口，直接和大模型 API 交互。用起来比较原始：

```java
ChatResponse response = chatModel.call(new Prompt(messages));
String text = response.getResult().getOutput().getContent();
```

**ChatClient** 是高层封装，提供了流式 API，用起来更优雅：

```java
String response = ChatClient.create(chatModel)
    .prompt()
    .system("你是一个助手")
    .user("你好")
    .call()
    .content();
```

ChatClient 还支持链式调用、Advisor 插件、流式响应等高级特性。

我们项目里**两个都用了**：
- 底层的 ModelServiceClient 封装了 ChatModel，做流式调用
- 上层业务代码有时用 ChatClient 做简单的一次性调用

选择的原则是：需要细粒度控制用 ChatModel，追求开发效率用 ChatClient。

---

### 29. 简历上说"深入源码排查 Spring AI 1.1 的路径拼接 bug"，能详细说说吗？

**考察点**：源码阅读能力、问题排查

**参考答案**：
这个问题是在对接阿里百炼（DashScope）时遇到的。

**现象**：配置了 base-url 为阿里的地址后，请求还是 404。

**排查过程**：
1. 抓包发现请求的 URL 是错的，路径多了一截
2. 跟进 Spring AI 源码，找到 OpenAiApi 类
3. 发现它在构造 URL 时，会把 baseUrl 和 completionsPath 拼起来
4. 但阿里的 API 路径和 OpenAI 不一样，直接拼就错了

**问题根源**：
Spring AI 假设所有兼容 OpenAI 的模型，API 路径都是 `/v1/chat/completions`，但阿里百炼的路径是 `/compatible-mode/v1/chat/completions`。

**解决方案**：
重写 OpenAiApi 的初始化逻辑，支持自定义 completionsPath：

```java
public OpenAiApi buildApi(String baseUrl, String apiKey, String completionsPath) {
    // 如果传了自定义路径，使用自定义路径
    // 否则使用默认的 /v1/chat/completions
}
```

这样配置 baseUrl 时就能适配不同厂商的路径规则了。

---

### 30. 你是怎么实现"统一抽象"来支持 OpenAI、DeepSeek、智谱等多个模型的？

**考察点**：抽象设计、适配器模式

**参考答案**：
核心思路是**工厂模式 + 配置化**。

我封装了一个 `buildChatModel` 方法，根据传入的模型配置动态构建 ChatModel：

```java
public ChatModel buildChatModel(ModelConfig config) {
    String provider = config.getProvider(); // openai, deepseek, zhipu
    
    switch (provider) {
        case "openai":
        case "deepseek":
            // DeepSeek 兼容 OpenAI 协议
            return new OpenAiChatModel(
                OpenAiApi.builder()
                    .baseUrl(config.getBaseUrl())
                    .apiKey(config.getApiKey())
                    .build()
            );
        case "zhipu":
            return new ZhiPuAiChatModel(config);
        default:
            throw new IllegalArgumentException("不支持的模型: " + provider);
    }
}
```

这样做的好处：
1. **业务代码不感知底层模型**：都是调用 ChatModel 接口
2. **支持动态切换**：同一个工作流里，不同节点可以用不同模型
3. **配置驱动**：模型信息存数据库，不用改代码就能加新模型

我们叫它"即用即毁"，每次调用都新建 ChatModel 实例，不存状态，避免并发问题。

---

### 31. Spring AI 的流式响应是怎么实现的？`Flux<ChatResponse>` 是什么？

**考察点**：响应式编程、Flux

**参考答案**：
Flux 是 Project Reactor 里的响应式类型，代表"0 到 N 个元素的异步序列"。

大模型的流式输出是一个 token 一个 token 返回的，Flux 正好能表达这种场景：

```java
Flux<ChatResponse> stream = chatModel.stream(new Prompt("讲个故事"));

stream.subscribe(response -> {
    // 每收到一个 token 就会触发这里
    String token = response.getResult().getOutput().getContent();
    System.out.print(token);  // 你好|，|我是|...
});
```

和传统的同步调用相比：
- 同步调用：等大模型全部生成完，一次性返回
- 流式调用：生成一点返回一点，用户体验更好

在我们项目里，流式响应的处理流程是：
1. 调用 `chatModel.stream()` 得到 Flux
2. 用 `flux.subscribe()` 注册回调
3. 每收到一个 token，就通过 SSE 推送给前端
4. 前端实时渲染，实现"打字机"效果

---

### 32. 什么是 `Advisor`？Spring AI 中的 Advisor 机制是怎么工作的？

**考察点**：Spring AI 高级特性

**参考答案**：
Advisor 是 Spring AI 的拦截器/插件机制，可以在调用大模型前后插入自定义逻辑。

常见的 Advisor 类型：
- **MessageChatMemoryAdvisor**：自动管理对话历史
- **QuestionAnswerAdvisor**：实现 RAG，在调用前检索知识库
- **SafeGuardAdvisor**：内容安全过滤

使用方式：

```java
ChatClient.create(chatModel)
    .defaultAdvisors(
        new MessageChatMemoryAdvisor(chatMemory),  // 添加记忆
        new QuestionAnswerAdvisor(vectorStore)    // 添加 RAG
    )
    .prompt()
    .user("你好")
    .call();
```

工作原理类似于 Spring MVC 的拦截器：

```
用户请求 → Advisor1.before() → Advisor2.before() → ChatModel → Advisor2.after() → Advisor1.after() → 返回
```

在我们项目里，我们自己实现了 LlmChatHistory 来管理对话历史，思路和 Advisor 类似，但更定制化一些。

---

### 33. 你有用过 Spring AI 的 Function Calling 功能吗？是怎么实现的？

**考察点**：工具调用、Function Calling

**参考答案**：
Function Calling 是让大模型能够调用外部工具的能力。比如用户问"今天北京天气怎么样"，大模型自己不知道，但它可以调用天气 API 来获取。

Spring AI 的实现方式：

```java
// 1. 定义工具函数
@Bean
public Function<WeatherRequest, WeatherResponse> getWeather() {
    return request -> weatherService.query(request.getCity());
}

// 2. 注册到 ChatClient
ChatClient.create(chatModel)
    .defaultFunctions("getWeather")
    .prompt()
    .user("今天北京天气怎么样")
    .call();
```

执行流程：
1. 用户提问，发给大模型
2. 大模型判断需要调用工具，返回 function_call 指令
3. Spring AI 自动调用对应的 Java 方法
4. 把结果塞回去，再问一次大模型
5. 大模型根据工具返回的数据生成最终回答

我们项目里的 PluginNode 也是类似的思路，但我们是自己解析大模型的输出来决定调用哪个工具，没有直接用 Spring AI 的 Function Calling。

---

### 34. 简历上提到"buildChatModel 即用即毁的轻量级客户端工厂"，为什么这样设计？

**考察点**：无状态设计、资源管理

**参考答案**：
"即用即毁"是指每次调用都新建一个 ChatModel 实例，用完就丢，不复用。

为什么这样设计？

1. **无状态**：ChatModel 实例不存储任何对话状态，避免并发时状态混乱。比如 A 用户和 B 用户同时对话，如果共用一个实例，可能会串话

2. **支持动态切换**：工作流里不同 LLM 节点可能配置了不同的模型，每次都新建就能用不同的配置

3. **资源隔离**：每个请求有自己的 HTTP 连接，一个请求超时不会影响其他请求

4. **简化代码**：不用考虑实例池化、并发控制这些复杂逻辑

代码大概是这样：

```java
public Flux<ChatResponse> chat(LlmReqBo req) {
    // 每次调用都构建新的 ChatModel
    ChatModel chatModel = buildChatModel(req.getModelConfig());
    return chatModel.stream(buildPrompt(req));
}
```

性能上，构建 ChatModel 的开销很小（就是创建几个对象），真正耗时的是网络请求，所以这个设计不会有性能问题。

---

### 35. Spring AI 和 LangChain4j 有什么区别？你为什么选择 Spring AI？

**考察点**：技术选型

**参考答案**：
两者都是 Java 生态的 AI 应用框架，但有些差异：

| 对比项 | Spring AI | LangChain4j |
|--------|-----------|-------------|
| 生态 | Spring 官方出品，和 Spring Boot 无缝集成 | 独立项目，更轻量 |
| 设计风格 | Spring 风格，注解驱动 | 更偏函数式，链式调用 |
| 模型支持 | 主流模型都支持 | 模型支持也很全 |
| RAG 支持 | 有，但相对简单 | 更完善 |
| 社区活跃度 | Spring 背书，长期维护有保障 | 社区驱动 |

我们选择 Spring AI 的原因：

1. **团队熟悉 Spring 生态**：学习成本低，上手快
2. **官方维护**：不用担心项目停更
3. **和现有代码集成方便**：项目本来就是 Spring Boot，加个依赖就能用

如果是新项目，而且团队对函数式编程更熟悉，LangChain4j 也是不错的选择，它的 Chain 和 Agent 抽象做得更好。

---

### 36. 如果要在 Spring AI 基础上实现 RAG，你会怎么做？

**考察点**：RAG 理解、实现思路

**参考答案**：
RAG（Retrieval Augmented Generation）的核心思路是：**先检索相关文档，再把文档内容塞到 Prompt 里让大模型回答**。

用 Spring AI 实现的步骤：

**1. 文档向量化入库**
```java
// 读取文档，切分成小块
List<Document> docs = new TextSplitter().split(content);
// 计算向量，存入向量数据库
vectorStore.add(docs);
```

**2. 检索相关文档**
```java
// 根据用户问题检索相关文档
List<Document> relevantDocs = vectorStore.similaritySearch(
    SearchRequest.query(userQuestion).withTopK(5)
);
```

**3. 构造增强 Prompt**
```java
String context = relevantDocs.stream()
    .map(Document::getContent)
    .collect(Collectors.joining("\n"));

String prompt = """
    根据以下文档回答问题：
    
    文档内容：
    %s
    
    问题：%s
    """.formatted(context, userQuestion);
```

**4. 调用大模型**
```java
ChatClient.create(chatModel)
    .prompt(prompt)
    .call();
```

Spring AI 也提供了 QuestionAnswerAdvisor 来简化这个流程，但自己实现更灵活。

---

### 37. Spring AI 中怎么处理 Token 超限的问题？

**考察点**：Token 管理、上下文窗口

**参考答案**：
Token 超限是大模型应用常见的问题。大模型有上下文窗口限制，比如 GPT-4 是 8K 或 128K，超了就会报错。

我们的处理方式：

**1. 滑动窗口截断历史消息**

```java
public List<ChatItem> getHistory(String chatId, String nodeId, int limit) {
    List<ChatItem> history = historyMap.get(chatId + nodeId);
    if (history.size() > limit * 2) {
        // 只保留最近的 N 轮对话
        return history.subList(history.size() - limit * 2, history.size());
    }
    return history;
}
```

**2. 长文本摘要**
如果单条消息就很长，先调用大模型做摘要，再塞到 Prompt 里。

**3. Token 计数器**
在发送前估算 Token 数量：
```java
int tokenCount = estimateTokens(messages);
if (tokenCount > MAX_TOKENS) {
    // 压缩或报错
}
```

**4. 返回值里记录消耗**
```java
Usage usage = response.getMetadata().getUsage();
log.info("消耗 Token: prompt={}, completion={}", 
    usage.getPromptTokens(), usage.getCompletionTokens());
```

这样既能避免超限报错，又能监控 Token 消耗做成本控制。

---

## 四、SSE 流式响应（8 道）

### 38. 什么是 SSE（Server-Sent Events）？和 WebSocket 有什么区别？

**考察点**：协议理解

**参考答案**：
SSE 是一种服务器向客户端单向推送数据的技术，基于 HTTP 协议。

和 WebSocket 的主要区别：

| 对比项 | SSE | WebSocket |
|--------|-----|-----------|
| 通信方向 | 单向（服务端→客户端） | 双向 |
| 协议 | HTTP | 独立的 WebSocket 协议 |
| 连接 | 普通 HTTP 连接 | 需要协议升级握手 |
| 数据格式 | 纯文本 | 文本或二进制 |
| 断线重连 | 浏览器自动重连 | 需要自己实现 |
| 兼容性 | 几乎所有浏览器 | 部分老浏览器不支持 |

我们选择 SSE 的原因：
1. **场景匹配**：大模型输出是服务端单向推给前端，不需要双向通信
2. **简单**：不需要额外的协议，nginx 也好配置
3. **兼容性好**：基于 HTTP，防火墙不会拦截

SSE 的数据格式：
```
data: 第一条消息

data: 第二条消息

event: custom
data: 自定义事件
```

---

### 39. 你是怎么用 Spring Boot 实现 SSE 接口的？

**考察点**：SseEmitter 使用

**参考答案**：
Spring Boot 提供了 SseEmitter 类来实现 SSE。

基本用法：

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream() {
    SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
    
    // 异步执行，发送数据
    executor.execute(() -> {
        try {
            for (int i = 0; i < 10; i++) {
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data("第 " + i + " 条消息"));
                Thread.sleep(1000);
            }
            emitter.complete(); // 正常结束
        } catch (Exception e) {
            emitter.completeWithError(e); // 异常结束
        }
    });
    
    return emitter;
}
```

在我们项目里，工作流执行的结果就是通过 SseEmitter 推送的：

```java
SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
StreamCallback callback = new SseStreamCallback(emitter);
workflowEngine.execute(workflowDSL, variablePool, inputs, callback);
return emitter;
```

WorkflowMsgCallback 封装了 emitter.send() 的调用，每次有消息就推送给前端。

---

### 40. SseEmitter 的超时时间怎么设置？如果连接断开了怎么处理？

**考察点**：超时处理、异常处理

**参考答案**：
**超时设置**：

```java
// 构造时设置，单位毫秒
SseEmitter emitter = new SseEmitter(60000L); // 60秒

// 或者设置为 0 表示不超时（不推荐，会占用连接）
SseEmitter emitter = new SseEmitter(0L);
```

我们设置的是 5 分钟，因为有些工作流执行时间比较长。

**连接断开处理**：

```java
SseEmitter emitter = new SseEmitter(300000L);

// 客户端断开连接时的回调
emitter.onCompletion(() -> {
    log.info("SSE 连接正常结束");
    // 清理资源
});

// 超时回调
emitter.onTimeout(() -> {
    log.warn("SSE 连接超时");
    emitter.complete();
});

// 错误回调
emitter.onError(ex -> {
    log.error("SSE 连接异常", ex);
    // 停止工作流执行
});
```

在发送数据时也要做异常处理：

```java
try {
    emitter.send(data);
} catch (IOException e) {
    // 客户端已断开，停止发送
    log.warn("客户端已断开连接");
}
```

---

### 41. 你说"首字响应延迟优化至 200ms 以内"，是怎么做到的？

**考察点**：性能优化

**参考答案**：
首字响应延迟是指从用户发送请求到看到第一个字的时间。优化的关键是**减少各环节的等待时间**。

具体做的优化：

**1. 流式调用大模型 API**
不等大模型全部生成完，而是用流式接口，生成一点返回一点：
```java
chatModel.stream(prompt).subscribe(response -> {
    // 有数据就立即推送
    callback.onNodeProcess(token);
});
```

**2. 减少节点调度开销**
节点执行前的参数解析、日志记录这些操作尽量轻量化。

**3. 连接复用**
用 OkHttp 的连接池，避免每次都重新建立 TCP 连接和 SSL 握手。

**4. 异步化非关键路径**
比如日志落库、metrics 上报这些不影响主流程的操作，异步执行。

**5. JIT 预热**
服务启动后先跑几次模拟请求，让 JVM 把热点代码编译好。

测量方式是在关键节点打时间戳：
```java
long start = System.currentTimeMillis();
// 第一个 token 到达
long firstTokenTime = System.currentTimeMillis() - start;
log.info("首字延迟: {}ms", firstTokenTime);
```

---

### 42. 流式响应过程中，怎么保证消息的有序性？

**考察点**：消息排序、并发控制

**参考答案**：
这是我们遇到的一个坑：多线程往 SseEmitter 写数据，前端收到的顺序可能是乱的。

**问题原因**：
大模型返回的 token 是有序的，但处理线程和发送线程不是同一个，可能存在竞争。

**解决方案——双队列架构**：

```java
// 接收队列：接收大模型返回的 token
Queue<LLMGenerate> streamQueue = new ConcurrentLinkedQueue<>();

// 排序队列：按序号排序后发送
Queue<ChatCallBackStreamResult> orderStreamResultQ = new ConcurrentLinkedQueue<>();
```

处理流程：
1. 大模型返回的每个 token 带一个序号
2. token 先进接收队列
3. 独立的消费者线程从接收队列取数据
4. 按序号排序后放入排序队列
5. 发送线程从排序队列取数据发送

```java
// 消费者线程
while (!finished) {
    LLMGenerate token = streamQueue.poll();
    if (token != null) {
        // 按序号插入到正确位置
        insertOrdered(orderStreamResultQ, token);
    }
    
    // 发送队首的连续消息
    while (canSend(orderStreamResultQ)) {
        emitter.send(orderStreamResultQ.poll());
    }
}
```

这样就保证了前端收到的消息一定是有序的。

---

### 43. 简历上提到"双队列架构消除 SSE 响应乱序与丢包"，能详细解释一下吗？

**考察点**：WorkflowMsgCallback 设计

**参考答案**：
这个是 WorkflowMsgCallback 类的核心设计。

**问题背景**：
- 工作流执行时，可能多个节点同时产生输出
- 每个 LLM 节点的流式输出是异步的
- 直接往 SseEmitter 写，消息会乱序

**双队列设计**：

```java
public class WorkflowMsgCallback {
    // 队列1：数据流队列，存放原始消息
    private final Queue<LLMGenerate> streamQueue;
    
    // 队列2：排序队列，存放待发送的有序消息
    private final Queue<ChatCallBackStreamResult> orderStreamResultQ;
    
    // 消费者线程
    private final Thread consumerThread;
}
```

**工作流程**：

```
生产者(多个)          队列1              消费者            队列2            发送
LLM节点1 ─→ ┐                         ┌─→ 排序 ─→ ┐
LLM节点2 ─→ ├→ streamQueue ─→ 消费者线程 ─→      ├→ orderStreamResultQ ─→ SseEmitter
Plugin节点 ─→ ┘                         └─→ 过滤 ─→ ┘
```

**消费者线程的工作**：
1. 从 streamQueue 取消息
2. 根据消息类型和序号做排序
3. 合并相邻的 token 片段（减少发送次数）
4. 放入 orderStreamResultQ
5. 按顺序发送给 SseEmitter

**防丢包**：
- 用 ConcurrentLinkedQueue 保证线程安全
- 发送失败时记录日志，不影响后续消息
- 结束时调用 `finished()` 方法确保队列清空

---

### 44. 如果客户端断开连接，服务端怎么感知？怎么释放资源？

**考察点**：资源管理、连接管理

**参考答案**：
**感知方式**：

1. **发送时报错**：往 SseEmitter 发送数据时，如果客户端已断开，会抛 IOException

```java
try {
    emitter.send(data);
} catch (IOException e) {
    // 客户端已断开
    log.warn("客户端断开连接");
    handleDisconnect();
}
```

2. **回调通知**：SseEmitter 提供了生命周期回调

```java
emitter.onCompletion(() -> {
    log.info("连接完成");
    cleanup();
});

emitter.onTimeout(() -> {
    log.warn("连接超时");
    cleanup();
});

emitter.onError(ex -> {
    log.error("连接异常", ex);
    cleanup();
});
```

**资源释放**：

```java
private void cleanup() {
    // 1. 停止工作流执行
    workflowEngine.cancel(workflowId);
    
    // 2. 关闭消费者线程
    consumerThread.interrupt();
    
    // 3. 清空队列
    streamQueue.clear();
    orderStreamResultQ.clear();
    
    // 4. 清理上下文
    EngineContextHolder.remove();
}
```

关键是要**及时感知**并**尽快释放**，避免资源泄漏。

---

### 45. SSE 和 HTTP 长轮询相比，有什么优势？

**考察点**：技术对比

**参考答案**：
长轮询是客户端不断发请求问服务端"有没有新数据"，服务端没数据就 hold 住请求，有数据再返回。

SSE 的优势：

| 对比项 | SSE | 长轮询 |
|--------|-----|--------|
| 连接数 | 一个连接持续使用 | 每次返回后要重新建立连接 |
| 实时性 | 服务端有数据立即推送 | 有延迟，取决于轮询间隔 |
| 服务端资源 | 保持一个连接 | 频繁创建/销毁连接 |
| 代码复杂度 | 简单，浏览器原生支持 | 需要自己实现轮询逻辑 |
| 数据格式 | 有标准格式（event/data） | 自定义 |
| 断线重连 | 浏览器自动处理 | 需要自己实现 |

在大模型场景下，SSE 特别合适：
- 模型输出是连续的 token 流，SSE 能实时推送
- 不需要客户端向服务端发数据
- 长轮询会有明显的延迟感，用户体验差

唯一的缺点是 SSE 不支持客户端向服务端发消息，但这个场景下不需要。

---

## 五、大模型调用与 Prompt（10 道）

### 46. 什么是 Prompt Engineering？你在项目中是怎么应用的？

**考察点**：Prompt 理解

**参考答案**：
Prompt Engineering 就是"怎么跟大模型说话"的技术。同样一个问题，问法不同，大模型的回答质量差很多。

在项目中的应用：

**1. 角色设定**
```
你是一个专业的播客主播，擅长把复杂的内容讲得通俗易懂。
```

**2. 任务描述**
```
请把下面的文章改写成适合口播的风格，要求：
- 语言口语化，避免书面语
- 加入适当的过渡语，如"那么"、"接下来"
- 控制在 500 字以内
```

**3. 输出格式约束**
```
请以 JSON 格式返回，包含以下字段：
- title: 标题
- content: 正文
```

**4. Few-shot 示例**
```
示例输入：人工智能是计算机科学的一个分支...
示例输出：大家好，今天咱们来聊聊人工智能。说白了呢，它就是...
```

我们的 LLM 节点支持配置 System Prompt 和 User Prompt，业务人员可以在可视化界面上编辑这些 Prompt，不用改代码。

---

### 47. System Prompt 和 User Prompt 有什么区别？什么时候用 System Prompt？

**考察点**：Prompt 设计

**参考答案**：
**System Prompt**：设定大模型的"人设"和行为准则，是全局性的指令。比如：
```
你是一个专业的法律顾问，只回答法律相关的问题，遇到其他问题请礼貌拒绝。
```

**User Prompt**：用户的具体问题或任务，是每次对话都会变的。比如：
```
我租的房子漏水，房东不修，我该怎么办？
```

**什么时候用 System Prompt**：

1. **定义角色**：让大模型扮演特定角色，如客服、老师、翻译
2. **设置边界**：限制大模型的行为，比如"不要回答政治敏感问题"
3. **指定格式**：要求大模型始终以某种格式输出
4. **提供背景知识**：把一些上下文信息放在 System Prompt 里

在我们代码里：

```java
String systemPrompt = getSystemPrompt(nodeParam, inputs);
String userPrompt = getPrompt(nodeParam, inputs);

if (StringUtils.isNotBlank(systemPrompt)) {
    LlmChatHistory.addMessage(chatId, nodeId, MsgTypeEnum.SYSTEM, systemPrompt);
}
LlmChatHistory.addMessage(chatId, nodeId, MsgTypeEnum.USER, userPrompt);
```

System Prompt 是可选的，如果配置了就加上。

---

### 48. 你的 `VariableTemplateRender` 是怎么实现 Prompt 模板渲染的？

**考察点**：模板引擎、正则解析

**参考答案**：
VariableTemplateRender 是一个轻量级的模板引擎，支持把 `{{nodeId.variableName}}` 格式的变量替换成实际值。

核心实现：

```java
public class VariableTemplateRender {
    // 预编译正则，提高性能
    private static final Pattern PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    public static String render(String template, Map<String, Object> variables) {
        Matcher matcher = PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String varPath = matcher.group(1);  // 如 "start.input"
            String[] parts = varPath.split("\\.");
            
            // 从变量池中取值
            Object value = variables.get(parts[0]);
            if (parts.length > 1 && value instanceof Map) {
                value = ((Map<?, ?>) value).get(parts[1]);
            }
            
            matcher.appendReplacement(result, 
                value != null ? value.toString() : "");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
```

**使用示例**：

模板：
```
请把以下内容改写成播客风格：
{{start.input}}
```

变量：
```json
{
  "start": {
    "input": "人工智能是计算机科学的一个分支..."
  }
}
```

渲染结果：
```
请把以下内容改写成播客风格：
人工智能是计算机科学的一个分支...
```

相比用 FreeMarker 这种重型模板引擎，自己实现的好处是轻量、可控，性能也更好（正则预编译）。

---

### 49. 大模型返回的 JSON 格式不稳定，你是怎么处理的？

**考察点**：容错处理、正则解析

**参考答案**：
这是大模型应用常见的问题。即使你在 Prompt 里要求返回 JSON，大模型有时候会：
- 前面加一句"好的，以下是结果："
- 用 markdown 代码块包起来
- 字段名大小写不一致
- 漏掉某些字段

我们的处理策略：

**1. 提取 JSON 部分**
```java
public static String extractJson(String text) {
    // 尝试找 {} 或 [] 包裹的内容
    Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}|\\[[\\s\\S]*\\]");
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
        return matcher.group();
    }
    return text;
}
```

**2. 宽松解析**
用 FastJSON 的宽松模式，容忍一些格式问题：
```java
JSONObject.parseObject(json, Feature.AllowSingleQuotes, Feature.AllowUnQuotedFieldNames);
```

**3. 默认值兜底**
```java
String title = json.getString("title");
if (title == null) {
    title = "默认标题";
}
```

**4. 重试机制**
如果解析失败，可以重新调用大模型，在 Prompt 里强调"请只返回 JSON，不要有其他内容"。

**5. 使用结构化输出**
一些模型支持 JSON Mode 或 Function Calling，强制返回结构化数据。

---

### 50. 什么是 Token？为什么大模型 API 按 Token 计费？

**考察点**：基础概念

**参考答案**：
Token 是大模型处理文本的基本单位，可以理解为"词片段"。

不是一个汉字就是一个 Token，也不是一个单词就是一个 Token。大模型用的分词算法（如 BPE）会把文本切成更小的片段。比如：

```
"Hello World" → ["Hello", " World"] → 2 个 Token
"你好世界" → ["你好", "世界"] → 2 个 Token（大概）
"ChatGPT" → ["Chat", "G", "PT"] → 3 个 Token
```

**为什么按 Token 计费**：

1. **反映计算量**：大模型处理每个 Token 都要做一次矩阵运算，Token 越多计算越多
2. **公平**：不同语言的字符数差异大，按 Token 计费更公平
3. **限制滥用**：Token 数量有上限，避免用户发送超长内容

**在我们项目中的应用**：

```java
Usage usage = response.getMetadata().getUsage();
GenerateUsage generateUsage = new GenerateUsage();
generateUsage.setPromptTokens(usage.getPromptTokens());      // 输入消耗
generateUsage.setCompletionTokens(usage.getCompletionTokens()); // 输出消耗
generateUsage.setTotalTokens(usage.getTotalTokens());        // 总消耗
```

我们会记录每次调用的 Token 消耗，用于成本统计和监控。

---

### 51. 你是怎么实现"历史记录截断策略"的？滑动窗口是什么意思？

**考察点**：上下文管理、LlmChatHistory

**参考答案**：
大模型的上下文窗口是有限的（比如 4K、8K、128K Token），历史对话太长就会超限。

**滑动窗口策略**：只保留最近 N 轮对话，老的自动丢弃。就像一个固定大小的窗口在对话历史上滑动。

实现代码：

```java
public class LlmChatHistory {
    // chatId + nodeId -> 对话历史
    private static final Map<String, List<ChatItem>> historyMap = new ConcurrentHashMap<>();
    
    public static List<ChatItem> getHistory(String chatId, String nodeId, int limit) {
        String key = chatId + "_" + nodeId;
        List<ChatItem> history = historyMap.get(key);
        
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 只保留最近的 limit 轮（每轮包含 user 和 assistant 两条消息）
        int keepCount = limit * 2;
        if (history.size() > keepCount) {
            return history.subList(history.size() - keepCount, history.size());
        }
        return history;
    }
    
    public static void addMessage(String chatId, String nodeId, MsgTypeEnum type, String content) {
        String key = chatId + "_" + nodeId;
        historyMap.computeIfAbsent(key, k -> new ArrayList<>())
                  .add(new ChatItem(type, content));
    }
}
```

**使用方式**：
```java
// 在 LLMNodeExecutor 中
List<ChatItem> history = LlmChatHistory.getHistory(chatId, nodeId, historyLimit);
req.setHistory(history);
```

节点配置里可以设置 `enableChatHistoryV2.rounds = 5`，表示保留最近 5 轮对话。

---

### 52. 大模型的 Temperature 参数是什么意思？什么时候调高，什么时候调低？

**考察点**：模型参数理解

**参考答案**：
Temperature 控制大模型输出的"随机性"或"创造性"，取值范围一般是 0 到 2。

**原理**：
大模型输出时，会计算每个 Token 的概率。Temperature 影响这些概率的分布：
- Temperature 低：概率高的 Token 更容易被选中，输出更确定
- Temperature 高：概率分布更平均，低概率 Token 也有机会被选中，输出更随机

**调低（0 ~ 0.3）的场景**：
- 代码生成：需要确定性的输出
- 事实性问答：不希望模型"编造"
- JSON 输出：格式要稳定
- 翻译：需要准确

**调高（0.7 ~ 1.0）的场景**：
- 创意写作：需要多样性
- 头脑风暴：希望有意想不到的点子
- 故事创作：不希望太死板
- 对话：希望回复更自然

在我们项目里：
```java
// 配置里可以指定
{
    "temperature": 0.7,
    "maxTokens": 4096
}
```

AI 播客改写场景用的是 0.7，既有创意又不会太离谱。

---

### 53. 如果大模型响应很慢，你会怎么优化？

**考察点**：性能优化思路

**参考答案**：
大模型响应慢可能有几个原因，对症下药：

**1. 网络延迟**
- 选择更近的 API 节点
- 使用国内代理或国内模型（DeepSeek、通义）
- 开启 HTTP/2 复用连接

**2. 输入太长**
- 精简 Prompt，去掉不必要的内容
- 截断历史对话
- 对长文本先做摘要

**3. 输出太长**
- 设置 max_tokens 限制
- 在 Prompt 里要求简洁回答

**4. 模型本身慢**
- 换更快的模型（小参数模型）
- 用 Flash 版本（牺牲一点质量换速度）

**5. 并发限制**
- 做请求排队，避免触发限流
- 购买更高的 QPS 配额

**6. 用户体验优化**
- 流式输出，让用户能看到进度
- 加 loading 动画
- 异步执行，先返回"正在处理"

在我们项目里，主要是用**流式输出**和**连接池复用**：

```java
// 流式输出
chatModel.stream(prompt).subscribe(response -> {
    callback.onNodeProcess(response.getContent());
});

// OkHttp 连接池
ConnectionPool pool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
```

---

### 54. 你说"支持业务方通过配置快速切换底层基座模型"，是怎么实现的？

**考察点**：配置化设计

**参考答案**：
核心思路是**把模型配置从代码里抽出来，存到数据库**。

**数据库表设计**：
```sql
CREATE TABLE model_config (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),         -- 配置名称，如 "DeepSeek-Chat"
    provider VARCHAR(20),     -- 厂商：openai, deepseek, zhipu
    base_url VARCHAR(200),    -- API 地址
    api_key VARCHAR(200),     -- 密钥（加密存储）
    model VARCHAR(50),        -- 模型名
    temperature DECIMAL(2,1), -- 默认温度
    max_tokens INT            -- 默认最大 Token
);
```

**节点配置引用模型**：
```json
{
    "nodeType": "LLM",
    "nodeParam": {
        "modelId": 123,  // 引用模型配置
        "template": "请帮我..."
    }
}
```

**运行时动态加载**：
```java
public ChatModel buildChatModel(Integer modelId) {
    ModelConfig config = modelConfigMapper.selectById(modelId);
    return buildChatModel(config);
}
```

**切换模型的方式**：
1. 在管理后台修改模型配置，立即生效
2. 或者在工作流编辑器里，选择另一个模型配置

这样，运营同学想试试新模型，直接在后台加一条配置，工作流里选一下就行，不用发版。

---

### 55. 大模型的"幻觉"问题你了解吗？怎么缓解？

**考察点**：大模型局限性理解

**参考答案**：
幻觉（Hallucination）是指大模型"一本正经地胡说八道"，生成的内容看起来很合理，但其实是错的。

比如问"鲁迅和周树人的关系"，模型可能回答"他们是好朋友"...

**为什么会有幻觉**：
- 大模型是基于概率生成文本，不是查数据库
- 训练数据可能有错误或过时
- 模型倾向于"编"出一个流畅的回答

**缓解方法**：

**1. RAG（检索增强生成）**
先从知识库检索相关内容，把真实的资料塞到 Prompt 里：
```
根据以下文档回答问题：
[检索到的真实内容]
问题：xxx
```

**2. 降低 Temperature**
减少随机性，让模型更保守：
```java
options.setTemperature(0.2);
```

**3. 明确要求**
在 Prompt 里说"如果不确定，请回答'我不知道'"

**4. 多次采样**
同一个问题问多次，看答案是否一致

**5. 事实核查**
让另一个模型检查输出是否正确

**6. 限定范围**
"只根据我提供的信息回答，不要使用外部知识"

在我们的 AI 播客场景里，幻觉问题不太严重，因为是改写用户提供的内容，不是让模型自己编。但如果做问答类应用，RAG 是必须的。

---

## 六、并发编程（10 道）

### 56. Java 中的线程池有哪些核心参数？你在项目中是怎么配置的？

**考察点**：ThreadPoolExecutor 参数

**参考答案**：
ThreadPoolExecutor 有 7 个核心参数：

```java
new ThreadPoolExecutor(
    corePoolSize,      // 核心线程数
    maximumPoolSize,   // 最大线程数
    keepAliveTime,     // 空闲线程存活时间
    timeUnit,          // 时间单位
    workQueue,         // 工作队列
    threadFactory,     // 线程工厂
    rejectedHandler    // 拒绝策略
);
```

在项目里，我们针对不同场景配置了不同的线程池：

```java
// 工作流节点执行线程池
ThreadPoolExecutor nodeExecutor = new ThreadPoolExecutor(
    10,                           // 核心 10 个线程
    50,                           // 最大 50 个线程
    60L, TimeUnit.SECONDS,        // 空闲 60 秒回收
    new SynchronousQueue<>(),     // 不排队，直接创建线程
    new NamedThreadFactory("node-executor"),
    new CallerRunsPolicy()        // 满了就调用者线程执行
);
```

配置思路：
- **核心线程数**：根据 CPU 核数和任务类型定。IO 密集型可以多一些
- **最大线程数**：根据系统资源和并发量定
- **队列**：用 SynchronousQueue 是因为我们希望任务尽快执行，不要排队
- **拒绝策略**：CallerRunsPolicy 可以起到"限流"作用

---

### 57. 为什么用 `SynchronousQueue` 而不是 `LinkedBlockingQueue`？

**考察点**：阻塞队列选型

**参考答案**：
SynchronousQueue 是一个"没有容量"的队列，每个 put 必须等待一个 take。

**选 SynchronousQueue 的原因**：

1. **即时响应**：工作流执行对延迟敏感，不希望任务在队列里等着
2. **弹性扩容**：队列满了就创建新线程，能快速应对突发流量
3. **避免堆积**：如果用 LinkedBlockingQueue，任务可能堆积很多才发现系统过载

**对比**：

| 特性 | SynchronousQueue | LinkedBlockingQueue |
|------|------------------|---------------------|
| 容量 | 0 | 可配置（默认 Integer.MAX_VALUE） |
| 入队 | 必须有消费者在等 | 直接入队 |
| 适合场景 | 追求低延迟 | 允许排队 |
| 风险 | 可能创建很多线程 | 可能堆积很多任务 |

```java
// SynchronousQueue：来一个任务，要么有线程处理，要么创建新线程
new ThreadPoolExecutor(10, 50, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

// LinkedBlockingQueue：来一个任务，先排队
new ThreadPoolExecutor(10, 50, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000));
```

---

### 58. CompletableFuture 和 Future 有什么区别？你是怎么用的？

**考察点**：异步编程

**参考答案**：
**Future** 是 Java 5 引入的，只能阻塞等待结果：
```java
Future<String> future = executor.submit(() -> "result");
String result = future.get(); // 阻塞等待
```

**CompletableFuture** 是 Java 8 引入的，支持链式调用和回调：
```java
CompletableFuture.supplyAsync(() -> "step1")
    .thenApply(s -> s + " step2")
    .thenAccept(System.out::println)
    .exceptionally(ex -> { log.error(ex); return null; });
```

**主要区别**：

| 特性 | Future | CompletableFuture |
|------|--------|-------------------|
| 获取结果 | 只能阻塞 get() | 支持回调 |
| 链式调用 | 不支持 | 支持 |
| 异常处理 | 需要 try-catch | exceptionally() |
| 组合多个异步 | 不支持 | allOf(), anyOf() |

**在项目中的使用**：

```java
// 并行执行多个节点
List<CompletableFuture<Void>> futures = nextNodes.stream()
    .map(node -> CompletableFuture.runAsync(() -> {
        executeNode(node, variablePool, callback);
    }, executor))
    .collect(Collectors.toList());

// 等待所有节点完成
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

---

### 59. @Async 注解是怎么工作的？有什么需要注意的地方？

**考察点**：Spring 异步

**参考答案**：
@Async 是 Spring 提供的注解，让方法异步执行。

**工作原理**：
Spring 通过 AOP 代理实现。调用 @Async 方法时，实际上是代理对象把任务提交到线程池。

```java
@Async
public void asyncMethod() {
    // 会在另一个线程执行
}
```

**需要注意的坑**：

**1. 同类调用不生效**
```java
public void methodA() {
    methodB(); // ❌ 直接调用，@Async 不生效
}

@Async
public void methodB() { }
```
因为同类调用不走代理。解决办法是注入自己或者拆到另一个类。

**2. 需要开启 @EnableAsync**
```java
@EnableAsync
@SpringBootApplication
public class Application { }
```

**3. 返回值只能是 void 或 Future**
```java
@Async
public Future<String> asyncWithReturn() {
    return new AsyncResult<>("result");
}
```

**4. 异常处理**
异步方法的异常不会抛到调用者，需要单独配置 AsyncUncaughtExceptionHandler。

**5. 指定线程池**
```java
@Async("customExecutor")  // 使用指定的线程池
public void asyncMethod() { }
```

---

### 60. 什么是 TTL（TransmittableThreadLocal）？为什么要用它？

**考察点**：线程上下文传递

**参考答案**：
TTL 是阿里开源的工具，解决**线程池场景下 ThreadLocal 值传递**的问题。

**问题背景**：
ThreadLocal 只能在当前线程取值。如果用线程池，任务提交到另一个线程执行，ThreadLocal 的值就取不到了。

```java
ThreadLocal<String> context = new ThreadLocal<>();
context.set("userId");

executor.execute(() -> {
    context.get(); // ❌ 是 null，因为是另一个线程
});
```

**TTL 的解决方案**：
```java
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();
context.set("userId");

// 包装线程池
Executor ttlExecutor = TtlExecutors.getTtlExecutor(executor);

ttlExecutor.execute(() -> {
    context.get(); // ✅ 能取到 "userId"
});
```

**在项目中的应用**：

```java
// 工作流执行上下文
public class EngineContextHolder {
    private static final TransmittableThreadLocal<EngineContext> CONTEXT = 
        new TransmittableThreadLocal<>();
    
    public static void initContext(String flowId, String chatId, WorkflowMsgCallback callback) {
        CONTEXT.set(new EngineContext(flowId, chatId, callback));
    }
    
    public static EngineContext get() {
        return CONTEXT.get();
    }
}
```

这样即使节点在线程池里执行，也能获取到工作流的上下文信息。

---

### 61. 你的 `AsyncUtil` 是怎么封装的？解决了什么问题？

**考察点**：工具类设计

**参考答案**：
AsyncUtil 是一个异步工具类，封装了超时控制和上下文传递。

主要功能：

```java
public class AsyncUtil {
    
    // 带超时的同步调用
    public static <T> T callWithTimeLimit(long timeout, TimeUnit unit, Callable<T> callable) 
            throws TimeoutException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(callable);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }
    
    // 全局线程池（带 TTL）
    private static final ExecutorService GLOBAL_EXECUTOR = TtlExecutors.getTtlExecutorService(
        new ThreadPoolExecutor(
            10, 50, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new NamedThreadFactory("async-util", true), // daemon 线程
            new CallerRunsPolicy()
        )
    );
    
    public static void runAsync(Runnable task) {
        GLOBAL_EXECUTOR.execute(task);
    }
}
```

**解决的问题**：

1. **超时控制**：节点执行可以设置超时时间，避免一直卡住
2. **上下文传递**：用 TTL 包装线程池，确保上下文不丢失
3. **线程命名**：自定义 ThreadFactory，方便排查问题
4. **守护线程**：设置为 daemon，JVM 退出时自动回收
5. **统一管理**：全局一个线程池，避免到处 new Executor

---

### 62. 多线程环境下，VariablePool 是怎么保证线程安全的？

**考察点**：并发安全

**参考答案**：
VariablePool 存储的是工作流执行过程中的变量，多个节点可能并发读写。

**线程安全设计**：

```java
public class VariablePool {
    // 用 ConcurrentHashMap 保证并发安全
    private final Map<String, Map<String, Object>> pool = new ConcurrentHashMap<>();
    
    public void set(String nodeId, String name, Object value) {
        pool.computeIfAbsent(nodeId, k -> new ConcurrentHashMap<>())
            .put(name, deepCopy(value));
    }
    
    public Object get(String nodeId, String name) {
        Map<String, Object> nodeVars = pool.get(nodeId);
        if (nodeVars == null) return null;
        return deepCopy(nodeVars.get(name));
    }
    
    // 深拷贝，避免数据被意外修改
    private Object deepCopy(Object obj) {
        if (obj == null) return null;
        return JSON.parseObject(JSON.toJSONString(obj), obj.getClass());
    }
}
```

**关键点**：

1. **ConcurrentHashMap**：底层用 CAS + synchronized 实现线程安全
2. **深拷贝**：每次 get/set 都做深拷贝，避免节点 A 修改了对象影响节点 B
3. **computeIfAbsent**：原子性地"检查并创建"，避免并发问题

**为什么要深拷贝**：
```java
// 如果不深拷贝
Map<String, Object> output = (Map) variablePool.get("node1", "output");
output.put("modified", true); // 会影响其他节点拿到的值

// 深拷贝后
Map<String, Object> output = (Map) variablePool.get("node1", "output");
output.put("modified", true); // 不影响原始值
```

---

### 63. 什么是 CAS（Compare And Swap）？ConcurrentLinkedQueue 用到了吗？

**考察点**：无锁编程

**参考答案**：
CAS 是一种乐观锁机制，原理是"比较并交换"：

```
CAS(内存地址, 期望值, 新值)
如果 内存地址的值 == 期望值，就把它改成新值，返回 true
否则不修改，返回 false
```

**优点**：不用加锁，性能好
**缺点**：ABA 问题，竞争激烈时自旋消耗 CPU

**ConcurrentLinkedQueue 的实现**：
它是一个基于 CAS 的无锁队列。入队时：

```java
// 简化版逻辑
void offer(E e) {
    Node<E> newNode = new Node<>(e);
    while (true) {
        Node<E> t = tail;
        Node<E> next = t.next;
        if (next == null) {
            // 尝试 CAS 把 newNode 挂到 tail 后面
            if (CAS(t.next, null, newNode)) {
                // 成功，更新 tail
                CAS(tail, t, newNode);
                return;
            }
        } else {
            // tail 落后了，先更新 tail
            CAS(tail, t, next);
        }
    }
}
```

没有 synchronized，多个线程可以同时入队，失败了就重试。

在我们项目里，用 ConcurrentLinkedQueue 存储就绪节点和消息队列，正是看中它的高性能。

---

### 64. 简历上说"支持单机并发处理 500+ 工作流节点"，怎么测试的？

**考察点**：压测、性能指标

**参考答案**：
这个数据是通过压测得出的。

**测试方法**：

1. **准备测试数据**：创建一个包含 50 个节点的工作流（10 个串行分支 × 5 个节点）

2. **并发执行**：用 JMeter 或代码模拟 10 个并发请求

3. **观察指标**：
   - 吞吐量（TPS）
   - 响应时间（P99）
   - CPU/内存使用率
   - 线程池状态

**测试代码**：
```java
@Test
void concurrencyTest() {
    int concurrency = 10;
    CountDownLatch latch = new CountDownLatch(concurrency);
    
    long start = System.currentTimeMillis();
    for (int i = 0; i < concurrency; i++) {
        executor.execute(() -> {
            workflowEngine.execute(workflowDSL, variablePool, inputs, callback);
            latch.countDown();
        });
    }
    latch.await();
    long cost = System.currentTimeMillis() - start;
    
    System.out.println("总耗时: " + cost + "ms");
    System.out.println("平均TPS: " + (concurrency * 50 * 1000 / cost));
}
```

**测试结果**：
- 10 并发 × 50 节点 = 500 节点同时执行
- 平均执行时间：3 秒左右
- CPU：70% 左右
- 没有 OOM，没有线程死锁

当然这个数据只是"节点调度"的性能，不包括 LLM 调用的时间。

---

### 65. 如果线程池满了，任务会怎么处理？你用的什么拒绝策略？

**考察点**：拒绝策略

**参考答案**：
线程池满了是指：核心线程都在忙 + 队列满了 + 已达最大线程数。这时新任务会被拒绝。

**四种内置拒绝策略**：

| 策略 | 行为 | 适用场景 |
|------|------|----------|
| AbortPolicy | 抛 RejectedExecutionException | 默认，能感知过载 |
| CallerRunsPolicy | 调用者线程执行 | 不想丢任务 |
| DiscardPolicy | 静默丢弃 | 允许丢任务 |
| DiscardOldestPolicy | 丢弃队列头的任务 | 新任务更重要 |

**我们用的是 CallerRunsPolicy**：

```java
new ThreadPoolExecutor(
    10, 50, 60L, TimeUnit.SECONDS,
    new SynchronousQueue<>(),
    new NamedThreadFactory("node-executor"),
    new CallerRunsPolicy()  // 调用者线程执行
);
```

选这个的原因：
1. **不丢任务**：工作流节点不能丢
2. **自动限流**：调用者线程被占用后，就不会再提交新任务，起到背压作用
3. **简单**：不用额外处理拒绝异常

**缺点**：
如果调用者线程是主线程，会阻塞主线程。但在我们场景里，调用者本身也是线程池里的线程，影响不大。

---

## 七、设计模式（8 道）

### 66. 你在项目中用到了哪些设计模式？能举几个例子吗？

**考察点**：设计模式应用

**参考答案**：
项目里用到的主要设计模式：

**1. 策略模式（NodeExecutor）**
不同类型节点有不同的执行逻辑，通过接口多态实现：
```java
public interface NodeExecutor {
    NodeTypeEnum getNodeType();
    NodeRunResult execute(NodeState state);
}
// LLMNodeExecutor, PluginNodeExecutor, StartNodeExecutor...
```

**2. 模板方法模式（AbstractNodeExecutor）**
父类定义执行骨架，子类实现具体逻辑：
```java
public abstract class AbstractNodeExecutor {
    public NodeRunResult execute() {
        onNodeStart();
        resolveInputs();
        NodeRunResult result = executeNode(); // 子类实现
        storeOutputs();
        return result;
    }
}
```

**3. 工厂模式（ChatModel 构建）**
根据配置动态创建不同的模型客户端：
```java
public ChatModel buildChatModel(ModelConfig config) {
    switch (config.getProvider()) {
        case "openai": return new OpenAiChatModel(...);
        case "zhipu": return new ZhiPuChatModel(...);
    }
}
```

**4. 观察者模式（回调机制）**
节点执行状态变化通过回调通知：
```java
callback.onNodeStart(nodeId, nodeName);
callback.onNodeProcess(token);
callback.onNodeEnd(nodeId, result);
```

**5. 责任链模式（节点链路执行）**
节点按顺序依次执行，一个执行完触发下一个。

---

### 67. NodeExecutor 接口的多态实现体现了什么设计原则？

**考察点**：开闭原则、策略模式

**参考答案**：
主要体现了**开闭原则（OCP）**和**依赖倒置原则（DIP）**。

**开闭原则**：对扩展开放，对修改关闭。
- 新增节点类型只需要新增一个 Executor 实现类
- 不需要修改 WorkflowEngine 的代码

```java
// 新增一个节点类型，只需要新增这一个类
@Component
public class CodeNodeExecutor extends AbstractNodeExecutor {
    @Override
    public NodeTypeEnum getNodeType() {
        return NodeTypeEnum.CODE;
    }
    
    @Override
    protected NodeRunResult executeNode(NodeState state, Map<String, Object> inputs) {
        // 执行代码节点的逻辑
    }
}
```

**依赖倒置原则**：高层模块不依赖低层模块，都依赖抽象。
```java
// WorkflowEngine 依赖接口，不依赖具体实现
public class WorkflowEngine {
    private final Map<NodeTypeEnum, NodeExecutor> executors;
    
    // 通过 Spring 自动注入所有实现
    public WorkflowEngine(List<NodeExecutor> executors) {
        for (NodeExecutor executor : executors) {
            this.executors.put(executor.getNodeType(), executor);
        }
    }
}
```

这样设计的好处是：系统的扩展性很好，加新功能不会影响已有功能。

---

### 68. 简历上提到"责任链模式"，在工作流中是怎么应用的？

**考察点**：责任链模式

**参考答案**：
责任链模式的核心是：把请求沿着链条传递，每个节点处理完后交给下一个。

在工作流里，节点之间的执行关系就是一条链：

```
Start → LLM节点 → Plugin节点 → End
```

实现方式：

```java
// Node 类有 nextNodes 列表
public class Node {
    private List<Node> nextNodes = new ArrayList<>();
    private List<Node> failNodes = new ArrayList<>();
}

// 执行时递归调用下一个
private void executeNode(Node node, ...) {
    // 1. 执行当前节点
    NodeRunResult result = executor.execute(node);
    
    // 2. 根据结果选择下一步
    if (result.isSuccess()) {
        // 传递给 nextNodes
        for (Node next : node.getNextNodes()) {
            executeNode(next, ...);
        }
    } else {
        // 传递给 failNodes
        for (Node fail : node.getFailNodes()) {
            executeNode(fail, ...);
        }
    }
}
```

和标准责任链的区别是：我们支持**分支**（多个后继节点）和**条件**（正常/异常分支）。

---

### 69. 工厂模式在你的项目中是怎么用的？

**考察点**：工厂模式

**参考答案**：
主要用在 ChatModel 的构建上。

**问题**：不同的大模型厂商，创建客户端的方式不一样。业务代码不应该关心这些细节。

**解决方案**：用工厂方法封装创建逻辑。

```java
public class ModelClientFactory {
    
    public ChatModel buildChatModel(ModelConfig config) {
        String provider = config.getProvider();
        
        switch (provider) {
            case "openai":
            case "deepseek":
                // 兼容 OpenAI 协议的模型
                return OpenAiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .defaultOptions(buildOptions(config))
                    .build();
                    
            case "zhipu":
                return ZhiPuAiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .build();
                    
            default:
                throw new IllegalArgumentException("不支持的模型: " + provider);
        }
    }
}
```

**使用方**只需要：
```java
ChatModel model = factory.buildChatModel(config);
model.call(prompt);
```

不用关心具体是哪个模型、怎么配置。后续加新模型也只改工厂类。

---

### 70. 装饰器模式你是怎么用的？举个例子。

**考察点**：装饰器模式

**参考答案**：
装饰器模式用来"增强"一个对象的功能，而不改变它的接口。

在项目里，超时控制就是装饰器思想：

```java
// 原始执行逻辑
NodeRunResult doExecute(NodeState state) {
    return executeNode(state, inputs);
}

// 增强：加上超时控制
NodeRunResult doExecuteWithTimeout(NodeState state, RetryConfig config) {
    if (config.timeOutEnabled()) {
        // 装饰：包一层超时逻辑
        return AsyncUtil.callWithTimeLimit(
            config.toMillis(), 
            TimeUnit.MILLISECONDS,
            () -> doExecute(state)  // 调用原始逻辑
        );
    }
    return doExecute(state);
}
```

另一个例子是 TTL 包装线程池：

```java
// 原始线程池
ExecutorService executor = new ThreadPoolExecutor(...);

// 装饰：加上上下文传递能力
ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);
```

装饰后的 executor 和原来接口一样，但多了传递 ThreadLocal 的能力。

---

### 71. 观察者模式（事件驱动）在项目中的应用是什么？

**考察点**：观察者模式、Spring Event

**参考答案**：
观察者模式的核心是：当一个对象状态变化时，自动通知所有关注它的对象。

在项目里主要体现在两个地方：

**1. 工作流回调机制**

```java
// 定义回调接口（观察者）
public interface StreamCallback {
    void onNodeStart(String nodeId, String nodeName);
    void onNodeProcess(String token);
    void onNodeEnd(String nodeId, NodeRunResult result);
}

// 节点执行时通知观察者
public class AbstractNodeExecutor {
    protected void doExecute(NodeState state) {
        callback.onNodeStart(nodeId, nodeName);  // 通知：开始
        
        NodeRunResult result = executeNode(...);
        
        callback.onNodeEnd(nodeId, result);      // 通知：结束
    }
}
```

**2. Spring Event**

Spring 原生支持事件驱动：

```java
// 定义事件
public class UserNicknameUpdatedEvent extends ApplicationEvent {
    private String userId;
    private String newNickname;
}

// 发布事件
eventPublisher.publishEvent(new UserNicknameUpdatedEvent(userId, nickname));

// 监听事件
@EventListener
public void onNicknameUpdated(UserNicknameUpdatedEvent event) {
    // 更新缓存、通知其他服务...
}
```

事件驱动的好处是**解耦**：发布者不需要知道有哪些监听者。

---

### 72. 单例模式在项目中有哪些应用？Spring 的 Bean 是单例吗？

**考察点**：单例模式

**参考答案**：
**Spring Bean 默认是单例的**。

```java
@Component  // 默认 scope = singleton
public class WorkflowEngine { }

@Scope("prototype")  // 每次注入都创建新实例
public class SomeBean { }
```

项目里用到单例的场景：

**1. 线程池**
```java
private static final ExecutorService EXECUTOR = 
    Executors.newFixedThreadPool(10);
```
线程池应该全局共享，不能每次都创建。

**2. 连接池**
```java
// OkHttp 的连接池是单例
private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
    .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
    .build();
```

**3. 配置类**
```java
@Configuration
public class AppConfig {
    // Configuration 类默认单例
}
```

**单例要注意的问题**：
- 线程安全：多个线程可能同时访问
- 状态管理：单例一般不应该有可变状态
- 懒加载 vs 饿汉：Spring 默认是懒加载

---

### 73. 模板方法模式在 AbstractNodeExecutor 中是怎么体现的？

**考察点**：模板方法模式

**参考答案**：
模板方法模式的核心是：**父类定义算法骨架，把某些步骤延迟到子类实现**。

在 AbstractNodeExecutor 中：

```java
public abstract class AbstractNodeExecutor implements NodeExecutor {
    
    // 模板方法：定义执行流程
    @Override
    public final NodeRunResult execute(NodeState nodeState) {
        // 步骤1：检查重试配置
        RetryConfig retryConfig = node.getData().getRetryConfig();
        
        // 步骤2：带超时执行（可选）
        if (retryConfig != null && retryConfig.timeOutEnabled()) {
            return doExecuteWithTimeout(nodeState, retryConfig);
        }
        
        // 步骤3：普通执行
        return doExecute(nodeState);
    }
    
    protected NodeRunResult doExecute(NodeState nodeState) {
        // 步骤1：发送开始事件
        callback.onNodeStart(nodeId, nodeName);
        
        // 步骤2：解析输入
        Map<String, Object> inputs = resolveInputs(node, variablePool);
        
        // 步骤3：执行节点（抽象方法，子类实现）
        NodeRunResult result = executeNode(nodeState, inputs);
        
        // 步骤4：保存输出
        storeOutputs(node, result.getOutputs(), variablePool);
        
        // 步骤5：发送结束事件
        callback.onNodeEnd(nodeId, result);
        
        return result;
    }
    
    // 抽象方法：由子类实现
    protected abstract NodeRunResult executeNode(NodeState nodeState, Map<String, Object> inputs);
}
```

子类只需要关注自己的核心逻辑：

```java
public class LLMNodeExecutor extends AbstractNodeExecutor {
    @Override
    protected NodeRunResult executeNode(NodeState state, Map<String, Object> inputs) {
        // 只需要实现调用 LLM 的逻辑
        // 其他都由父类处理
    }
}
```

---

## 八、微服务架构（8 道）

### 74. PaiFlow 的微服务是怎么划分的？每个服务的职责是什么？

**考察点**：服务拆分

**参考答案**：
按照业务能力和技术特点，拆成了这几个服务：

| 服务 | 端口 | 职责 |
|------|------|------|
| Console Hub | 8080 | 用户管理、权限、工作流 CRUD、对外 API 网关 |
| Workflow Engine | 7880 | 工作流执行引擎，核心调度逻辑 |
| AI Tools | 18668 | AI 能力封装，如语音合成 |
| Link | 动态 | 外部工具连接器，对接第三方 API |
| Frontend | 80 | React 前端，可视化编排界面 |

**拆分原则**：
1. **单一职责**：每个服务只做一件事
2. **独立部署**：可以单独扩容
3. **技术异构**：不同服务可以用不同语言（Java/Python）
4. **数据隔离**：每个服务有自己的数据库

比如 Workflow Engine 是计算密集型的，可能需要多实例；AI Tools 依赖讯飞 SDK，单独维护版本更方便。

---

### 75. 服务之间是怎么通信的？用的 HTTP 还是 RPC？

**考察点**：服务间调用

**参考答案**：
我们用的是 **HTTP REST**，没有用 gRPC 或 Dubbo。

原因：
1. **简单**：HTTP 调试方便，curl 就能测
2. **技术栈混合**：Java 和 Python 服务都有，HTTP 兼容性好
3. **流量不大**：我们的场景不需要 RPC 那么高的性能

调用方式：

```java
// Console Hub 调用 Workflow Engine
RestTemplate restTemplate = new RestTemplate();
String url = "http://core-workflow:7880/api/workflow/execute";
ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
```

流式场景用 **SSE**：
```java
// Workflow Engine 返回 SSE 流
SseEmitter emitter = new SseEmitter();
// 前端直连 Workflow Engine 的 SSE 端点
```

服务发现用 **Docker 内网 DNS**：
```yaml
# docker-compose.yml
services:
  core-workflow:
    hostname: core-workflow  # 其他服务用这个名字访问
```

---

### 76. 什么是服务注册与发现？你的项目用了吗？

**考察点**：服务治理

**参考答案**：
服务注册与发现是微服务的基础设施：
- **注册**：服务启动时告诉注册中心"我在这"
- **发现**：调用方从注册中心查询目标服务的地址

常见方案：Nacos、Eureka、Consul、etcd。

**我们项目没有用传统的注册中心**，因为：
1. 服务数量少（就几个），不需要复杂的服务治理
2. 用 Docker Compose 部署，容器名就是服务名，天然支持 DNS 发现

```yaml
# Docker 内置 DNS
services:
  console-hub:
    environment:
      WORKFLOW_URL: http://core-workflow:7880  # 直接用容器名
```

如果后续要做 K8s 部署或者服务数量变多，可以考虑接入 Nacos。

---

### 77. 如果 Workflow 服务挂了，会影响哪些功能？怎么保证高可用？

**考察点**：高可用设计

**参考答案**：
**影响范围**：
- 工作流执行全部失败
- 用户看到"服务不可用"
- 正在执行的工作流中断

**高可用方案**：

1. **多实例部署**
```yaml
services:
  core-workflow:
    deploy:
      replicas: 3  # 部署 3 个实例
```

2. **负载均衡**
用 Nginx 做反向代理，轮询分发请求：
```nginx
upstream workflow {
    server core-workflow-1:7880;
    server core-workflow-2:7880;
    server core-workflow-3:7880;
}
```

3. **健康检查**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:7880/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
```

4. **无状态设计**
Workflow Engine 不存储会话状态，任何一个实例都能处理请求。

5. **重试机制**
调用方做重试，一个实例失败了换另一个。

目前我们是单实例部署，后续生产环境会做多实例 + 负载均衡。

---

### 78. 你是怎么做链路追踪的？OpenTelemetry 是什么？

**考察点**：可观测性

**参考答案**：
链路追踪是用来排查分布式系统问题的：一个请求经过了哪些服务，每一步花了多长时间。

**OpenTelemetry（OTel）** 是 CNCF 的开源项目，提供统一的可观测性标准：
- **Traces**：链路追踪
- **Metrics**：指标监控
- **Logs**：日志收集

在 Python 服务里的使用：

```python
from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

# 初始化
tracer = trace.get_tracer(__name__)

# 在代码中埋点
with tracer.start_as_current_span("execute_workflow") as span:
    span.set_attribute("workflow_id", workflow_id)
    # 执行逻辑
```

**我们项目的实践**：
- Python 服务集成了 OTel，上报到 Jaeger
- Java 服务用 Spring 的日志 + 自定义 traceId
- 通过 workflowId 和 sessionId 做全链路关联

全链路 ID 设计：
```java
// 在入口生成 traceId，一路传递
String traceId = IdUtil.generateTraceId();
EngineContextHolder.initContext(flowId, traceId, callback);
log.info("[{}] 开始执行工作流", traceId);
```

---

### 79. 微服务的配置管理是怎么做的？

**考察点**：配置中心

**参考答案**：
我们没有用 Nacos 或 Apollo 这种配置中心，用的是**环境变量 + Docker Compose**。

```yaml
# docker-compose.yml
services:
  core-workflow:
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/workflow
      - REDIS_HOST=redis
      - MINIO_ENDPOINT=http://minio:9000
    env_file:
      - .env  # 敏感配置放这里
```

```properties
# .env 文件（不提交到 git）
DEEPSEEK_API_KEY=sk-xxx
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=xxx
```

**为什么不用配置中心**：
1. 项目规模小，配置不多
2. 配置变更不频繁，改完重启就行
3. 减少依赖，部署更简单

**如果要升级**，可以用：
- **Spring Cloud Config**：Git 托管配置
- **Nacos**：配置 + 注册中心一体
- **Kubernetes ConfigMap**：云原生方案

---

### 80. 服务之间的数据一致性怎么保证？

**考察点**：分布式事务

**参考答案**：
分布式事务是微服务的经典难题。我们项目里主要用**最终一致性**方案。

**场景举例**：
用户创建工作流 → Console Hub 存数据库 → 同步到 Workflow Engine

**我们的做法**：

1. **同步调用 + 本地事务**
大部分场景是同步的，Console Hub 调 Workflow Engine，失败就回滚。

2. **幂等设计**
接口设计成幂等的，重复调用不会产生副作用：
```java
// 用 workflowId 做幂等键
if (exists(workflowId)) {
    return existingResult;
}
```

3. **补偿机制**
如果下游失败，上游做补偿：
```java
try {
    workflowService.execute(workflow);
} catch (Exception e) {
    // 记录失败状态，后续重试或人工处理
    failureRecordService.save(workflow);
}
```

4. **异步场景用消息队列**
如果需要真正的异步解耦，可以用 Kafka：
```
Console Hub → Kafka → Workflow Engine
```
消息队列保证最终一致性。

我们目前没有强一致性需求，最终一致就够了。

---

### 81. 如果要做服务降级，你会怎么设计？

**考察点**：熔断降级

**参考答案**：
服务降级是指：当下游服务不可用时，返回一个"兜底"结果，而不是直接报错。

**降级策略**：

1. **返回默认值**
```java
public String callLLM(String prompt) {
    try {
        return llmService.chat(prompt);
    } catch (Exception e) {
        return "服务繁忙，请稍后再试";  // 降级响应
    }
}
```

2. **返回缓存**
```java
public Workflow getWorkflow(Long id) {
    try {
        return workflowService.getById(id);
    } catch (Exception e) {
        return cache.get(id);  // 返回缓存的旧数据
    }
}
```

3. **熔断器（Circuit Breaker）**
用 Resilience4j 或 Sentinel：
```java
@CircuitBreaker(name = "llm", fallbackMethod = "fallback")
public String callLLM(String prompt) {
    return llmService.chat(prompt);
}

public String fallback(String prompt, Exception e) {
    return "AI 服务暂时不可用";
}
```

熔断器的三个状态：
- **CLOSED**：正常调用
- **OPEN**：直接走降级，不再调用下游
- **HALF_OPEN**：试探性调用，成功则恢复

**我们项目的实践**：
节点执行失败时，根据配置的 ErrorStrategy 决定是报错还是用默认值继续：
```java
if (errorStrategy == ErrorStrategyEnum.ERR_CODE) {
    return config.getCustomOutput();  // 返回配置的默认值
}
```

---

## 九、Docker / DevOps（5 道）

### 82. Docker Compose 中的 depends_on 和健康检查有什么区别？

**考察点**：Docker Compose

**参考答案**：
**depends_on** 只保证容器**启动顺序**，不保证服务**真正可用**。

```yaml
services:
  app:
    depends_on:
      - mysql  # 只保证 mysql 容器先启动
```

问题：MySQL 容器启动了，但数据库可能还没初始化完，app 连接就会失败。

**健康检查** 才能保证服务真正可用：

```yaml
services:
  mysql:
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
      
  app:
    depends_on:
      mysql:
        condition: service_healthy  # 等 MySQL 健康检查通过才启动
```

**我们项目的配置**：
```yaml
mysql:
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-proot123"]
    interval: 10s
    timeout: 5s
    retries: 10

core-workflow:
  depends_on:
    mysql:
      condition: service_healthy
    redis:
      condition: service_healthy
```

这样就能保证 Workflow 服务启动时，MySQL 和 Redis 都是可用的。

---

### 83. 你是怎么解决 MySQL 中文乱码问题的？

**考察点**：字符集配置

**参考答案**：
MySQL 8.4 默认字符集已经是 utf8mb4 了，但有些场景还是会出问题。

**问题现象**：插入中文变成问号 ??? 或者乱码。

**排查方向**：
1. 数据库字符集
2. 表字符集
3. 连接字符集
4. 客户端字符集

**我们的解决方案**：

1. **容器启动命令指定字符集**
```yaml
mysql:
  image: mysql:8.4
  command: 
    - --character-set-server=utf8mb4
    - --collation-server=utf8mb4_unicode_ci
```

2. **JDBC 连接字符串**
```
jdbc:mysql://localhost:3306/db?useUnicode=true&characterEncoding=utf8mb4
```

3. **建表时指定**
```sql
CREATE TABLE xxx (
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**检查字符集**：
```sql
SHOW VARIABLES LIKE 'character%';
-- 确保 character_set_server, character_set_database 都是 utf8mb4
```

---

### 84. Nginx 的 try_files 指令是干什么的？为什么 SPA 需要配置它？

**考察点**：Nginx 配置

**参考答案**：
**问题**：SPA（单页应用）用的是前端路由，比如 `/workflow/123`。用户直接访问这个 URL 或者刷新页面，Nginx 会去找 `/workflow/123` 这个文件，找不到就 404。

**try_files** 的作用是：按顺序尝试查找文件，都找不到就用最后一个。

```nginx
location / {
    root /usr/share/nginx/html;
    index index.html;
    try_files $uri $uri/ /index.html;
}
```

解释：
1. 先找 `$uri`（比如 `/workflow/123`）这个文件
2. 再找 `$uri/`（目录）
3. 都找不到，返回 `/index.html`

这样不管访问什么路由，最终都返回 index.html，然后前端 JS 根据 URL 渲染对应的页面。

**完整的 Nginx 配置**：
```nginx
server {
    listen 80;
    
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理
    location /api/ {
        proxy_pass http://console-hub:8080;
    }
}
```

---

### 85. 你说"静态资源传输体积减少约 70%"，是怎么做到的？

**考察点**：Gzip 压缩、缓存策略

**参考答案**：
主要做了两件事：**Gzip 压缩** 和 **浏览器缓存**。

**1. Gzip 压缩**

```nginx
gzip on;
gzip_min_length 1k;           # 小于 1k 的不压缩
gzip_types text/plain text/css application/javascript application/json;
gzip_comp_level 6;            # 压缩级别 1-9
```

效果：JS/CSS 文件从 500KB 压缩到 150KB 左右。

**2. 长期缓存**

```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
    expires 1y;                # 缓存 1 年
    add_header Cache-Control "public, immutable";
}
```

配合前端构建的文件名 hash：
```
main.a1b2c3d4.js  # 内容变了，hash 也变
```

用户第二次访问，这些静态文件直接从浏览器缓存读取，不再请求服务器。

**测量方式**：
用 Chrome DevTools 的 Network 面板，对比优化前后的 transferred 大小。

优化前：首屏加载 2MB
优化后：首屏加载 600KB（Gzip）+ 后续访问 0（缓存）

---

### 86. 容器化部署相比传统部署有什么优势？

**考察点**：容器化理解

**参考答案**：
传统部署是直接在服务器上装软件，容器化是把应用和环境打包成镜像。

| 对比项 | 传统部署 | 容器化部署 |
|--------|----------|------------|
| 环境一致性 | "我本地能跑" | 开发/测试/生产环境一致 |
| 部署速度 | 手动装依赖，可能几小时 | 拉镜像启动，几分钟 |
| 资源隔离 | 多个应用可能冲突 | 容器之间隔离 |
| 扩缩容 | 手动加服务器 | docker-compose scale 或 K8s |
| 回滚 | 复杂，可能要重装 | 换个镜像版本就行 |
| 技术栈 | 服务器上要装各种运行时 | 每个容器自带运行时 |

**我们项目的体会**：

1. **新人上手快**：克隆代码，`docker compose up`，10 分钟跑起来
2. **环境问题少**：不会出现"我本地 JDK 是 17 你是 21"这种问题
3. **部署简单**：CI/CD 构建镜像，推到仓库，服务器拉取运行
4. **多语言友好**：Java、Python、Go 服务各自独立，互不影响

唯一的成本是要学习 Docker，但这已经是必备技能了。

---

## 十、数据库 / 缓存（6 道）

### 87. 工作流数据是存在哪里的？为什么选择 PostgreSQL？

**考察点**：数据库选型

**参考答案**：
工作流数据存在 **PostgreSQL** 里。

**存储内容**：
- 工作流定义（DSL JSON）
- 执行记录
- 节点状态
- 变量快照

**为什么选 PostgreSQL**：

1. **JSON 支持好**：工作流 DSL 是 JSON 格式，PostgreSQL 有 jsonb 类型，可以直接存储和查询
```sql
-- 查询包含某个节点类型的工作流
SELECT * FROM workflow WHERE dsl::jsonb @> '{"nodeType": "LLM"}';
```

2. **性能稳定**：复杂查询性能比 MySQL 好

3. **开源友好**：没有 MySQL 的商业授权问题

4. **向量扩展**：后续做 RAG 可以用 pgvector 扩展
```sql
-- 向量相似度搜索
SELECT * FROM documents ORDER BY embedding <=> query_vector LIMIT 10;
```

**表结构示例**：
```sql
CREATE TABLE workflow (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100),
    dsl JSONB,           -- 工作流定义
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### 88. Redis 在项目中是怎么用的？缓存了哪些数据？

**考察点**：缓存设计

**参考答案**：
Redis 在项目里主要用于**缓存**和**临时数据存储**。

**缓存的数据**：

1. **模型配置**
```java
// 模型配置不常变，缓存起来
String key = "model:config:" + modelId;
ModelConfig config = redis.get(key);
if (config == null) {
    config = db.getById(modelId);
    redis.setex(key, 3600, config);  // 缓存 1 小时
}
```

2. **工作流定义**
```java
String key = "workflow:" + workflowId;
// 执行时先查缓存，避免每次都查数据库
```

3. **会话状态**
```java
// 多轮对话的历史记录
String key = "chat:history:" + sessionId;
redis.lpush(key, message);
redis.expire(key, 3600);  // 1 小时过期
```

4. **分布式锁**
```java
// 防止同一个工作流并发执行
String lockKey = "workflow:lock:" + workflowId;
boolean locked = redis.setnx(lockKey, "1", 60);
```

**使用的数据结构**：
- String：简单缓存
- List：对话历史
- Hash：结构化数据
- Set：去重场景

---

### 89. MinIO 是什么？你用它来存什么？

**考察点**：对象存储

**参考答案**：
MinIO 是一个开源的对象存储服务，兼容 AWS S3 协议。可以理解为"自己搭的 S3"。

**存储的内容**：

1. **语音合成的音频文件**
```java
// 生成的 MP3 文件上传到 MinIO
String objectName = "audio/" + workflowId + "/" + timestamp + ".mp3";
minioClient.putObject(
    PutObjectArgs.builder()
        .bucket("paiflow")
        .object(objectName)
        .stream(audioStream, audioStream.available(), -1)
        .contentType("audio/mpeg")
        .build()
);

// 返回可访问的 URL
String url = minioClient.getPresignedObjectUrl(...);
```

2. **用户上传的文件**
- 知识库文档
- 图片素材

**为什么用 MinIO**：

1. **私有化部署**：数据存在自己服务器上，不用担心云存储费用
2. **S3 兼容**：代码不用改，换成 AWS S3 也能跑
3. **简单**：Docker 一行命令就能跑起来

```yaml
minio:
  image: minio/minio
  command: server /data --console-address ":9001"
  ports:
    - "9000:9000"   # API
    - "9001:9001"   # Console
```

---

### 90. 如果工作流执行记录数据量很大，怎么优化查询性能？

**考察点**：数据库优化

**参考答案**：
执行记录是典型的时序数据，会持续增长。优化方案：

**1. 索引优化**
```sql
-- 常用查询条件建索引
CREATE INDEX idx_workflow_id ON execution_log(workflow_id);
CREATE INDEX idx_created_at ON execution_log(created_at);
-- 联合索引，覆盖常用查询
CREATE INDEX idx_workflow_time ON execution_log(workflow_id, created_at DESC);
```

**2. 分页查询**
```sql
-- 不要 SELECT *，只查需要的字段
SELECT id, status, created_at FROM execution_log
WHERE workflow_id = ?
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

**3. 分表（按时间）**
```sql
-- 每月一张表
execution_log_202601
execution_log_202602
```

**4. 冷热分离**
- 热数据（最近 7 天）放主库
- 冷数据归档到历史库或者压缩存储

**5. 读写分离**
- 主库写入
- 从库查询

**6. 定期清理**
```sql
-- 删除 30 天前的详细日志，只保留摘要
DELETE FROM execution_log WHERE created_at < NOW() - INTERVAL 30 DAY;
```

---

### 91. 你是怎么做数据库连接池配置的？

**考察点**：连接池

**参考答案**：
用的是 **HikariCP**，Spring Boot 默认的连接池。

**配置**：
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5           # 最小空闲连接
      maximum-pool-size: 20     # 最大连接数
      idle-timeout: 300000      # 空闲连接超时（5分钟）
      max-lifetime: 1800000     # 连接最大存活时间（30分钟）
      connection-timeout: 30000 # 获取连接超时
```

**参数设置原则**：

1. **maximum-pool-size**：根据数据库支持的最大连接数和服务实例数计算
   ```
   最大连接数 = 数据库最大连接数 / 服务实例数 / 安全系数
   比如：200 / 3 / 1.5 ≈ 40
   ```

2. **minimum-idle**：通常设为 maximum-pool-size 的 1/4

3. **connection-timeout**：不要太长，避免请求堆积

4. **max-lifetime**：要小于数据库的 wait_timeout

**监控**：
```java
HikariDataSource ds = (HikariDataSource) dataSource;
HikariPoolMXBean poolMXBean = ds.getHikariPoolMXBean();
log.info("活跃连接: {}, 空闲连接: {}, 等待线程: {}",
    poolMXBean.getActiveConnections(),
    poolMXBean.getIdleConnections(),
    poolMXBean.getThreadsAwaitingConnection());
```

---

### 92. 分布式 ID 你是怎么生成的？用的什么算法？

**考察点**：ID 生成策略

**参考答案**：
我们用的是**雪花算法（Snowflake）**的变体。

**为什么需要分布式 ID**：
- 多个服务实例同时写数据库，自增 ID 会冲突
- ID 要全局唯一

**雪花算法原理**：
```
64位 ID = 1位符号 + 41位时间戳 + 10位机器ID + 12位序列号

0 | 00000000000000000000000000000000000000000 | 0000000000 | 000000000000
```

- 时间戳：毫秒级，可用 69 年
- 机器 ID：支持 1024 个节点
- 序列号：每毫秒 4096 个 ID

**代码实现**：
```java
public class IdGenerator {
    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - EPOCH) << 22) | (workerId << 12) | sequence;
    }
}
```

**使用**：
```java
String workflowId = "wf_" + idGenerator.nextId();  // wf_1234567890123456789
```

雪花 ID 的好处是**趋势递增**，对数据库索引友好。

---

## 十一、AI Agent / RAG 概念（8 道）

### 93. 什么是 AI Agent？和普通的 LLM 调用有什么区别？

**考察点**：Agent 概念

**参考答案**：
普通的 LLM 调用就是"一问一答"：你给它一个问题，它给你一个回答，完事。

AI Agent 是让大模型能够"自主行动"：它不只是回答问题，还能**规划任务、使用工具、循环思考**，直到完成目标。

**核心区别**：

| 特性 | 普通 LLM 调用 | AI Agent |
|------|--------------|----------|
| 交互方式 | 单轮问答 | 多轮自主循环 |
| 工具使用 | 不能 | 可以调用 API、搜索、执行代码 |
| 规划能力 | 没有 | 能分解任务、制定计划 |
| 记忆 | 只有当前上下文 | 可以有长期记忆 |

**举个例子**：

普通 LLM：
```
用户：今天北京天气怎么样？
LLM：抱歉，我无法获取实时天气信息。
```

AI Agent：
```
用户：今天北京天气怎么样？
Agent：（思考）我需要查天气 → （调用天气 API）→ （得到结果）
Agent：今天北京晴，气温 25°C，适合户外活动。
```

我们的 PaiFlow 本质上就是一个"可配置的 Agent"，用户通过编排节点来定义 Agent 的行为逻辑。

---

### 94. ReAct 范式是什么？你在 PluginNode 中是怎么实现的？

**考察点**：ReAct 模式

**参考答案**：
ReAct 是 "Reasoning + Acting" 的缩写，是一种让大模型能够"边思考边行动"的范式。

**核心循环**：
```
Thought（思考）→ Action（行动）→ Observation（观察）→ Thought（继续思考）→ ...
```

**举例**：
```
问题：帮我查一下特斯拉的股价，然后算一下买100股要多少钱

Thought: 我需要先查特斯拉的当前股价
Action: 调用股票API查询 TSLA
Observation: 特斯拉当前股价 $250

Thought: 股价是250美元，100股就是250×100
Action: 计算 250 * 100
Observation: 25000

Thought: 我已经得到了答案
Final Answer: 买100股特斯拉需要25000美元
```

**在 PluginNode 中的实现**：

```java
public class PluginNodeExecutor extends AbstractNodeExecutor {
    @Override
    protected NodeRunResult executeNode(NodeState state, Map<String, Object> inputs) {
        // 1. 把工具描述和用户问题发给大模型
        String toolDescription = getToolDescription(pluginId);
        String prompt = buildReActPrompt(toolDescription, userQuestion);
        
        // 2. 大模型决定调用哪个工具、传什么参数
        LlmResponse response = llm.call(prompt);
        ToolCall toolCall = parseToolCall(response);
        
        // 3. 执行工具调用
        Object result = linkClient.execute(toolCall.toolId, toolCall.params);
        
        // 4. 把结果返回给大模型，生成最终答案
        String finalAnswer = llm.call(buildObservationPrompt(result));
        
        return NodeRunResult.success(finalAnswer);
    }
}
```

---

### 95. 什么是 RAG（检索增强生成）？为什么需要 RAG？

**考察点**：RAG 理解

**参考答案**：
RAG = Retrieval Augmented Generation，检索增强生成。

**核心思路**：大模型的知识是训练时固定的，可能过时或者不全。RAG 就是在调用大模型前，先从知识库里检索相关内容，塞到 Prompt 里，让大模型"看着资料回答"。

**为什么需要 RAG**：

1. **知识更新**：大模型不知道最新的信息，但知识库可以随时更新
2. **私有数据**：公司内部文档、产品手册，大模型没见过
3. **减少幻觉**：有了参考资料，大模型不容易编造
4. **可解释**：可以告诉用户"这个答案来自 XX 文档"

**RAG 流程**：
```
用户问题
    ↓
向量化（Embedding）
    ↓
在向量数据库中检索相似文档
    ↓
把文档内容 + 用户问题 组装成 Prompt
    ↓
调用大模型
    ↓
返回答案
```

**Prompt 模板**：
```
根据以下文档回答用户问题，如果文档中没有相关信息，请回答"我不知道"。

参考文档：
{{检索到的文档内容}}

用户问题：{{用户问题}}
```

---

### 96. Function Calling / Tool Use 是什么？大模型怎么知道要调用哪个工具？

**考察点**：工具调用

**参考答案**：
Function Calling 是让大模型能够调用外部函数/API 的能力。

**工作原理**：

1. **告诉大模型有哪些工具**：
```json
{
    "name": "get_weather",
    "description": "获取指定城市的天气",
    "parameters": {
        "type": "object",
        "properties": {
            "city": {"type": "string", "description": "城市名"}
        }
    }
}
```

2. **大模型决定是否调用**：
```
用户：今天北京天气怎么样？
大模型：（分析）这个问题需要实时数据，我应该调用 get_weather
返回：{"function_call": {"name": "get_weather", "arguments": {"city": "北京"}}}
```

3. **执行调用，返回结果**：
```
调用结果：{"temperature": 25, "weather": "晴"}
```

4. **大模型组织答案**：
```
大模型：今天北京天气晴朗，气温 25°C。
```

**大模型怎么知道调用哪个工具**：

靠的是 Prompt 里的工具描述。大模型学过大量的代码和 API 文档，它能理解"根据用户意图匹配最合适的工具"。

关键是**工具描述要写清楚**：
- 名称要有意义（get_weather 而不是 func1）
- description 要说明白什么时候用
- 参数要有清晰的说明

---

### 97. MCP（Model Context Protocol）是什么？你是怎么实现 MCP Client 的？

**考察点**：MCP 协议

**参考答案**：
MCP 是 Anthropic 提出的"模型上下文协议"，目的是**标准化大模型和外部工具的交互方式**。

**为什么需要 MCP**：
以前每个工具都要单独对接，写一堆适配代码。MCP 定义了统一的协议，工具实现 MCP Server，应用实现 MCP Client，就能互联互通。

**MCP 的核心概念**：
- **Resources**：只读数据源（文件、数据库）
- **Tools**：可执行的功能（API 调用、代码执行）
- **Prompts**：预定义的提示词模板

**通信方式**：
- **stdio**：通过标准输入输出通信，适合本地工具
- **SSE**：通过 HTTP SSE 通信，适合远程服务

**我们的 MCP Client 实现思路**：

```java
public class McpClient {
    private final String transport;  // "stdio" or "sse"
    
    // 发现可用工具
    public List<Tool> listTools() {
        return sendRequest("tools/list");
    }
    
    // 调用工具
    public Object callTool(String name, Map<String, Object> args) {
        return sendRequest("tools/call", Map.of("name", name, "arguments", args));
    }
    
    private Object sendRequest(String method, Object params) {
        if ("stdio".equals(transport)) {
            // 写入子进程的 stdin，从 stdout 读取结果
        } else {
            // 发送 HTTP 请求
        }
    }
}
```

通过配置化方式接入 MCP 工具：
```yaml
mcp_servers:
  - name: "filesystem"
    transport: "stdio"
    command: ["python", "filesystem_server.py"]
  - name: "search"
    transport: "sse"
    url: "http://search-mcp:8080"
```

---

### 98. 什么是 Agent 的"规划"能力？你的工作流引擎支持吗？

**考察点**：Agent 规划

**参考答案**：
规划能力是指：Agent 能把一个复杂任务分解成多个子任务，然后按顺序或并行执行。

**举例**：
```
任务：帮我写一篇关于 AI 的文章并发到公众号

Agent 规划：
1. 搜索 AI 最新动态
2. 整理素材，形成大纲
3. 撰写文章正文
4. 生成配图
5. 调用公众号 API 发布
```

**我们工作流引擎的规划能力**：

我们的工作流引擎是**静态规划**，用户在设计时就把流程定好了：
```
Start → LLM(搜索) → LLM(写作) → Plugin(配图) → Plugin(发布) → End
```

和"动态规划"的区别：
- **静态规划**（我们）：流程预先定义，运行时按图执行
- **动态规划**（真正的 Agent）：运行时根据情况自己决定下一步

如果要做动态规划，需要：
1. 一个"规划 LLM"负责分解任务
2. 一个"执行 LLM"负责执行每个子任务
3. 支持循环和条件判断

这是后续可以扩展的方向，可以参考 LangGraph4J 的 Agent 实现。

---

### 99. 多 Agent 协作是什么意思？你了解过哪些多 Agent 框架？

**考察点**：Multi-Agent

**参考答案**：
多 Agent 协作是让多个 Agent 一起完成任务，每个 Agent 有自己的角色和专长。

**协作模式**：

1. **串行协作**：A 做完交给 B，B 做完交给 C
   ```
   研究员 Agent → 写作 Agent → 审核 Agent
   ```

2. **并行协作**：多个 Agent 同时工作，最后汇总
   ```
   ┌→ 数据 Agent ─┐
   │              │
   → 文案 Agent ─→ 汇总 Agent
   │              │
   └→ 设计 Agent ─┘
   ```

3. **辩论式**：多个 Agent 对同一问题给出观点，最后综合
   ```
   正方 Agent ←→ 反方 Agent → 裁判 Agent
   ```

**常见的多 Agent 框架**：

| 框架 | 特点 |
|------|------|
| AutoGen | 微软出品，支持对话式协作 |
| CrewAI | 定义角色、任务、流程 |
| LangGraph | 基于图的编排，灵活度高 |
| MetaGPT | 模拟软件公司，有产品经理、工程师等角色 |

**我们的工作流其实就是简化版的多 Agent**：每个节点可以看作一个"单任务 Agent"，通过边连接起来协作。

---

### 100. 如果让你设计一个支持自主决策的 AI Agent，你会怎么做？

**考察点**：Agent 架构设计

**参考答案**：
设计一个能自主决策的 Agent，核心要解决几个问题：

**1. 架构设计**

```
用户输入
    ↓
┌─────────────────────────────┐
│         Agent Core          │
│  ┌─────────────────────┐    │
│  │    Planning Module   │   │  ← 规划：分解任务
│  └─────────────────────┘    │
│            ↓                │
│  ┌─────────────────────┐    │
│  │   Reasoning Module   │   │  ← 推理：决定下一步
│  └─────────────────────┘    │
│            ↓                │
│  ┌─────────────────────┐    │
│  │    Action Module     │   │  ← 执行：调用工具
│  └─────────────────────┘    │
│            ↓                │
│  ┌─────────────────────┐    │
│  │    Memory Module     │   │  ← 记忆：保存上下文
│  └─────────────────────┘    │
└─────────────────────────────┘
    ↓
输出结果
```

**2. 核心组件**

- **Planning**：把用户目标分解成可执行的步骤
- **Reasoning**：根据当前状态决定下一步（ReAct 范式）
- **Tools**：可调用的工具集合
- **Memory**：短期记忆（当前对话）+ 长期记忆（历史知识）

**3. 决策循环**

```python
def agent_loop(goal):
    plan = planning_llm.plan(goal)
    memory = Memory()
    
    while not is_goal_achieved(memory):
        # 1. 思考：分析当前状态，决定下一步
        thought = reasoning_llm.think(memory.get_context())
        
        # 2. 行动：执行动作（调用工具或回答）
        if thought.needs_tool:
            result = tools.execute(thought.action)
            memory.add_observation(result)
        else:
            return thought.final_answer
        
        # 3. 检查：是否需要调整计划
        if should_replan(memory):
            plan = planning_llm.replan(plan, memory)
    
    return summarize(memory)
```

**4. 关键挑战**

- **循环控制**：防止无限循环，设置最大步数
- **错误恢复**：工具调用失败后的处理
- **成本控制**：每次决策都要调用 LLM，成本高

如果用我们的 WorkflowEngine 来实现，需要加上"动态节点创建"和"循环边"的支持。

---

### 100.1 什么是 Claude Skills？和传统的 Function Calling / MCP 有什么区别？

**考察点**：前沿技术、Agent 能力扩展机制

**参考答案**：
Claude Skills 是 Anthropic 推出的一种**结构化知识包**机制，用于增强 Claude 在特定任务上的能力。它不是 API 调用，而是把专业知识、指令、脚本打包成一个文件夹，让 Claude 按需加载。

**核心概念**：
- Skills 是一个**文件夹**，包含 `SKILL.md` 文件（必需）和可选的资源文件
- `SKILL.md` 包含 YAML 元数据（name、description）和 Markdown 指令
- Claude 根据任务需要，动态加载相关 Skill 的内容

**三者对比**：

| 对比项 | Function Calling | MCP | Claude Skills |
|--------|------------------|-----|---------------|
| 本质 | API 调用 | 工具协议 | 知识包 |
| 格式 | JSON Schema | 复杂协议 | Markdown + YAML |
| Token 消耗 | 低（只传参数） | 高（协议开销） | 按需加载，高效 |
| 适用场景 | 调用外部 API | 复杂工具集成 | 增强特定任务能力 |
| 学习成本 | 中等 | 高 | 低 |

**举个例子**：
假设要让 Claude 更擅长处理 PDF，用 Skills 的方式：

```
pdf-skill/
├── SKILL.md          # 定义如何处理 PDF 的指令
├── examples/         # 示例用法
└── scripts/          # 辅助脚本
```

而不是写一个 `parsePdf()` 的 Function。Skills 更像是"教会 Claude 一项技能"，而不是"给 Claude 一个工具"。

---

### 100.2 Claude Skills 的目录结构和 SKILL.md 文件格式是怎样的？

**考察点**：实际使用 Skills 的能力

**参考答案**：
一个标准的 Skill 目录结构：

```
my-skill/
├── SKILL.md              # 必需，技能定义文件
├── examples/             # 可选，使用示例
│   ├── example1.md
│   └── example2.md
├── scripts/              # 可选，辅助脚本
│   └── helper.py
└── resources/            # 可选，其他资源
    └── template.json
```

**SKILL.md 文件格式**：

```markdown
---
name: pdf-processor
description: 帮助 Claude 更好地处理 PDF 文件，包括提取表单字段、填写 PDF、转换格式等
---

# PDF 处理技能

## 能力说明
这个技能让你能够：
1. 提取 PDF 中的表单字段
2. 填写 PDF 表单
3. 将 PDF 转换为其他格式

## 使用指南

### 提取表单字段
当用户要求提取 PDF 表单时，使用以下步骤：
1. 首先分析 PDF 结构
2. 识别所有表单字段
3. 输出字段列表，包含字段名、类型、当前值

### 填写表单
...

## 示例
参见 examples/ 目录中的具体用例

## 注意事项
- 处理大型 PDF 时要注意内存
- 某些加密 PDF 可能无法处理
```

**关键点**：
- YAML frontmatter 中 `name` 和 `description` 是**必填项**
- description 要写清楚"这个技能做什么、什么时候用"
- Markdown 内容是给 Claude 看的"操作手册"

---

### 100.3 Skills 的"渐进式披露"(Progressive Disclosure) 原理是什么？为什么这样设计？

**考察点**：理解 Skills 的核心设计理念

**参考答案**：
渐进式披露是 Skills 最核心的设计思想，解决的是**上下文窗口有限**的问题。

**问题背景**：
传统做法是把所有工具说明、使用指南都塞进 System Prompt，导致：
- Token 消耗巨大（可能几万 Token 的说明）
- 无关信息干扰模型判断
- 上下文空间被挤占

**渐进式披露的三层加载**：

```
第一层：只加载元数据（几十 Token）
┌────────────────────────────────────┐
│ name: pdf-processor                 │
│ description: 处理 PDF 文件的技能    │
└────────────────────────────────────┘
         ↓ 用户任务匹配时
第二层：加载主文档（几百 Token）
┌────────────────────────────────────┐
│ # PDF 处理技能                      │
│ ## 能力说明                         │
│ ## 使用指南                         │
└────────────────────────────────────┘
         ↓ 需要具体示例时
第三层：加载附加文件（按需）
┌────────────────────────────────────┐
│ examples/extract-form.md           │
│ scripts/pdf-helper.py              │
└────────────────────────────────────┘
```

**类比**：
就像一本技术手册：
- 目录页（元数据）：让你快速知道有哪些内容
- 章节概述（主文档）：告诉你怎么用
- 详细示例（附加文件）：只在需要时翻阅

**好处**：
1. **Token 高效**：不需要时不加载
2. **响应更准确**：没有无关信息干扰
3. **扩展性好**：可以有几百个 Skills，不会撑爆上下文

---

### 100.4 Claude Skills 与 MCP 的关系是什么？什么场景用 Skills，什么场景用 MCP？

**考察点**：技术选型能力

**参考答案**：
Skills 和 MCP 是**互补而非替代**的关系，解决的是不同层面的问题。

**本质区别**：

| 维度 | Claude Skills | MCP (Model Context Protocol) |
|------|---------------|------------------------------|
| 定位 | 知识增强 | 工具调用协议 |
| 形式 | Markdown 文件 | 标准化协议 + Server 实现 |
| 执行 | Claude 自己执行（借助代码环境） | 外部 Server 执行 |
| 复杂度 | 简单，只需写 SKILL.md | 复杂，需要实现 MCP Server |
| Token 开销 | 低（渐进式加载） | 高（协议开销） |

**场景选择**：

**用 Skills 的场景**：
```
✓ 增强 Claude 在特定领域的专业能力
✓ 提供操作指南、最佳实践
✓ 简单的脚本执行（Claude 自己跑代码）
✓ 不需要外部系统交互的任务

例子：
- Excel 数据分析技能
- 代码审查技能
- 文档写作风格技能
```

**用 MCP 的场景**：
```
✓ 需要调用外部 API
✓ 需要访问数据库、文件系统
✓ 需要和其他系统集成
✓ 需要实时数据

例子：
- 查询数据库
- 调用天气 API
- 操作 Git 仓库
- 发送消息到 Slack
```

**组合使用**：
```
用户：帮我分析这个 Excel 文件，然后把结果发到 Slack

Skills 负责：Excel 分析的专业知识和方法
MCP 负责：读取文件、发送 Slack 消息
```

---

### 100.5 如果在 PaiFlow 中实现类似 Claude Skills 的机制，你会怎么设计？

**考察点**：架构设计能力、技术迁移

**参考答案**：
可以在 PaiFlow 中设计一个"技能包"机制，让用户能定义和复用专业知识。

**设计思路**：

```
skills/
├── podcast-writing/           # 播客脚本写作技能
│   ├── SKILL.md
│   └── examples/
├── data-analysis/             # 数据分析技能
│   ├── SKILL.md
│   └── templates/
└── code-review/               # 代码审查技能
    └── SKILL.md
```

**核心组件**：

```java
// 1. 技能注册中心
@Component
public class SkillRegistry {
    private Map<String, SkillMetadata> skills = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void loadSkills() {
        // 扫描 skills/ 目录，加载所有 SKILL.md 的元数据
        Files.walk(skillsPath)
            .filter(p -> p.endsWith("SKILL.md"))
            .forEach(this::parseAndRegister);
    }
    
    public List<SkillMetadata> matchSkills(String userQuery) {
        // 根据用户查询匹配相关技能
        return skills.values().stream()
            .filter(s -> isRelevant(s, userQuery))
            .collect(toList());
    }
}

// 2. 技能加载器（渐进式）
public class SkillLoader {
    
    // 第一层：只返回元数据
    public String getMetadataPrompt(List<SkillMetadata> skills) {
        return skills.stream()
            .map(s -> s.getName() + ": " + s.getDescription())
            .collect(joining("\n"));
    }
    
    // 第二层：加载完整 SKILL.md
    public String loadFullSkill(String skillName) {
        return Files.readString(skillsPath.resolve(skillName + "/SKILL.md"));
    }
    
    // 第三层：加载附加资源
    public String loadResource(String skillName, String resourcePath) {
        return Files.readString(skillsPath.resolve(skillName + "/" + resourcePath));
    }
}
```

**在 LLM 节点中集成**：

```java
public class LLMNodeExecutor {
    
    @Override
    protected NodeRunResult executeNode(NodeState state, Map<String, Object> inputs) {
        String userPrompt = inputs.get("prompt");
        
        // 1. 匹配相关技能
        List<SkillMetadata> relevantSkills = skillRegistry.matchSkills(userPrompt);
        
        // 2. 构建增强后的 System Prompt
        String systemPrompt = buildSystemPrompt(
            node.getSystemPrompt(),
            skillLoader.getMetadataPrompt(relevantSkills)  // 只加载元数据
        );
        
        // 3. 调用 LLM
        ChatResponse response = chatModel.call(new Prompt(userPrompt, systemPrompt));
        
        // 4. 如果 LLM 请求更多技能信息，动态加载
        if (response.needsSkillDetail()) {
            String fullSkill = skillLoader.loadFullSkill(response.getRequestedSkill());
            // 再次调用，带上完整技能内容
        }
    }
}
```

**好处**：
- 专业知识可复用、可共享
- 不会撑爆上下文（渐进式加载）
- 用户可以自定义技能包

---

## 十二、LangGraph4J（10 道）

### 101. 什么是 LangGraph4J？它和 LangChain4j 有什么区别？

**考察点**：框架理解、技术选型

**参考答案**：
LangGraph4J 是 LangGraph 的 Java 版本，专门用来构建有状态的、多步骤的 AI 应用。简单来说，LangChain4j 更像是一个"工具箱"，提供了调用大模型、做 RAG 的各种工具；而 LangGraph4J 更像是一个"流程编排器"，它让你可以把多个 AI 步骤串成一个图（Graph），支持循环、条件分支、状态管理这些复杂逻辑。

打个比方，如果 LangChain4j 是乐高积木，那 LangGraph4J 就是告诉你怎么把积木拼成一个能动的机器人。在我们 PaiFlow 项目里，工作流引擎的设计思路其实和 LangGraph4J 很像，都是基于图的节点编排。

---

### 102. LangGraph4J 中的 StateGraph 是什么？状态是怎么在节点之间传递的？

**考察点**：核心概念理解

**参考答案**：
StateGraph 是 LangGraph4J 的核心类，它定义了一个有状态的图结构。每个节点执行完后会产生一些输出，这些输出会合并到一个全局的 State 对象里，然后传给下一个节点。

这个设计和我们 PaiFlow 的 VariablePool 很像。我们的做法是：每个节点执行完把结果存到 VariablePool 里，下一个节点从 VariablePool 取它需要的输入。LangGraph4J 用的是 Reducer 函数来决定怎么合并状态，比如是覆盖还是追加。

---

### 103. LangGraph4J 是怎么实现条件分支的？conditional_edges 是什么意思？

**考察点**：流程控制

**参考答案**：
LangGraph4J 用 conditional_edges 来实现"根据条件走不同分支"的逻辑。你定义一个函数，这个函数根据当前状态返回下一个要执行的节点名称。

比如做一个客服机器人，用户问的如果是退款问题就走退款流程，如果是咨询问题就走咨询流程。在我们 PaiFlow 里，这个对应的是 Edge 上的 sourceHandle 字段，我们通过判断是 `normal_one_of` 还是 `intent_chain` 来决定走正常分支还是异常分支。

---

### 104. LangGraph4J 怎么实现循环？比如让 Agent 反复思考直到得出满意答案。

**考察点**：循环控制、ReAct 模式

**参考答案**：
LangGraph4J 天然支持图中的环，你只要在 conditional_edges 里让某个条件指回之前的节点就行了。典型的应用是 ReAct 模式：Agent 先思考（Reason），再行动（Act），然后观察结果，如果不满意就继续思考，形成一个循环。

退出循环的条件通常是：达到最大迭代次数，或者 Agent 判断任务已完成。我们 PaiFlow 目前的实现是单次执行，不支持循环，但如果要加这个功能，思路就是在 executeNode 里判断节点状态，允许 RUNNING 状态的节点重新执行。

---

### 105. LangGraph4J 的 Checkpointer 是干什么的？怎么实现工作流的断点续传？

**考察点**：状态持久化

**参考答案**：
Checkpointer 是 LangGraph4J 的状态持久化组件，它会在每个节点执行完后把当前状态保存下来。这样如果中途出错或者需要人工介入，下次可以从保存的状态继续执行，不用从头开始。

LangGraph4J 提供了 MemorySaver（内存存储）和 SqliteSaver（数据库存储）两种实现。在我们 PaiFlow 里，我们是通过 EngineContextHolder 保存执行上下文，节点状态存在 Node 对象的 status 字段里。如果要做断点续传，需要把整个 WorkflowDSL 和 VariablePool 序列化存到数据库。

---

### 106. 什么是 Human-in-the-Loop？LangGraph4J 是怎么支持人工介入的？

**考察点**：人机协作

**参考答案**：
Human-in-the-Loop 就是"让人插一脚"，在 AI 执行过程中加入人工审核或决策的环节。比如 Agent 要发一封重要邮件，发之前先让人确认一下。

LangGraph4J 通过 interrupt_before 或 interrupt_after 参数来实现，指定在某个节点前后暂停，等人工确认后再继续。结合 Checkpointer，它能保存暂停时的状态，人工审批通过后从断点继续。

我们 PaiFlow 里有类似的设计，就是 NodeRunResult 里的 INTERRUPT 状态，当节点返回这个状态时，工作流会暂停并发送中断事件给前端。

---

### 107. LangGraph4J 和你们自研的 WorkflowEngine 有什么异同？

**考察点**：架构对比、设计思考

**参考答案**：
**相同点**：
1. 都是基于图（Graph）的节点编排，用边（Edge）表示节点间的关系
2. 都支持条件分支，根据执行结果走不同路径
3. 都有状态管理机制，我们用 VariablePool，它用 State

**不同点**：
1. LangGraph4J 原生支持循环，我们目前只支持 DAG（无环图）
2. LangGraph4J 有内置的 Checkpointer 做断点续传，我们需要自己实现
3. LangGraph4J 和 LangChain4j 生态集成更好，我们和 Spring AI 集成更深
4. 我们的优势是 SSE 流式输出做得更完善，有双队列保证消息有序

如果重新设计，我可能会考虑借鉴 LangGraph4J 的 Checkpointer 设计来完善断点续传功能。

---

### 108. 如果用 LangGraph4J 实现一个 ReAct Agent，代码大概是什么样的？

**考察点**：实际应用、代码能力

**参考答案**：
大概的思路是这样：

```java
// 1. 定义状态类
public class AgentState {
    List<Message> messages;
    String nextAction;
}

// 2. 创建图
StateGraph<AgentState> graph = new StateGraph<>(AgentState.class);

// 3. 添加节点
graph.addNode("reason", this::reasonStep);    // 思考：分析问题，决定下一步
graph.addNode("act", this::actStep);          // 行动：调用工具
graph.addNode("observe", this::observeStep);  // 观察：处理工具返回结果

// 4. 添加边和条件分支
graph.addEdge("reason", "act");
graph.addConditionalEdges("act", this::shouldContinue, 
    Map.of("continue", "observe", "end", END));
graph.addEdge("observe", "reason");  // 形成循环

// 5. 编译执行
CompiledGraph<AgentState> app = graph.compile();
```

核心就是 reason → act → observe → reason 这个循环，直到 shouldContinue 返回 "end"。

---

### 109. LangGraph4J 的 Subgraph 是什么？什么场景下需要用到子图？

**考察点**：模块化设计

**参考答案**：
Subgraph 就是"图中图"，把一组相关的节点封装成一个子图，然后在主图里当作一个节点来用。这样做的好处是模块化，让复杂的工作流更容易维护。

典型场景比如：
1. **多 Agent 协作**：每个 Agent 是一个子图，主图负责协调
2. **复用逻辑**：比如"内容审核"是一个通用流程，封装成子图后可以在多个地方复用
3. **团队协作**：不同团队负责不同子图，最后组装到一起

在 PaiFlow 里我们目前没有子图的概念，但如果要实现，可以让一个节点的 NodeExecutor 内部再跑一个完整的 WorkflowEngine。

---

### 110. 如果让你把 PaiFlow 迁移到 LangGraph4J，你会怎么做？有哪些挑战？

**考察点**：架构演进、技术判断

**参考答案**：
**迁移思路**：
1. 把我们的 Node 映射成 LangGraph4J 的节点函数
2. 把 VariablePool 改造成符合 LangGraph4J State 规范的状态类
3. 把 Edge 的解析逻辑改成 LangGraph4J 的 addEdge/addConditionalEdges
4. 用 LangGraph4J 的 Checkpointer 替换我们自己的状态管理

**主要挑战**：
1. **SSE 流式输出**：LangGraph4J 的流式支持可能没有我们做得细，需要适配
2. **Spring AI 集成**：我们深度用了 Spring AI，迁移后要处理好和 LangChain4j 的关系
3. **现有 DSL 兼容**：前端保存的工作流 JSON 格式要能平滑迁移
4. **学习成本**：团队需要熟悉 LangGraph4J 的 API 和设计理念

我的判断是，如果业务上确实需要循环、人工介入这些高级特性，迁移是值得的；如果当前的 DAG 编排已经够用，自研的方案更可控。

---

## 面试技巧提示

1. **回答项目问题时**：先说业务背景和价值，再说技术实现，最后说效果和数据
2. **遇到不会的问题**：诚实说不太了解，但可以说说自己的思考方向
3. **技术深度问题**：结合源码和实际踩坑经验来回答，更有说服力
4. **数据和指标**：尽量用具体数字来量化你的贡献，但要确保数据真实可解释

---

*面试题整理于 2026-01-15*
