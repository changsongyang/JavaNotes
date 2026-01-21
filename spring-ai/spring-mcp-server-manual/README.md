这个基于 Spring Boot 和 Fastjson 的 MCP（Model Context Protocol）服务实现，是一个遵循 JSON-RPC 2.0 规范的轻量级工具服务端，旨在为大语言模型（LLM）提供标准化的外部能力接口 36。

以下是该实现的核心架构与技术特点介绍：

### 1. 核心协议架构
该实现采用了 MCP 的 Streamable HTTP 传输机制，通过单一的 HTTP POST 端点（/mcp）处理所有交互逻辑 26。

消息格式：所有请求和响应均严格遵循 JSON-RPC 2.0 结构，包含 jsonrpc、id、method 和 params 字段 6。
生命周期管理：手动实现了 MCP 协议定义的完整链路，包括初始化握手（initialize）、初始化完成通知（notifications/initialized）、工具发现（tools/list）以及工具执行（tools/call） 56。
### 2. Java 21 技术优化
利用 Java 21 的现代语法特性极大简化了协议模版代码：

Record 记录类：使用 record 定义 JsonRpcRequest、Tool 和 ToolCallResult 等数据模型。这消除了 Getter/Setter 等样板代码，确保了消息对象的不可变性，并自动支持 JSON 序列化。
Switch 表达式：在控制器中使用增强的 switch 表达式处理 method 分发。这种方式比传统的 if-else 更具读性，且利用 yield 关键字实现了逻辑的紧凑闭环。
文本块（Text Blocks）：利用 """ 语法定义工具的 JSON Schema。这使得复杂的输入参数描述（如 inputSchema）在代码中能以原始 JSON 格式直观呈现，便于维护 5。
### 3. 工具定义与执行逻辑
该服务模拟了一个名为 getWeather 的城市天气查询工具：

工具发现：在 tools/list 阶段，服务端会返回该工具的名称、描述以及基于 JSON Schema 的参数定义（要求必填 city 字符串），以便 LLM 理解如何调用该工具 56。
参数解析：通过 Fastjson 的 JSONObject 直接处理动态参数。在 tools/call 触发时，程序会从 arguments 映射中提取城市名称，并返回标准化的内容结构。
响应规范：响应体封装在 content 数组中，并包含 isError 标识，这符合 MCP 对工具执行结果的标准化要求 6。
### 4. 最佳实践体现
轻量化：不依赖于复杂的 MCP 官方 SDK，仅通过 Spring Boot 基础框架和 Fastjson 实现，适合快速集成到现有生产微服务中 2。
无状态处理：服务设计为无状态，符合 MCP Streamable HTTP 的简化模式，便于水平扩展。
错误处理基础：虽然为简易版，但结构上预留了 isError 字段，允许在工具内部出错时让 LLM 感知并尝试自我修正 6。
这种实现方式展示了如何通过极简的代码量构建符合开放协议标准的 AI 插件系统，降低了 LLM 与私有数据源及外部工具对接的复杂度 13。