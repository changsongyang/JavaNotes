package com.wdbyte.ai.mcp.manual;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter.Feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mcp")
public class McpWeatherController {

    private static final Logger log = LoggerFactory.getLogger(McpWeatherController.class);

    // 1. 静态化工具定义，使 tools/list 极其简洁
    private static final List<Tool> AVAILABLE_TOOLS = List.of(
        new Tool("getWeather", "获取指定城市的天气预报",
            JSONObject.parseObject("""
                {
                    "type": "object",
                    "properties": { "city": { "type": "string", "description": "城市名" } },
                    "required": ["city"],
                    "additionalProperties": false
                }
                """))
    );

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> handleMcpRequest(@RequestBody JsonRpcRequest request) {
        Object id = request.id();

         var response = switch (request.method()) {
            case "initialize" -> ok(id, new InitializeResult());
            case "notifications/initialized" -> accepted();
            case "ping" -> ok(id, Map.of());
            case "tools/list" -> ok(id, Map.of("tools", AVAILABLE_TOOLS));
            case "tools/call" -> handleToolCall(id, request.params());
            default -> ResponseEntity.notFound().build();
        };
        log.info("\nrequest: {}\nresponse: {}", JSON.toJSONString(request), JSON.toJSONString(response.getBody()));
         return response;
    }

    /**
     * 优雅处理工具调用：直接通过 JSONObject 转换，无需 String 二次中转
     */
    private ResponseEntity<?> handleToolCall(Object id, JSONObject params) {
        if (params == null) return badRequest();

        var callParams = params.toJavaObject(ToolCallParams.class);

        // 使用 switch 处理多工具扩展性更好
        return switch (callParams.name()) {
            case "getWeather" -> {
                String city = String.valueOf(callParams.arguments().getOrDefault("city", "未知城市"));
                yield ok(id, new ToolCallResult(city + "今日雷暴雨，建议居家"));
            }
            default -> badRequest();
        };
    }

    // --- 辅助方法 ---
    private static ResponseEntity<JsonRpcResponse> ok(Object id, Object result) {
        return ResponseEntity.ok(new JsonRpcResponse(id, result));
    }

    private static ResponseEntity<Void> accepted() {
        return ResponseEntity.status(202).build();
    }

    private static ResponseEntity<Void> badRequest() {
        return ResponseEntity.badRequest().build();
    }

    // --- MCP 协议 Records (Java 21) ---

    // 将 params 定义为 JSONObject，方便后续 toJavaObject 转换
    public record JsonRpcRequest(String jsonrpc, Object id, String method, JSONObject params) {}

    public record JsonRpcResponse(String jsonrpc, Object id, Object result) {
        public JsonRpcResponse(Object id, Object result) {
            this("2.0", id, result);
        }
    }

    // 初始化结果模型
    public record InitializeResult(String protocolVersion, Capabilities capabilities, ServerInfo serverInfo) {
        public InitializeResult() {
            this("2025-06-18", new Capabilities(new Tools(false)), new ServerInfo("mcp-weather-server", "1.0.0"));
        }
    }

    public record ServerInfo(String name, String version) {}
    public record Capabilities(Tools tools) {}
    public record Tools(boolean listChanged) {}

    // 工具定义模型
    public record Tool(String name, String description, Object inputSchema) {}

    // 工具调用参数模型
    public record ToolCallParams(String name, Map<String, Object> arguments) {}

    // 响应内容模型
    public record Content(String type, String text) {
        public Content(String text) { this("text", text); }
    }

    public record ToolCallResult(List<Content> content, boolean isError) {
        public ToolCallResult(String text) {
            this(List.of(new Content(text)), false);
        }
    }
}
