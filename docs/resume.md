## **PaiFlow** Java 后端开发 2025 年 12 月-2026 年 1 月
项目介绍：**派派工作流是一个企业级 AI Agent 工作流编排平台，支持用户通过可视化方式编排大模型节点、插件与逻辑控制流**。项目采用微服务架构，集成了 LLM、超拟人音频合成等工具，提供从工作流设计、调试到发布的完整生命周期管理。

技术栈（Java 版工作流这样写）：Java 21, Spring Boot, MySQL 8.4, Redis, MinIO, Docker Compose, MyBatis-Plus, SSE，SpringAI，LangChain4j

技术栈（Python 版工作流这样写）：Python 3.11, FastAPI, Uvicorn, SQLAlchemy, LangChain, Kafka, OTLP

核心职责：

+ 编写并优化 Docker Compose 编排脚本 ，统一管理 MySQL、Redis、MinIO、Console Hub 及 Workflow Engine 等 5+ 个核心服务的依赖关系与健康检查，实现了“一键拉起”开发环境，将本地环境搭建时间从小时级降低至 10 分钟以内。
+ 基于 OkHttp3 封装了统一的 OkHttpUtil 组件，针对大模型的延迟特性定制 ConnectionPool 连接池参数，高效复用 TCP 连接，将外部 API 调用的握手耗时降低了 60% 。
+ 自定义 EventSourceListener，用于处理大模型输出的断流重连与心跳检测，保障在弱网环境下流式对话的连续性。
+ 在 Link 客户端中实现了动态 URL 构建与 Header 注入机制，支持通过 HTTP 协议调用远程 OpenAPI 工具；结合 OkHttp 的拦截器机制 ，统一处理鉴权签名、请求日志记录及超时重试逻辑，为上层业务提供 RPC 能力。
+ 通过线程池、CompletableFuture 和 Spring 的 @Async 注解实现异步处理，通过SseEmitter实现SSE流式响应，结合EventSource和回调机制处理流式数据。
+ **基于 Spring 的事件驱动模型与责任链模式，通过 NodeExecutor 接口的多态实现不同类型节点的统一调度；通过构建执行链路（边驱动的顺序执行）完成从输入→LLM 节点→超拟人合成节点→ 输出节点的流程推进**。
+ 通过遍历边将目标节点加入源节点的 next/fail 列表，形成 DAG 的邻接关系。
+ 针对 MySQL 8.4 新特性，通过定制容器启动命令 (--character-set-server=utf8mb4)，彻底解决了字符集不一致导致的中文乱码问题。
+ 使用 Nginx + Docker 构建轻量级前后端运行环境，实现 React 单页应用 (SPA) 的生产级托管；并通过配置 try_files 指令彻底解决 SPA 路由模式下的刷新 404 问题，并启用 Gzip 智能压缩与长期缓存策略 (Expires 1y) ，将静态资源传输体积减少约 70% ，首屏加载速度控制在 200 毫秒内。
+ 利用 Reactor 的 Flux 响应式编程模型，对接大模型的 SSE 流式接口；通过 Flux.subscribe() 实现了非阻塞的数据消费，在单机并发场景下，将 LLM 响应的吞吐量提升了 200%。
+ **深度集成 Spring AI 框架，通过 OpenAiChatModel 与 OpenAiApi 标准接口，实现对 OpenAI、DeepSeek 的统一抽象与调用；支持业务方通过配置快速切换底层基座模型，模型接入效率得到大幅提升**。
+ 设计了 LlmChatHistory 组件，支持动态组装 SystemMessage 、 UserMessage 与 AssistantMessage ；并基于 Token 滑动窗口实现了历史记录截断策略，在有限的 Context Window 内最大化保留对话上下文，有效解决长对话场景下的 Token 溢出问题。
+ 自研工作流编排引擎 WorkflowEngine，支持基于 DSL (JSON) 的动态流程定义；并支持节点状态的持久化与恢复机制，以及复杂业务流程的断点续传，核心调度延时低至 10ms 级 。
+ **在 WorkflowEngine 中实现了 DAG (有向无环图) 的拓扑排序与遍历算法，通过 ConcurrentLinkedQueue 管理就绪节点；支持复杂的并行分支、汇聚及条件判断逻辑， 确保 50+ 节点规模的工作流在多线程环境下的执行顺序与数据一致性**。
+ 设计并实现了基于 FastAPI + Uvicorn 的高性能微服务底座，解耦 Workflow 引擎、Link 连接器与 AI Tools 三大核心模块；通过统一的服务工厂模式 (Service Factory) 与依赖注入机制 ，标准化 Kafka、Redis、MinIO 等基础设施的调用方式， 将新服务的接入成本降低了 50% 以上 。
+ 针对 Confluent Kafka 客户端在非生产环境下的连接冗余问题，重构了 KafkaProducerService 的初始化逻辑，引入环境感知的条件加载机制 (Lazy Loading) ，彻底解决开发环境下因 Kafka 禁用导致的 CPU 资源空转与日志泛滥问题。
+ 主导 Link (连接器) 与 AITools (AI 能力) 子系统的解耦拆分，通过定义标准化的 HTTP/MCP 协议接口 ，实现了外部工具（如 API）与内部原子能力（如 OCR、TTS）的无缝热插拔， 支持业务方在不重启核心引擎的情况下动态扩展 10+ 种新的 AI 处理能力。
+ 基于 DSL (Domain Specific Language) 解析器与 DAG (有向无环图) 调度算法，自主研发支持并行与串行混合编排的 Workflow 引擎，实现了 10+ 种节点类型（大模型、插件、代码、逻辑控制等）的自动化调度，支持单次执行链路长度超 50+ 节点，复杂工作流执行耗时降低 30% 。
+ **深入源码排查 Spring AI 1.1 对接阿里百炼等非标准 OpenAI 接口时的路径拼接 bug，通过重写 baseUrl 和 completionsPath 的初始化逻辑， 实现“兼容模式”下特殊 API 路由规则的精准适配，确保 Qwen 等国产大模型在 Spring AI 框架下的无缝接入**。
+ 利用 Server-Sent Events (SSE) 技术，设计并实现了全链路流式响应机制，支持 LLM 生成内容的实时逐字推送；通过回调处理器 (Callback Handler) 模式，实现节点状态变更、中间结果输出及 Token 消耗统计的实时反馈， 首字响应延迟优化至 200ms 以内 。
+ 封装 Spark LLM 等多种基座模型的调用接口，实现 Prompt 模板的动态渲染与参数注入；针对 JSON 输出场景，开发了定制化的正则解析器与容错重试机制 ，解决大模型输出格式不稳定导致的解析失败问题。
+ 基于 Spring AI 重构 WorkflowEngine 核心调度器，实现节点执行器 ( NodeExecutor ) 的自动发现与注册机制，支持 10+ 种不同类型节点（LLM、插件、代码等）的无缝接入。
+ 开发 VariablePool (变量池) 组件，通过引入 FastJSON2 进行对象的序列化与深拷贝，彻底解决多节点并发执行时的数据污染与类型转换问题， 支持复杂对象（JSON/List/Map）在节点间的精准传递与引用。
+ 定义标准化的 StreamCallback 接口，打通从底层节点执行到上层 HTTP SSE 响应的流式数据通路；通过异步回调与事件驱动，实现 LLM 生成内容的毫秒级实时推送。
+ 利用 Java 并发包 ( ConcurrentLinkedQueue 与 ThreadPoolExecutor )，实现基于 BFS (广度优先搜索) 的节点调度算法，支持无依赖关系的并行节点同时执行， 在多分支复杂工作流场景下，整体执行耗时减少 40% 以上 。
+ **构建无状态的异构大模型统一网关，基于 Spring AI  设计了“即用即毁”的轻量级客户端工厂 buildChatModel，将 OpenAI、智谱、讯飞等模型的调用差异抹平为标准的流式响应 Flux<ChatResponse>**； 从而实现模型层与业务层的彻底解耦，并支持在单次工作流执行中动态切换不同底座模型，且无任何会话状态残留风险。
+ 针对 LLM 接口无状态的特性，在 LLMNodeExecutor 中设计了基于 LlmChatHistory 的上下文注入机制，在每次请求前自动装载历史对话与滑动窗口策略；成功在无状态的 HTTP 协议上构建出有状态的多轮对话能力，确保模型在长链路交互中“人设”与“记忆”的连续。
+ 负责动态角色编排与提示词渲染系，开发 VariableTemplateRender 模版引擎，实现了 System Prompt 与 User Prompt 的运行时动态渲染；通过将工作流变量实时注入到 Prompt 上下文中， 支持模型在不同节点中的业务角色（如 AI 播客、代码 reviewer），实现从“通用大模型”到“垂直领域专家”的零代码配置转型。
+ 设计分级的异常处理机制，针对节点执行失败、超时、参数错误等不同场景实现了精细化的错误捕获与状态流转；结合 ErrorStrategyEnum 策略枚举，支持“忽略错误继续执行”与“阻断执行”两种模式， 大幅提升工作流运行的鲁棒性 。
+ 负责工作流 DSL (领域特定语言) 的 Java 端解析逻辑，实现了从前端 JSON 配置到后端 WorkflowDSL 实体对象的自动映射与校验；通过预处理校验逻辑 ，在执行前即可发现循环依赖、孤立节点等结构性错误。
+ 引入装饰器模式，将核心业务逻辑封装在 doExecuteWithTimeout 方法中，利用 Guava TimeLimiter 实现了毫秒级的超时中断控制；同时构建了可配置的重试策略（支持线性退避与指数退避）， 保障在外部服务（如 LLM API）不稳定时的系统仍然正常运行。
+ 基于 TTL 封装了 AsyncUtil 全局异步工具类，解决父子线程间上下文丢失的问题；自定义带有守护线程属性的 ThreadFactory 与 SynchronousQueue 策略， 防止高并发场景下的线程资源耗尽与 OOM 风险，支持单机并发处理 500+ 工作流节点。
+ 实现了轻量级的 VariableTemplateRender 引擎，基于 Regex 正则预编译技术，支持对 Prompt 提示词中 {{node-id.variable}} 格式变量的毫秒级解析与替换；针对 JSON/List 等复杂嵌套数据结构，实现了递归式参数提取逻辑， Prompt 渲染速度较传统字符串替换方案提升 50倍。
+ 定义节点生命周期状态机枚举，并通过 EngineContextHolder 实现线程隔离的执行上下文管理 ；结合日志埋点，支持对每个节点执行耗时、输入输出及重试次数的全链路追踪， 将线上问题的平均排查时间 (MTTR) 从小时级缩短至分钟级。
+ 在 WorkflowMsgCallback 中设计了基于 ConcurrentLinkedQueue 的双队列架构（数据流队列 + 排序队列），通过独立的异步消费者线程将 LLM 生成的 Token 片段、节点状态变更事件与结构化日志进行解耦处理与有序重组，彻底消除高并发场景下 SSE 响应乱序与丢包现象，消息投递可靠性达到 99.99% 。
+ 封装轻量级的 S3ClientUtil ，统一文件上传、预签名 URL 生成及 Bucket 策略管理逻辑；实现基于流式传输的大文件处理机制，支持在工作流节点间高效传递图片、PDF 等多媒体数据，同时通过自定义的 Public Read 策略配置，解决私有化部署环境下文件外网访问受限的问题。
+ 构建了涵盖 WorkflowID 、 SessionID (SID) 及 EventID 的全链路唯一标识体系，贯穿请求接入、引擎调度、日志追踪与数据落盘的全过程， 为系统提供分布式链路追踪与全息日志分析。
+ 在 PluginNode 中实现了基于 ReAct 范式的工具调用逻辑，支持大模型根据上下文自动决策、参数提取与 Link 插件系统的 API 调用。



