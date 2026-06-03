package orange.wz.mcp.tool.impl;

import orange.wz.mcp.dto.OverwriteStrategy;
import orange.wz.mcp.service.McpWorkspaceService;
import orange.wz.mcp.session.McpSessionManager;
import orange.wz.mcp.tool.support.BaseSessionTool;
import orange.wz.mcp.tool.support.ToolParamHelper;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;

import static orange.wz.mcp.tool.support.ToolSchemas.*;

public final class PasteNodesTool extends BaseSessionTool {
    private final McpWorkspaceService service;

    public PasteNodesTool(McpSessionManager sessionManager, McpWorkspaceService service) {
        super(sessionManager, "将会话剪贴板内容粘贴到一个或多个目标节点。单目标使用 rootPath/nodePath；批量使用 targets 数组。", objectSchema(
                Map.of(
                        "targets", arraySchema(objectSchema(
                                Map.of(
                                        "rootPath", stringSchema(),
                                        "nodePath", stringSchema(),
                                        "strategy", stringSchema(),
                                        "autoParse", booleanSchema()
                                ),
                                List.of("rootPath")
                        )),
                        "rootPath", stringSchema(),
                        "nodePath", stringSchema(),
                        "strategy", stringSchema(),
                        "autoParse", booleanSchema()
                ),
                List.of("rootPath")
        ));
        this.service = service;
    }

    @Override
    public String name() {
        return "paste_nodes";
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> params) {
        var session = session(params);
        Object rawTargets = params.get("targets");
        if (rawTargets instanceof List<?>) {
            List<Map<String, Object>> targets = ToolParamHelper.getObjectList(params, "targets");
            List<Map<String, Object>> results = new ArrayList<>();
            String defaultStrategy = ToolParamHelper.getString(params, "strategy", OverwriteStrategy.ERROR.name());
            boolean defaultAutoParse = ToolParamHelper.getBoolean(params, "autoParse", true);
            session.lock();
            try {
                for (Map<String, Object> targetParams : targets) {
                    var target = ToolParamHelper.getNodeReference(targetParams);
                    boolean autoParse = ToolParamHelper.getBoolean(targetParams, "autoParse", defaultAutoParse);
                    String strategyText = ToolParamHelper.getString(targetParams, "strategy", defaultStrategy);
                    OverwriteStrategy strategy = OverwriteStrategy.valueOf(strategyText.toUpperCase(Locale.ROOT));
                    results.add(Map.of(
                            "rootPath", target.rootPath(),
                            "nodePath", target.nodePath(),
                            "pasted", service.pasteToNode(session, target, strategy, autoParse)
                    ));
                }
            } finally {
                session.unlock();
            }
            return Map.of("results", results);
        }
        var target = ToolParamHelper.getNodeReference(params);
        boolean autoParse = ToolParamHelper.getBoolean(params, "autoParse", true);
        String strategyText = ToolParamHelper.getString(params, "strategy", OverwriteStrategy.ERROR.name());
        OverwriteStrategy strategy = OverwriteStrategy.valueOf(strategyText.toUpperCase(Locale.ROOT));
        return Map.of("pasted", service.pasteToNode(session, target, strategy, autoParse));
    }
}
