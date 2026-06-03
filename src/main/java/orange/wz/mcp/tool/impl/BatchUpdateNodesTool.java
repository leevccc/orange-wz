package orange.wz.mcp.tool.impl;

import orange.wz.mcp.service.McpWorkspaceService;
import orange.wz.mcp.session.McpSessionManager;
import orange.wz.mcp.tool.support.BaseSessionTool;

import java.util.List;
import java.util.Map;

import static orange.wz.mcp.tool.support.ToolSchemas.*;

public final class BatchUpdateNodesTool extends BaseSessionTool {
    private final McpWorkspaceService service;

    public BatchUpdateNodesTool(McpSessionManager sessionManager, McpWorkspaceService service) {
        super(sessionManager, "兼容批量写入入口；新调用优先使用 mutate_nodes 的 operations 数组。支持 create_child、delete、rename、set_value、set_vector、set_png、set_sound、save、save_as。", objectSchema(
                Map.of("operations", arraySchema(updateOperationSchema())),
                List.of("operations")
        ));
        this.service = service;
    }

    @Override
    public String name() {
        return "batch_update_nodes";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) {
        var session = session(params);
        Object operations = params.get("operations");
        List<Map<String, Object>> list = operations instanceof List<?> raw ? (List<Map<String, Object>>) raw : List.of();
        return Map.of("results", service.batchUpdateNodes(session, list));
    }
}
