package orange.wz.mcp.ui;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.mcp.dto.NodeDetail;
import orange.wz.mcp.dto.NodeReference;
import orange.wz.mcp.dto.NodeSummary;
import orange.wz.mcp.session.McpSessionState;
import orange.wz.provider.WzObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public final class McpUiBridge {
    @Value("${orange.gui.enabled:true}")
    private boolean guiEnabled;

    public void syncSessionRoots(McpSessionState session) {
        if (!isAvailable()) {
            return;
        }
        runOnEdt(() -> {
            EditPane editPane = leftPane();
            session.lock();
            try {
                session.getRoots().clear();
                session.getRoots().addAll(editPane.snapshotRootObjects());
            } finally {
                session.unlock();
            }
        });
    }

    public void beforeToolCall(String toolName, Map<String, Object> arguments) {
        if (!isAvailable()) {
            return;
        }
        String message = switch (toolName) {
            case "load_files" -> "MCP 正在加载文件...";
            case "unload_node" -> "MCP 正在卸载节点...";
            case "unload_all" -> "MCP 正在清空工作区...";
            case "create_wz_file" -> "MCP 正在创建 WZ 文件...";
            case "create_img_file" -> "MCP 正在创建 IMG 文件...";
            case "create_child_node" -> "MCP 正在创建子节点...";
            case "delete_node" -> "MCP 正在删除节点...";
            case "copy_nodes" -> "MCP 正在复制节点...";
            case "paste_nodes" -> "MCP 正在粘贴节点...";
            case "save_node" -> "MCP 正在保存文件...";
            case "save_as" -> "MCP 正在另存为...";
            case "search_node" -> "MCP 正在搜索节点...";
            case "find_node" -> "MCP 正在查找节点...";
            case "query_nodes" -> "MCP 正在执行统一查询...";
            case "get_node_detail" -> "MCP 正在读取节点详情...";
            case "list_children" -> "MCP 正在列出子节点...";
            case "get_node_tree_json" -> "MCP 正在读取节点树...";
            case "batch_update_nodes", "mutate_nodes" -> "MCP 正在修改节点...";
            default -> "MCP 正在执行 " + toolName + "...";
        };
        runOnEdt(() -> {
            prepareNodeTargets(leftPane(), arguments);
            MainFrame frame = frame();
            frame.updateProgress(0, 0);
            frame.setStatusTextDirect(message);
        });
    }

    @SuppressWarnings("unchecked")
    public void afterToolCall(String toolName, Map<String, Object> arguments, Map<String, Object> result) {
        if (!isAvailable()) {
            return;
        }
        runOnEdt(() -> {
            EditPane editPane = leftPane();
            MainFrame frame = frame();
            switch (toolName) {
                case "load_files", "create_wz_file", "create_img_file" -> syncAllRoots(editPane, arguments);
                case "unload_all" -> editPane.unloadAll();
                case "unload_node", "delete_node" -> {
                    NodeReference reference = nodeReference(arguments);
                    removeNode(editPane, reference);
                    focusParent(editPane, reference);
                }
                case "create_child_node" -> {
                    NodeReference parent = nodeReference(arguments);
                    insertChild(editPane, extractNode(result), parent);
                    focusNode(editPane, extractNode(result));
                }
                case "paste_nodes" -> {
                    insertPastedResults(editPane, result, nodeReference(arguments));
                    focusFirstPasted(editPane, result);
                }
                case "find_node", "search_node" -> {
                    NodeSummary node = extractNode(result);
                    if (node != null) {
                        focusNode(editPane, node);
                    } else {
                        focusFirstNodeFromResultItems(editPane, result);
                    }
                }
                case "get_node_detail" -> {
                    NodeSummary node = extractNode(result);
                    if (node != null) {
                        focusNode(editPane, node);
                    } else if (focusFirstNodeFromResultItems(editPane, result)) {
                        // Already focused.
                    } else {
                        focusFirstReference(editPane, arguments);
                    }
                }
                case "list_children", "get_node_tree_json" -> focusFirstReference(editPane, arguments);
                case "batch_update_nodes", "mutate_nodes" -> {
                    applyMutationResults(editPane, result);
                    focusFirstUpdated(editPane, result);
                }
                case "query_nodes" -> focusFirstNodeFromResults(editPane, result);
                case "save_node", "save_as" -> {
                    NodeReference reference = nodeReference(arguments);
                    if (reference != null) {
                        editPane.reloadFilePreservingStateByRootPath(reference.rootPath());
                    }
                }
                default -> {
                }
            }
            frame.updateProgress(0, 0);
            frame.setStatusTextDirect(successMessage(toolName, result));
            editPane.getTree().updateUI();
        });
    }

    private void syncAllRoots(EditPane editPane, Map<String, Object> arguments) {
        Object sessionRoots = arguments.get("__sessionRoots");
        if (!(sessionRoots instanceof List<?> roots)) {
            return;
        }
        editPane.unloadAll();
        for (Object obj : roots) {
            if (obj instanceof WzObject wzObject) {
                editPane.insertNodeToTree(editPane.getTreeRoot(), wzObject, true);
            }
        }
    }

    private void removeNode(EditPane editPane, NodeReference reference) {
        if (reference == null) {
            return;
        }
        editPane.ensureRootNodeParsedByRootPath(reference.rootPath());
        DefaultMutableTreeNode node = editPane.findTreeNodeByReference(reference.rootPath(), reference.nodePath());
        if (node != null) {
            editPane.removeNodeFromTree(node);
            editPane.resetValueForm();
        }
    }

    private void insertChild(EditPane editPane, NodeSummary nodeSummary, NodeReference parent) {
        if (nodeSummary == null || parent == null) {
            return;
        }
        editPane.ensureRootNodeParsedByRootPath(parent.rootPath());
        DefaultMutableTreeNode parentNode = editPane.findTreeNodeByReference(parent.rootPath(), parent.nodePath());
        if (parentNode == null) {
            return;
        }
        WzObject parentObj = (WzObject) parentNode.getUserObject();
        WzObject childObj = findChildObject(parentObj, nodeSummary.name());
        if (childObj != null && editPane.findTreeNodeByReference(nodeSummary.rootPath(), nodeSummary.nodePath()) == null) {
            editPane.insertNodeToTree(parentNode, childObj, true);
        }
    }

    private void insertPasted(EditPane editPane, List<Object> pastedItems, NodeReference target) {
        if (pastedItems == null || target == null) {
            return;
        }
        for (Object item : pastedItems) {
            NodeSummary nodeSummary = item instanceof NodeSummary summary ? summary : nodeSummary(item);
            if (nodeSummary != null) {
                insertChild(editPane, nodeSummary, target);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void insertPastedResults(EditPane editPane, Map<String, Object> result, NodeReference singleTarget) {
        Object rawResults = result.get("results");
        if (rawResults instanceof List<?> results) {
            for (Object item : results) {
                if (item instanceof Map<?, ?> map) {
                    insertPasted(editPane, (List<Object>) map.get("pasted"), nodeReference(map));
                }
            }
            return;
        }
        insertPasted(editPane, (List<Object>) result.get("pasted"), singleTarget);
    }

    private void focusNode(EditPane editPane, NodeSummary node) {
        if (node == null) {
            return;
        }
        editPane.focusNodeByReference(node.rootPath(), node.nodePath());
    }

    private void focusReference(EditPane editPane, NodeReference reference) {
        if (reference == null) {
            return;
        }
        editPane.focusNodeByReference(reference.rootPath(), reference.nodePath());
    }

    private void focusParent(EditPane editPane, NodeReference reference) {
        if (reference == null || reference.nodePath().isBlank()) {
            return;
        }
        int index = reference.nodePath().lastIndexOf('/');
        String parentNodePath = index < 0 ? "" : reference.nodePath().substring(0, index);
        focusReference(editPane, new NodeReference(reference.rootPath(), parentNodePath));
    }

    private NodeSummary extractNode(Map<String, Object> result) {
        Object raw = result.get("node");
        if (raw instanceof NodeSummary nodeSummary) {
            return nodeSummary;
        }
        NodeSummary mapped = nodeSummary(raw);
        if (mapped != null) {
            return mapped;
        }
        Object detail = result.get("detail");
        if (detail instanceof NodeDetail nodeDetail) {
            return nodeDetail.node();
        }
        if (detail instanceof Map<?, ?> detailMap) {
            Object rawNode = detailMap.get("node");
            if (rawNode instanceof NodeSummary nodeSummary) {
                return nodeSummary;
            }
            return nodeSummary(rawNode);
        }
        return null;
    }

    private NodeSummary nodeSummary(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Object name = map.get("name");
        Object rootPath = map.get("rootPath");
        Object nodePath = map.get("nodePath");
        if (name instanceof String n && rootPath instanceof String r) {
            return new NodeSummary(n, r, nodePath instanceof String p ? p : "", null);
        }
        return null;
    }

    private String successMessage(String toolName, Map<String, Object> result) {
        return switch (toolName) {
            case "load_files" -> "MCP 文件加载完成";
            case "unload_node" -> "MCP 节点卸载完成";
            case "unload_all" -> "MCP 工作区已清空";
            case "create_wz_file" -> "MCP WZ 文件创建完成";
            case "create_img_file" -> "MCP IMG 文件创建完成";
            case "create_child_node" -> "MCP 子节点创建完成";
            case "delete_node" -> "MCP 节点删除完成";
            case "copy_nodes" -> "MCP 节点复制完成";
            case "paste_nodes" -> "MCP 节点粘贴完成";
            case "save_node" -> "MCP 文件保存完成";
            case "save_as" -> "MCP 文件另存为完成";
            case "search_node" -> "MCP 节点搜索完成";
            case "find_node" -> "MCP 节点查找完成";
            case "query_nodes" -> "MCP 统一查询完成";
            case "get_node_detail" -> "MCP 节点详情读取完成";
            case "list_children" -> "MCP 子节点列出完成";
            case "get_node_tree_json" -> "MCP 节点树读取完成";
            case "batch_update_nodes" -> "MCP 批量修改完成";
            case "mutate_nodes" -> "MCP 统一修改完成";
            default -> "MCP 执行完成";
        };
    }

    private void applyMutationResults(EditPane editPane, Map<String, Object> result) {
        Object rawResult = result.get("result");
        if (rawResult instanceof Map<?, ?> resultMap) {
            applyMutationResultItem(editPane, resultMap);
            return;
        }
        Object rawResults = result.get("results");
        if (!(rawResults instanceof List<?> results)) {
            return;
        }
        for (Object item : results) {
            if (item instanceof Map<?, ?> map) {
                applyMutationResultItem(editPane, map);
            }
        }
    }

    private void applyMutationResultItem(EditPane editPane, Map<?, ?> map) {
        Object rawOp = map.get("op");
        if (!(rawOp instanceof String op)) {
            return;
        }
        switch (op) {
            case "create_child" -> insertChild(editPane, nodeSummary(map.get("node")), nodeReference(map));
            case "delete" -> removeNode(editPane, nodeReference(map));
            case "save", "save_as" -> {
                NodeReference reference = nodeReference(map);
                if (reference != null) {
                    editPane.reloadFilePreservingStateByRootPath(reference.rootPath());
                }
            }
            default -> {
            }
        }
    }

    private void focusFirstNodeFromResults(EditPane editPane, Map<String, Object> result) {
        Object rawResult = result.get("result");
        if (rawResult instanceof Map<?, ?> resultMap && focusFirstNodeFromResultItem(editPane, resultMap)) {
            return;
        }
        Object rawResults = result.get("results");
        if (!(rawResults instanceof List<?> results)) {
            return;
        }
        for (Object item : results) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object matchesObj = map.get("matches");
            if (!(matchesObj instanceof List<?> matches) || matches.isEmpty()) {
                continue;
            }
            Object first = matches.get(0);
            if (first instanceof NodeSummary nodeSummary) {
                focusNode(editPane, nodeSummary);
                return;
            }
            NodeSummary nodeSummary = nodeSummary(first);
            if (nodeSummary != null) {
                focusNode(editPane, nodeSummary);
                return;
            }
        }
    }

    private boolean focusFirstNodeFromResultItems(EditPane editPane, Map<String, Object> result) {
        Object rawResults = result.get("results");
        if (!(rawResults instanceof List<?> results)) {
            return false;
        }
        for (Object item : results) {
            if (item instanceof Map<?, ?> map && focusFirstNodeFromResultItem(editPane, map)) {
                return true;
            }
        }
        return false;
    }

    private boolean focusFirstNodeFromResultItem(EditPane editPane, Map<?, ?> map) {
        Object matchesObj = map.get("matches");
        if (matchesObj instanceof List<?> matches && !matches.isEmpty()) {
            Object first = matches.get(0);
            if (first instanceof NodeSummary nodeSummary) {
                focusNode(editPane, nodeSummary);
                return true;
            }
            NodeSummary matchNode = nodeSummary(first);
            if (matchNode != null) {
                focusNode(editPane, matchNode);
                return true;
            }
        }
        Object rawNode = map.get("node");
        if (rawNode instanceof NodeSummary nodeSummary) {
            focusNode(editPane, nodeSummary);
            return true;
        }
        NodeSummary mapped = nodeSummary(rawNode);
        if (mapped != null) {
            focusNode(editPane, mapped);
            return true;
        }
        Object detail = map.get("detail");
        if (detail instanceof NodeDetail nodeDetail) {
            focusNode(editPane, nodeDetail.node());
            return true;
        }
        if (detail instanceof Map<?, ?> detailMap) {
            NodeSummary detailNode = nodeSummary(detailMap.get("node"));
            if (detailNode != null) {
                focusNode(editPane, detailNode);
                return true;
            }
        }
        NodeReference reference = nodeReference(map);
        if (reference != null) {
            focusReference(editPane, reference);
            return true;
        }
        return false;
    }

    private void focusFirstUpdated(EditPane editPane, Map<String, Object> result) {
        Object rawResult = result.get("result");
        if (rawResult instanceof Map<?, ?> resultMap) {
            Object rawOp = resultMap.get("op");
            if ("delete".equals(rawOp)) {
                NodeReference reference = nodeReference(resultMap);
                if (reference != null) {
                    focusParent(editPane, reference);
                    return;
                }
            }
            if (focusFirstNodeFromResultItem(editPane, resultMap)) {
                return;
            }
        }
        Object rawResults = result.get("results");
        if (!(rawResults instanceof List<?> results)) {
            return;
        }
        for (Object item : results) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object rawOp = map.get("op");
            if ("delete".equals(rawOp)) {
                NodeReference reference = nodeReference(map);
                if (reference != null) {
                    focusParent(editPane, reference);
                    return;
                }
            }
            Object rawNode = map.get("node");
            if (rawNode instanceof NodeSummary nodeSummary) {
                focusNode(editPane, nodeSummary);
                return;
            }
            NodeSummary mapped = nodeSummary(rawNode);
            if (mapped != null) {
                focusNode(editPane, mapped);
                return;
            }
            NodeReference reference = nodeReference(map);
            if (reference != null) {
                focusReference(editPane, reference);
                return;
            }
        }
    }

    private void focusFirstReference(EditPane editPane, Map<String, Object> source) {
        Set<NodeReference> references = new LinkedHashSet<>();
        collectNodeReferences(source, references);
        for (NodeReference reference : references) {
            focusReference(editPane, reference);
            return;
        }
    }

    private void focusFirstPasted(EditPane editPane, Map<String, Object> result) {
        Object rawResults = result.get("results");
        if (rawResults instanceof List<?> results) {
            for (Object item : results) {
                if (item instanceof Map<?, ?> map) {
                    Object nestedPasted = map.get("pasted");
                    if (nestedPasted instanceof List<?> nested && !nested.isEmpty()) {
                        Object first = nested.get(0);
                        if (first instanceof NodeSummary nodeSummary) {
                            focusNode(editPane, nodeSummary);
                            return;
                        }
                        NodeSummary mapped = nodeSummary(first);
                        if (mapped != null) {
                            focusNode(editPane, mapped);
                            return;
                        }
                    }
                }
            }
            return;
        }
        Object rawPasted = result.get("pasted");
        if (!(rawPasted instanceof List<?> pasted) || pasted.isEmpty()) {
            return;
        }
        Object first = pasted.get(0);
        if (first instanceof NodeSummary nodeSummary) {
            focusNode(editPane, nodeSummary);
            return;
        }
        NodeSummary mapped = nodeSummary(first);
        if (mapped != null) {
            focusNode(editPane, mapped);
        }
    }

    private WzObject findChildObject(WzObject parent, String childName) {
        if (parent == null || childName == null) {
            return null;
        }
        if (parent instanceof orange.wz.provider.WzDirectory dir) {
            WzObject subDir = dir.getDirectory(childName);
            if (subDir != null) return subDir;
            return dir.getImage(childName);
        }
        if (parent instanceof orange.wz.provider.WzImage image) {
            return image.getChild(childName);
        }
        if (parent instanceof orange.wz.provider.WzImageProperty prop) {
            return prop.getChild(childName);
        }
        return null;
    }

    private void prepareNodeTargets(EditPane editPane, Map<String, Object> arguments) {
        Set<NodeReference> references = new LinkedHashSet<>();
        collectNodeReferences(arguments, references);
        for (NodeReference reference : references) {
            editPane.ensureRootNodeParsedByRootPath(reference.rootPath());
        }
    }

    @SuppressWarnings("unchecked")
    private void collectNodeReferences(Object source, Set<NodeReference> collector) {
        if (source instanceof Map<?, ?> map) {
            NodeReference reference = nodeReference(map);
            if (reference != null) {
                collector.add(reference);
            }
            for (Object value : map.values()) {
                collectNodeReferences(value, collector);
            }
            return;
        }
        if (source instanceof List<?> list) {
            for (Object item : list) {
                collectNodeReferences(item, collector);
            }
        }
    }

    private NodeReference nodeReference(Map<?, ?> source) {
        if (source == null) {
            return null;
        }
        Object rootPath = source.get("rootPath");
        if (!(rootPath instanceof String r) || r.isBlank()) {
            return null;
        }
        Object nodePath = source.get("nodePath");
        return new NodeReference(r, nodePath instanceof String p ? p : "");
    }

    private boolean isAvailable() {
        return guiEnabled && !java.awt.GraphicsEnvironment.isHeadless();
    }

    private MainFrame frame() {
        return MainFrame.getInstance();
    }

    private EditPane leftPane() {
        return frame().getCenterPane().getLeftEditPane();
    }

    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