## 案例 2 Ai Agent 综合应用提效智能体 
面向业务应用系统提效，构建一套 Ai Agent 综合智能体方案，覆盖需求文档分析、代码评审、文档资料编写 + 消息通知、ELK 日志检索与 Prometheus 监控的智能分析等场景；将 Advisor / Prompt / MCP 等能力抽象入库，实现自由配置与编排，按需组装不同场景的 Ai Auto Agent。 

技术栈：Spring AI（RAG、MCP、Advisor）、Spring Boot、MyBatis、MySQL、PGVector、Redis、React、flowgram.ai、Docker 等 

核心职责： 

+ 围绕业务提效目标完成需求调研与方案设计，基于 DDD 分层架构进行模块拆分与领域建模，沉淀可复用的可编排 Ai Agent 执行能力（配置 化装配 + 运行时注入）。 
+ 抽象 Ai Agent 运行元模型：Advisor（上下文记忆 / 接入 RAG）、Prompt、Model、Tool（Function Call / MCP）；通过数据库表实现表 驱动编排、灰度与版本化管理（含编排配置版本）。 
+ 设计通用对话分析与执行引擎，实现“问题分析 -> 任务规划 -> 精准执行 -> 结果校验/判罚 -> 循环优化 -> 总结输出”闭环，并通过SSE流 式回传过程与结果。 
+ 实现 MCP Client 适配层（stdio / SSE），以配置化方式接入外部检索/监控/日志类 MCP 工具能力，统一注册管理并装配到模型Tool Callback调用链路。 
+ 搭建 RAG 知识库（PGVector）能力，支持文件/资料解析入库，按标签管理知识空间供 Advisor 检索增强，提升问答/评审/分析准确性与一 致性。 
+ 基于 Spring TaskScheduler 封装任务调度与触发机制，支持按配置定时触发智能体执行，并可扩展至巡检/告警分析/报告生成等业务场景。 
+ 基于 React + flowgram.ai 实现拖拽式编排能力，支持运营侧可视化配置与发布 Ai Agent 流程，提升使用体验与场景迭代效率。



## 案例 3：北邮 ICT 智教平台 Java 后端开发 2025.02 - 2025.04
项目简介：基于 Dify 搭建教育场景下的 AI 中台系统，集成 DeepSeek、Qwen 等主流大模型及多智能体 Agent，打通校内认证体系与模型服务大厅，支撑日均 1w+ 请求量的智能问答、论文审阅、代码辅助等 10+ AI 应用。

• 主导 Dify 框架的本地化二次开发与部署，设计“智教应用广场”模块，支持多类 Agent 应用自定义注册、分类聚合与独立配置

• 建设统一认证中心，打通学号/工号登录链路，结合 JWT + OAuth2 实现与北邮门户系统的单点登录（SSO），完成用户态上下文的安全透传

• 开发多模型服务网关，兼容接入 DeepSeek/Qwen 等大模型 API，支持模型动态调度与透明接入

