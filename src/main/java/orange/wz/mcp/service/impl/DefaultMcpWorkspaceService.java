package orange.wz.mcp.service.impl;

import orange.wz.mcp.dto.NodeDetail;
import orange.wz.mcp.dto.NodeReference;
import orange.wz.mcp.dto.NodeSummary;
import orange.wz.mcp.dto.OverwriteStrategy;
import orange.wz.mcp.resolve.NodePathResolver;
import orange.wz.mcp.service.McpWorkspaceService;
import orange.wz.mcp.session.McpSessionState;
import orange.wz.mcp.support.McpException;
import orange.wz.gui.utils.WzNodeUtil;
import orange.wz.provider.*;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.WzFileStatus;
import orange.wz.provider.tools.wzkey.WzKey;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DefaultMcpWorkspaceService implements McpWorkspaceService {
    private final NodePathResolver resolver = new NodePathResolver();

    @Override
    public void loadFiles(McpSessionState session, List<File> files, WzKey key) {
        if (key == null) throw new McpException("key 不能为空");
        if (files == null || files.isEmpty()) return;

        session.lock();
        try {
            List<String> requestedRootPaths = new ArrayList<>();
            for (File f : files) {
                if (f == null) continue;
                String requestedRootPath = NodePathResolver.normalizeRootPath(f.getAbsolutePath());
                ensureRootNotLoaded(session, requestedRootPath);
                ensureRootNotRepeated(requestedRootPaths, requestedRootPath);
                requestedRootPaths.add(requestedRootPath);
            }
            for (File f : files) {
                if (f == null) continue;
                if (f.isFile()) {
                    if (f.getName().endsWith(".wz")) {
                        WzFile wzFile = new WzFile(f.getAbsolutePath(), (short) -1, key.getName(), key.getIv(), key.getUserKey());
                        session.getRoots().add(wzFile.getWzDirectory());
                    } else if (f.getName().endsWith(".img")) {
                        session.getRoots().add(new WzImageFile(f.getName(), f.getAbsolutePath(), key.getName(), key.getIv(), key.getUserKey()));
                    } else if (f.getName().endsWith(".xml")) {
                        session.getRoots().add(new WzXmlFile(f.getName(), f.getAbsolutePath(), key.getName(), key.getIv(), key.getUserKey()));
                    }
                } else if (f.isDirectory()) {
                    session.getRoots().add(new WzFolder(f.getAbsolutePath(), key.getName(), key.getIv(), key.getUserKey()));
                }
            }
        } finally {
            session.unlock();
        }
    }

    private void ensureRootNotLoaded(McpSessionState session, String requestedRootPath) {
        for (WzObject root : session.getRoots()) {
            String loadedRootPath = NodePathResolver.rootPathOf(root);
            if (NodePathResolver.sameRootPath(loadedRootPath, requestedRootPath)) {
                throw new McpException("文件已加载，禁止重复加载: " + requestedRootPath);
            }
        }
    }

    private void ensureRootNotRepeated(List<String> requestedRootPaths, String requestedRootPath) {
        for (String existingPath : requestedRootPaths) {
            if (NodePathResolver.sameRootPath(existingPath, requestedRootPath)) {
                throw new McpException("同一次请求中存在重复加载路径: " + requestedRootPath);
            }
        }
    }

    @Override
    public void unloadNode(McpSessionState session, NodeReference reference) {
        session.lock();
        try {
            WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, false);
            if (obj.getParent() == null) {
                session.getRoots().remove(obj);
                return;
            }
            removeFromParent(obj.getParent(), obj);
        } finally {
            session.unlock();
        }
    }

    @Override
    public void unloadAll(McpSessionState session) {
        session.lock();
        try {
            session.getRoots().clear();
            session.getClipboard().clear();
        } finally {
            session.unlock();
        }
    }

    @Override
    public NodeSummary createWz(McpSessionState session, String fileName, short version, WzKey key) {
        if (fileName == null || fileName.isBlank()) throw new McpException("fileName 不能为空");
        if (key == null) throw new McpException("key 不能为空");
        if (!fileName.endsWith(".wz")) fileName = fileName + ".wz";
        WzFile wzFile = WzFile.createNewFile(fileName, version, key.getName(), key.getIv(), key.getUserKey());
        wzFile.setNewFile(true);
        wzFile.getWzDirectory().setTempChanged(true);
        session.getRoots().add(wzFile.getWzDirectory());
        return NodeSummary.from(wzFile.getWzDirectory());
    }

    @Override
    public NodeSummary createImg(McpSessionState session, String fileName, WzKey key) {
        if (fileName == null || fileName.isBlank()) throw new McpException("fileName 不能为空");
        if (key == null) throw new McpException("key 不能为空");
        if (!fileName.endsWith(".img")) fileName = fileName + ".img";
        WzImageFile wzImageFile = new WzImageFile(fileName, fileName, key.getName(), key.getIv(), key.getUserKey());
        wzImageFile.setReader(new BinaryReader(wzImageFile.getIv(), wzImageFile.getKey()));
        wzImageFile.setNewFile(true);
        wzImageFile.setStatus(WzFileStatus.PARSE_SUCCESS);
        wzImageFile.setChanged(true);
        wzImageFile.setTempChanged(true);
        session.getRoots().add(wzImageFile);
        return NodeSummary.from(wzImageFile);
    }

    @Override
    public List<NodeSummary> listLoadedRoots(McpSessionState session) {
        session.lock();
        try {
            List<NodeSummary> result = new ArrayList<>(session.getRoots().size());
            for (WzObject root : session.getRoots()) {
                result.add(NodeSummary.from(root));
            }
            return result;
        } finally {
            session.unlock();
        }
    }

    @Override
    public WzObject findNode(McpSessionState session, NodeReference reference, boolean autoParse) {
        return resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
    }

    @Override
    public List<NodeSummary> listChildren(McpSessionState session, NodeReference reference, boolean autoParse) {
        WzObject parent = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        List<WzObject> children = getChildren(parent);
        List<NodeSummary> result = new ArrayList<>();
        for (WzObject child : children) {
            result.add(NodeSummary.from(child));
        }
        return result;
    }

    @Override
    public void copyNodes(McpSessionState session, List<NodeReference> sources, boolean autoParse) {
        if (sources == null || sources.isEmpty()) return;
        session.lock();
        try {
            session.getClipboard().clear();
            for (NodeReference source : sources) {
                WzObject obj = resolver.resolveFromRoots(session.getRoots(), source, autoParse);
                session.getClipboard().add(obj.deepClone(null));
            }
        } finally {
            session.unlock();
        }
    }

    @Override
    public List<NodeSummary> pasteToNode(McpSessionState session, NodeReference targetReference, OverwriteStrategy strategy, boolean autoParse) {
        session.lock();
        try {
            WzObject target = resolver.resolveFromRoots(session.getRoots(), targetReference, autoParse);
            if (session.getClipboard().isEmpty()) {
                throw new McpException("剪贴板为空");
            }

            List<WzObject> copied = new ArrayList<>();
            for (WzObject item : session.getClipboard()) {
                copied.add(item.deepClone(target));
            }

            if (target instanceof WzDirectory dir) {
                setPasteWzFileAndReader(copied, dir.getWzFile());
            } else if (target instanceof WzImage img) {
                setPasteWzImage(copied, img);
            } else if (target instanceof WzImageProperty prop && prop.isListProperty()) {
                setPasteWzImage(copied, prop.getWzImage());
            } else {
                throw new McpException("目标节点不支持粘贴: " + target.getClass().getSimpleName());
            }

            List<NodeSummary> pasted = new ArrayList<>();
            for (WzObject item : copied) {
                if (!handleConflict(target, item, strategy)) {
                    continue;
                }
                addChild(target, item);
                item.setTempChanged(true);
                pasted.add(NodeSummary.from(item));
            }
            return pasted;
        } finally {
            session.unlock();
        }
    }

    @Override
    public NodeSummary createChildNode(McpSessionState session, NodeReference parentReference, String type, String name, String value, Integer x, Integer y, String base64Png, String base64Mp3, String pngFormat, boolean autoParse) {
        if (name == null || name.isBlank()) throw new McpException("节点名称不能为空");
        session.lock();
        try {
            WzObject parent = resolver.resolveFromRoots(session.getRoots(), parentReference, autoParse);
            WzObject child = createNodeByType(parent, type, name, value, x, y, base64Png, base64Mp3, pngFormat);
            addChild(parent, child);
            child.setTempChanged(true);
            return NodeSummary.from(child);
        } finally {
            session.unlock();
        }
    }

    @Override
    public void deleteNode(McpSessionState session, NodeReference reference, boolean autoParse) {
        unloadNode(session, reference);
    }

    @Override
    public List<NodeSummary> searchNodeByName(McpSessionState session, NodeReference start, String keyword, boolean autoParse) {
        if (keyword == null || keyword.isBlank()) throw new McpException("keyword 不能为空");
        WzObject root = resolver.resolveFromRoots(session.getRoots(), start, autoParse);
        List<NodeSummary> result = new ArrayList<>();
        String key = keyword.toLowerCase(Locale.ROOT);
        walkByName(root, key, autoParse, result);
        return result;
    }

    @Override
    public List<Map<String, Object>> searchNodeByValue(McpSessionState session, NodeReference start, String keyword, boolean autoParse) {
        if (keyword == null || keyword.isBlank()) throw new McpException("keyword 不能为空");
        WzObject root = resolver.resolveFromRoots(session.getRoots(), start, autoParse);
        List<Map<String, Object>> result = new ArrayList<>();
        String key = keyword.toLowerCase(Locale.ROOT);
        walkByValue(root, key, autoParse, result);
        return result;
    }

    @Override
    public NodeDetail getNodeDetail(McpSessionState session, NodeReference reference, boolean autoParse) {
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        return new NodeDetail(NodeSummary.from(obj), extractValue(obj));
    }

    @Override
    public Map<String, Object> getNodeTreeJson(McpSessionState session, NodeReference reference, boolean autoParse, int maxDepth) {
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        return serializeTree(obj, autoParse, maxDepth <= 0 ? Integer.MAX_VALUE : maxDepth, 0);
    }

    @Override
    public List<Map<String, Object>> batchFindNodes(McpSessionState session, List<Map<String, Object>> queries) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (queries == null) {
            return results;
        }
        for (Map<String, Object> query : queries) {
            results.add(executeBatchFindQuery(session, query));
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> batchUpdateNodes(McpSessionState session, List<Map<String, Object>> operations) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (operations == null) {
            return results;
        }
        session.lock();
        try {
            for (Map<String, Object> operation : operations) {
                results.add(executeBatchUpdateOperation(session, operation));
            }
        } finally {
            session.unlock();
        }
        return results;
    }

    @Override
    public void saveNode(McpSessionState session, NodeReference reference, boolean autoParse) {
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        WzSavableFile file = toSavableFile(obj);
        if (file == null) throw new McpException("该节点不支持保存: " + obj.getClass().getSimpleName());
        if (!file.save()) throw new McpException("保存失败: " + file.getName());
    }

    @Override
    public void saveNodeAs(McpSessionState session, NodeReference reference, String filePath, boolean autoParse) {
        if (filePath == null || filePath.isBlank()) throw new McpException("filePath 不能为空");
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        WzSavableFile file = toSavableFile(obj);
        if (file == null) throw new McpException("该节点不支持另存为: " + obj.getClass().getSimpleName());
        file.setFilePath(filePath);
        if (!file.save()) throw new McpException("另存为失败: " + file.getName());
    }

    private List<WzObject> getChildren(WzObject parent) {
        if (parent instanceof WzFolder folder) return folder.getChildren();
        if (parent instanceof WzDirectory dir) return dir.getChildren();
        if (parent instanceof WzImage image) return new ArrayList<>(image.getChildren());
        if (parent instanceof WzImageProperty prop && prop.isListProperty()) return new ArrayList<>(prop.getChildren());
        return List.of();
    }

    private void removeFromParent(WzObject parent, WzObject child) {
        switch (parent) {
            case WzDirectory pDir when child instanceof WzDirectory ->
                    pDir.removeDirectoryChild(child.getName());
            case WzDirectory pDir when child instanceof WzImage ->
                    pDir.removeImageChild(child.getName());
            case WzImage pImg when child instanceof WzImageProperty ->
                    pImg.removeChild(child.getName());
            case WzImageProperty pProp when pProp.isListProperty() && child instanceof WzImageProperty ->
                    pProp.removeChild(child.getName());
            default -> throw new McpException("不支持的父子组合: " + parent.getClass().getSimpleName() + " -> " + child.getClass().getSimpleName());
        }
    }

    private void addChild(WzObject parent, WzObject child) {
        switch (parent) {
            case WzDirectory pDir when child instanceof WzDirectory cDir ->
                    pDir.addChild(cDir);
            case WzDirectory pDir when child instanceof WzImage cImg ->
                    pDir.addChild(cImg);
            case WzImage pImg when child instanceof WzImageProperty cProp ->
                    pImg.addChild(cProp);
            case WzImageProperty pProp when pProp.isListProperty() && child instanceof WzImageProperty cProp ->
                    pProp.addChild(cProp);
            default -> throw new McpException("不支持的父子组合: " + parent.getClass().getSimpleName() + " + " + child.getClass().getSimpleName());
        }
    }

    private boolean existsChild(WzObject parent, WzObject child) {
        return switch (parent) {
            case WzDirectory pDir when child instanceof WzDirectory -> pDir.existDirectory(child.getName());
            case WzDirectory pDir when child instanceof WzImage -> pDir.existImage(child.getName());
            case WzImage pImg -> pImg.existChild(child.getName());
            case WzImageProperty pProp when pProp.isListProperty() -> pProp.existChild(child.getName());
            default -> false;
        };
    }

    private boolean handleConflict(WzObject parent, WzObject child, OverwriteStrategy strategy) {
        if (!existsChild(parent, child)) return true;
        if (strategy == OverwriteStrategy.SKIP) {
            return false;
        }
        if (strategy == OverwriteStrategy.ERROR) {
            throw new McpException("节点已存在: " + child.getName());
        }
        removeFromParent(parent, child);
        return true;
    }

    private WzObject createNodeByType(
            WzObject parent,
            String type,
            String name,
            String value,
            Integer x,
            Integer y,
            String base64Png,
            String base64Mp3,
            String pngFormat
    ) {
        String normalized = normalizeNodeType(type);
        return switch (normalized) {
            case "WZ_DIRECTORY", "DIRECTORY" -> createDirectoryNode(parent, name);
            case "IMAGE" -> createImageNode(parent, name);
            case "IMAGE_LIST", "LIST" -> createPropertyNode(parent, new WzListProperty(name, parent, getWzImage(parent)));
            case "IMAGE_STRING", "STRING" ->
                    createPropertyNode(parent, new WzStringProperty(name, value == null ? "" : value, parent, getWzImage(parent)));
            case "IMAGE_SHORT", "SHORT" ->
                    createPropertyNode(parent, new WzShortProperty(name, parseShort(value), parent, getWzImage(parent)));
            case "IMAGE_INT", "INT" ->
                    createPropertyNode(parent, new WzIntProperty(name, parseInt(value), parent, getWzImage(parent)));
            case "IMAGE_LONG", "LONG" ->
                    createPropertyNode(parent, new WzLongProperty(name, parseLong(value), parent, getWzImage(parent)));
            case "IMAGE_FLOAT", "FLOAT" ->
                    createPropertyNode(parent, new WzFloatProperty(name, parseFloat(value), parent, getWzImage(parent)));
            case "IMAGE_DOUBLE", "DOUBLE" ->
                    createPropertyNode(parent, new WzDoubleProperty(name, parseDouble(value), parent, getWzImage(parent)));
            case "IMAGE_CANVAS", "CANVAS" -> createCanvasNode(parent, name, base64Png, pngFormat);
            case "IMAGE_CONVEX", "CONVEX" ->
                    createPropertyNode(parent, new WzConvexProperty(name, parent, getWzImage(parent)));
            case "IMAGE_VECTOR", "VECTOR" ->
                    createPropertyNode(parent, new WzVectorProperty(name, x == null ? 0 : x, y == null ? 0 : y, parent, getWzImage(parent)));
            case "IMAGE_UOL", "UOL" ->
                    createPropertyNode(parent, new WzUOLProperty(name, value == null ? "" : value, parent, getWzImage(parent)));
            case "IMAGE_SOUND", "SOUND" -> createSoundNode(parent, name, base64Mp3);
            case "IMAGE_NULL", "NULL" ->
                    createPropertyNode(parent, new WzNullProperty(name, parent, getWzImage(parent)));
            default -> throw new McpException("不支持的节点类型: " + type);
        };
    }

    private WzObject createDirectoryNode(WzObject parent, String name) {
        if (parent instanceof WzDirectory dir) {
            WzFile wzFile = dir.getWzFile();
            if (!wzFile.parse()) {
                throw new McpException("WZ 解析失败: " + wzFile.getName());
            }
            return new WzDirectory(name, dir, wzFile);
        }
        throw new McpException("该父节点不支持创建 Directory: " + parent.getClass().getSimpleName());
    }

    private WzObject createImageNode(WzObject parent, String name) {
        if (parent instanceof WzDirectory dir) {
            if (!name.endsWith(".img")) {
                name = name + ".img";
            }
            WzFile wzFile = dir.getWzFile();
            if (!wzFile.parse()) {
                throw new McpException("WZ 解析失败: " + wzFile.getName());
            }
            return new WzImage(name, dir, wzFile.getReader());
        }
        throw new McpException("该父节点不支持创建 Image: " + parent.getClass().getSimpleName());
    }

    private WzObject createCanvasNode(WzObject parent, String name, String base64Png, String pngFormat) {
        WzImage wzImage = getWzImage(parent);
        WzCanvasProperty prop = new WzCanvasProperty(name, parent, wzImage);
        prop.initPngProperty(name, prop, wzImage);
        BufferedImage image;
        if (base64Png == null || base64Png.isBlank()) {
            image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        } else {
            image = decodeBase64Png(base64Png);
        }
        WzPngFormat format = pngFormat == null || pngFormat.isBlank() ? WzPngFormat.ARGB8888 : parsePngFormat(pngFormat);
        prop.setPng(image, format, 0);
        return createPropertyNode(parent, prop);
    }

    private WzObject createSoundNode(WzObject parent, String name, String base64Mp3) {
        if (base64Mp3 == null || base64Mp3.isBlank()) {
            throw new McpException("创建 SOUND 节点必须提供 base64Mp3");
        }
        WzSoundProperty prop = new WzSoundProperty(name, parent, getWzImage(parent));
        prop.setSound(Base64.getDecoder().decode(base64Mp3));
        return createPropertyNode(parent, prop);
    }

    private WzObject createPropertyNode(WzObject parent, WzImageProperty node) {
        if (parent instanceof WzImage img) {
            if (!img.parse()) {
                throw new McpException("IMG 解析失败: " + img.getName());
            }
            return node;
        }
        if (parent instanceof WzImageProperty prop && prop.isListProperty()) {
            return node;
        }
        throw new McpException("该父节点不支持创建属性节点: " + parent.getClass().getSimpleName());
    }

    private void walkByName(WzObject current, String keyword, boolean autoParse, List<NodeSummary> collector) {
        if (current.getName().toLowerCase(Locale.ROOT).contains(keyword)) {
            collector.add(NodeSummary.from(current));
        }
        for (WzObject child : getChildrenForSearch(current, autoParse)) {
            walkByName(child, keyword, autoParse, collector);
        }
    }

    private void walkByValue(WzObject current, String keyword, boolean autoParse, List<Map<String, Object>> collector) {
        Map<String, Object> match = buildValueMatch(current, keyword);
        if (match != null) {
            collector.add(match);
        }
        for (WzObject child : getChildrenForSearch(current, autoParse)) {
            walkByValue(child, keyword, autoParse, collector);
        }
    }

    private Map<String, Object> buildValueMatch(WzObject obj, String keyword) {
        Map<String, Object> match = new HashMap<>();
        match.put("name", obj.getName());
        match.put("rootPath", NodePathResolver.rootPathOf(obj));
        match.put("nodePath", NodePathResolver.nodePathOf(obj));
        match.put("type", obj.getType().name());
        match.put("matchedIn", "value");

        switch (obj) {
            case WzStringProperty p -> {
                if (!containsIgnoreCase(p.getValue(), keyword)) return null;
                match.put("value", p.getValue());
            }
            case WzIntProperty p -> {
                if (!containsIgnoreCase(String.valueOf(p.getValue()), keyword)) return null;
                match.put("value", p.getValue());
            }
            case WzShortProperty p -> {
                if (!containsIgnoreCase(String.valueOf(p.getValue()), keyword)) return null;
                match.put("value", p.getValue());
            }
            case WzLongProperty p -> {
                if (!containsIgnoreCase(String.valueOf(p.getValue()), keyword)) return null;
                match.put("value", p.getValue());
            }
            case WzFloatProperty p -> {
                if (!containsIgnoreCase(String.valueOf(p.getValue()), keyword)) return null;
                match.put("value", p.getValue());
            }
            case WzDoubleProperty p -> {
                if (!containsIgnoreCase(String.valueOf(p.getValue()), keyword)) return null;
                match.put("value", p.getValue());
            }
            case WzUOLProperty p -> {
                if (!containsIgnoreCase(p.getValue(), keyword)) return null;
                match.put("value", p.getValue());
            }
            case WzVectorProperty p -> {
                String vectorText = p.getX() + "," + p.getY();
                if (!(containsIgnoreCase(vectorText, keyword)
                        || containsIgnoreCase(String.valueOf(p.getX()), keyword)
                        || containsIgnoreCase(String.valueOf(p.getY()), keyword))) {
                    return null;
                }
                match.put("x", p.getX());
                match.put("y", p.getY());
                match.put("value", vectorText);
            }
            default -> {
                return null;
            }
        }
        return match;
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private List<WzObject> getChildrenForSearch(WzObject current, boolean autoParse) {
        if (current instanceof WzDirectory dir) {
            if (dir.isWzFile() && autoParse && !dir.getWzFile().parse()) {
                throw new McpException("WZ 文件解析失败: " + dir.getName());
            }
            return new ArrayList<>(dir.getChildren());
        }
        if (current instanceof WzImage image) {
            if (autoParse && !image.parse()) {
                throw new McpException("IMG 解析失败: " + image.getName());
            }
            return new ArrayList<>(image.getChildren());
        }
        if (current instanceof WzImageProperty prop && prop.isListProperty()) {
            return new ArrayList<>(prop.getChildren());
        }
        if (current instanceof WzFolder folder) {
            return new ArrayList<>(folder.getChildren());
        }
        return List.of();
    }

    private Map<String, Object> extractValue(WzObject obj) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", obj.getName());
        result.put("rootPath", NodePathResolver.rootPathOf(obj));
        result.put("nodePath", NodePathResolver.nodePathOf(obj));
        result.put("type", obj.getType().name());

        switch (obj) {
            case WzStringProperty p -> result.put("value", p.getValue());
            case WzIntProperty p -> result.put("value", p.getValue());
            case WzShortProperty p -> result.put("value", p.getValue());
            case WzLongProperty p -> result.put("value", p.getValue());
            case WzFloatProperty p -> result.put("value", p.getValue());
            case WzDoubleProperty p -> result.put("value", p.getValue());
            case WzVectorProperty p -> {
                result.put("x", p.getX());
                result.put("y", p.getY());
            }
            case WzUOLProperty p -> result.put("value", p.getValue());
            case WzSoundProperty p -> {
                result.put("lenMs", p.getLenMs());
                result.put("mp3", Base64.getEncoder().encodeToString(p.getSoundBytes()));
            }
            case WzCanvasProperty p -> {
                result.put("width", p.getWidth());
                result.put("height", p.getHeight());
                result.put("pngFormat", p.getFormat().name());
                result.put("png", Base64.getEncoder().encodeToString(p.getImageBytes(false)));
            }
            default -> {
            }
        }
        return result;
    }

    private Map<String, Object> serializeTree(WzObject obj, boolean autoParse, int maxDepth, int currentDepth) {
        Map<String, Object> result = new HashMap<>(extractValue(obj));
        result.put("children", List.of());
        if (currentDepth >= maxDepth) {
            return result;
        }

        List<WzObject> children = getChildrenForTree(obj, autoParse);
        if (children.isEmpty()) {
            return result;
        }

        List<Map<String, Object>> serializedChildren = new ArrayList<>();
        for (WzObject child : children) {
            serializedChildren.add(serializeTree(child, autoParse, maxDepth, currentDepth + 1));
        }
        result.put("children", serializedChildren);
        return result;
    }

    private List<WzObject> getChildrenForTree(WzObject obj, boolean autoParse) {
        if (obj instanceof WzDirectory dir) {
            if (dir.isWzFile() && autoParse && !dir.getWzFile().parse()) {
                throw new McpException("WZ 文件解析失败: " + dir.getName());
            }
            return new ArrayList<>(dir.getChildren());
        }
        if (obj instanceof WzImage image) {
            if (autoParse && !image.parse()) {
                throw new McpException("IMG 解析失败: " + image.getName());
            }
            return new ArrayList<>(image.getChildren());
        }
        if (obj instanceof WzImageProperty prop && prop.isListProperty()) {
            return new ArrayList<>(prop.getChildren());
        }
        if (obj instanceof WzFolder folder) {
            return new ArrayList<>(folder.getChildren());
        }
        return List.of();
    }

    private Map<String, Object> executeBatchUpdateOperation(McpSessionState session, Map<String, Object> operation) {
        String op = normalizeUpdateOp(optionalString(operation.get("op")), operation);
        boolean autoParse = booleanValue(operation.get("autoParse"), true);
        return switch (op) {
            case "create_child" -> executeCreateChildOperation(session, operation, autoParse, op);
            case "delete" -> executeDeleteOperation(session, operation, autoParse, op);
            case "save" -> executeSaveOperation(session, operation, autoParse, op);
            case "save_as" -> executeSaveAsOperation(session, operation, autoParse, op);
            default -> executeValueUpdateOperation(session, operation, autoParse);
        };
    }

    private Map<String, Object> executeCreateChildOperation(McpSessionState session, Map<String, Object> operation, boolean autoParse, String op) {
        NodeReference parentReference = nodeReference(operation);
        NodeSummary child = createChildNode(
                session,
                parentReference,
                stringValue(operation.get("type")),
                stringValue(operation.get("name")),
                optionalString(operation.get("value")),
                integerValue(operation.get("x")),
                integerValue(operation.get("y")),
                optionalString(operation.get("base64Png")),
                optionalString(operation.get("base64Mp3")),
                optionalString(operation.get("pngFormat")),
                autoParse
        );
        return Map.of(
                "rootPath", parentReference.rootPath(),
                "nodePath", parentReference.nodePath(),
                "node", child,
                "op", op,
                "created", true
        );
    }

    private Map<String, Object> executeDeleteOperation(McpSessionState session, Map<String, Object> operation, boolean autoParse, String op) {
        NodeReference reference = nodeReference(operation);
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        NodeSummary deletedNode = NodeSummary.from(obj);
        if (obj.getParent() == null) {
            session.getRoots().remove(obj);
        } else {
            removeFromParent(obj.getParent(), obj);
        }
        return Map.of(
                "rootPath", deletedNode.rootPath(),
                "nodePath", deletedNode.nodePath(),
                "node", deletedNode,
                "op", op,
                "deleted", true
        );
    }

    private Map<String, Object> executeSaveOperation(McpSessionState session, Map<String, Object> operation, boolean autoParse, String op) {
        NodeReference reference = nodeReference(operation);
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        WzSavableFile file = toSavableFile(obj);
        if (file == null) throw new McpException("该节点不支持保存: " + obj.getClass().getSimpleName());
        if (!file.save()) throw new McpException("保存失败: " + file.getName());
        return Map.of(
                "rootPath", NodePathResolver.rootPathOf(obj),
                "nodePath", NodePathResolver.nodePathOf(obj),
                "node", NodeSummary.from(obj),
                "op", op,
                "saved", true
        );
    }

    private Map<String, Object> executeSaveAsOperation(McpSessionState session, Map<String, Object> operation, boolean autoParse, String op) {
        NodeReference reference = nodeReference(operation);
        String filePath = stringValue(operation.get("filePath"));
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        WzSavableFile file = toSavableFile(obj);
        if (file == null) throw new McpException("该节点不支持另存为: " + obj.getClass().getSimpleName());
        file.setFilePath(filePath);
        if (!file.save()) throw new McpException("另存为失败: " + file.getName());
        return Map.of(
                "rootPath", NodePathResolver.rootPathOf(obj),
                "nodePath", NodePathResolver.nodePathOf(obj),
                "node", NodeSummary.from(obj),
                "op", op,
                "filePath", filePath,
                "saved", true
        );
    }

    private Map<String, Object> executeValueUpdateOperation(McpSessionState session, Map<String, Object> operation, boolean autoParse) {
        NodeReference reference = nodeReference(operation);
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        String op = updateNode(obj, operation);
        return Map.of(
                "rootPath", NodePathResolver.rootPathOf(obj),
                "nodePath", NodePathResolver.nodePathOf(obj),
                "node", NodeSummary.from(obj),
                "op", op,
                "updated", true
        );
    }

    private String updateNode(WzObject obj, Map<String, Object> operation) {
        String op = normalizeUpdateOp(optionalString(operation.get("op")), operation);
        return switch (op) {
            case "rename" -> {
                applyRename(obj, stringValue(operation.get("name")));
                yield op;
            }
            case "set_value" -> {
                applySetValue(obj, operation.get("value"));
                yield op;
            }
            case "set_vector" -> {
                applySetVector(obj, operation.get("x"), operation.get("y"));
                yield op;
            }
            case "set_png" -> {
                applySetPng(obj, stringValue(operation.get("base64Png")), optionalString(operation.get("pngFormat")));
                yield op;
            }
            case "set_sound" -> {
                applySetSound(obj, stringValue(operation.get("base64Mp3")));
                yield op;
            }
            case "legacy" -> {
                applyLegacyUpdate(obj, operation);
                yield op;
            }
            default -> throw new McpException("不支持的批量修改操作: " + op);
        };
    }

    private void applyLegacyUpdate(WzObject obj, Map<String, Object> operation) {
        String newName = optionalString(operation.get("name"));
        String value = optionalString(operation.get("value"));
        Integer x = integerValue(operation.get("x"));
        Integer y = integerValue(operation.get("y"));
        String base64Png = optionalString(operation.get("base64Png"));
        String base64Mp3 = optionalString(operation.get("base64Mp3"));
        String pngFormat = optionalString(operation.get("pngFormat"));

        switch (obj) {
            case WzDirectory dir -> {
                if (newName != null && !newName.equals(dir.getName())) {
                    if (dir.isWzFile()) {
                        if (!newName.endsWith(".wz")) {
                            throw new McpException("WZ 文件名必须以 .wz 结尾");
                        }
                        dir.setNameAnyway(newName);
                        dir.getWzFile().setNameAnyway(newName);
                    } else if (!dir.setName(newName)) {
                        throw new McpException("存在同名目录，保存失败");
                    }
                    dir.setTempChanged(true);
                }
            }
            case WzImage image -> {
                if (newName != null && !newName.equals(image.getName())) {
                    if (!newName.endsWith(".img")) {
                        throw new McpException("IMG 文件名必须以 .img 结尾");
                    }
                    if (!image.setName(newName)) {
                        throw new McpException("存在同名 image，保存失败");
                    }
                    image.setTempChanged(true);
                    image.setChanged(true);
                }
            }
            case WzCanvasProperty prop -> {
                renameProperty(prop, newName);
                if (base64Png != null) {
                    BufferedImage image = decodeBase64Png(base64Png);
                    WzPngFormat format = pngFormat == null || pngFormat.isBlank() ? prop.getFormat() : parsePngFormat(pngFormat);
                    prop.setPng(image, format, prop.getScale());
                    markChanged(prop);
                }
            }
            case WzConvexProperty prop -> {
                renameProperty(prop, newName);
                markChanged(prop);
            }
            case WzDoubleProperty prop -> {
                renameProperty(prop, newName);
                if (value != null) prop.setValue(parseDouble(value));
                markChanged(prop);
            }
            case WzFloatProperty prop -> {
                renameProperty(prop, newName);
                if (value != null) prop.setValue(parseFloat(value));
                markChanged(prop);
            }
            case WzIntProperty prop -> {
                renameProperty(prop, newName);
                if (value != null) prop.setValue(parseInt(value));
                markChanged(prop);
            }
            case WzListProperty prop -> {
                renameProperty(prop, newName);
                markChanged(prop);
            }
            case WzLongProperty prop -> {
                renameProperty(prop, newName);
                if (value != null) prop.setValue(parseLong(value));
                markChanged(prop);
            }
            case WzNullProperty prop -> {
                renameProperty(prop, newName);
                markChanged(prop);
            }
            case WzShortProperty prop -> {
                renameProperty(prop, newName);
                if (value != null) prop.setValue(parseShort(value));
                markChanged(prop);
            }
            case WzSoundProperty prop -> {
                renameProperty(prop, newName);
                if (base64Mp3 != null) prop.setSound(Base64.getDecoder().decode(base64Mp3));
                markChanged(prop);
            }
            case WzStringProperty prop -> {
                renameProperty(prop, newName);
                if (value != null) prop.setValue(value);
                markChanged(prop);
            }
            case WzUOLProperty prop -> {
                renameProperty(prop, newName);
                if (value != null) prop.setValue(value);
                markChanged(prop);
            }
            case WzVectorProperty prop -> {
                renameProperty(prop, newName);
                if (x != null) prop.setX(x);
                if (y != null) prop.setY(y);
                markChanged(prop);
            }
            case WzLuaProperty prop -> {
                renameProperty(prop, newName);
                if (value != null) prop.setString(value);
                markChanged(prop);
            }
            default -> throw new McpException("该节点类型暂不支持批量修改: " + obj.getClass().getSimpleName());
        }
    }

    private void applyRename(WzObject obj, String newName) {
        switch (obj) {
            case WzDirectory dir -> {
                if (newName.equals(dir.getName())) {
                    return;
                }
                if (dir.isWzFile()) {
                    if (!newName.endsWith(".wz")) {
                        throw new McpException("WZ 文件名必须以 .wz 结尾");
                    }
                    dir.setNameAnyway(newName);
                    dir.getWzFile().setNameAnyway(newName);
                } else if (!dir.setName(newName)) {
                    throw new McpException("存在同名目录，保存失败");
                }
                dir.setTempChanged(true);
            }
            case WzImage image -> {
                if (newName.equals(image.getName())) {
                    return;
                }
                if (!newName.endsWith(".img")) {
                    throw new McpException("IMG 文件名必须以 .img 结尾");
                }
                if (!image.setName(newName)) {
                    throw new McpException("存在同名 image，保存失败");
                }
                image.setTempChanged(true);
                image.setChanged(true);
            }
            case WzImageProperty prop -> renameProperty(prop, newName);
            default -> throw new McpException("该节点类型不支持 rename: " + obj.getClass().getSimpleName());
        }
    }

    private void applySetValue(WzObject obj, Object rawValue) {
        String value = rawValue == null ? null : String.valueOf(rawValue);
        switch (obj) {
            case WzDoubleProperty prop -> {
                prop.setValue(parseDouble(stringValue(value)));
                markChanged(prop);
            }
            case WzFloatProperty prop -> {
                prop.setValue(parseFloat(stringValue(value)));
                markChanged(prop);
            }
            case WzIntProperty prop -> {
                prop.setValue(parseInt(stringValue(value)));
                markChanged(prop);
            }
            case WzLongProperty prop -> {
                prop.setValue(parseLong(stringValue(value)));
                markChanged(prop);
            }
            case WzShortProperty prop -> {
                prop.setValue(parseShort(stringValue(value)));
                markChanged(prop);
            }
            case WzStringProperty prop -> {
                prop.setValue(value == null ? "" : value);
                markChanged(prop);
            }
            case WzUOLProperty prop -> {
                prop.setValue(value == null ? "" : value);
                markChanged(prop);
            }
            case WzLuaProperty prop -> {
                prop.setString(value == null ? "" : value);
                markChanged(prop);
            }
            default -> throw new McpException("该节点类型不支持 set_value: " + obj.getClass().getSimpleName());
        }
    }

    private void applySetVector(WzObject obj, Object rawX, Object rawY) {
        if (!(obj instanceof WzVectorProperty prop)) {
            throw new McpException("该节点类型不支持 set_vector: " + obj.getClass().getSimpleName());
        }
        Integer x = integerValue(rawX);
        Integer y = integerValue(rawY);
        if (x == null && y == null) {
            throw new McpException("set_vector 至少需要 x 或 y");
        }
        if (x != null) {
            prop.setX(x);
        }
        if (y != null) {
            prop.setY(y);
        }
        markChanged(prop);
    }

    private void applySetPng(WzObject obj, String base64Png, String pngFormat) {
        if (!(obj instanceof WzCanvasProperty prop)) {
            throw new McpException("该节点类型不支持 set_png: " + obj.getClass().getSimpleName());
        }
        BufferedImage image = decodeBase64Png(base64Png);
        WzPngFormat format = pngFormat == null || pngFormat.isBlank() ? prop.getFormat() : parsePngFormat(pngFormat);
        prop.setPng(image, format, prop.getScale());
        markChanged(prop);
    }

    private void applySetSound(WzObject obj, String base64Mp3) {
        if (!(obj instanceof WzSoundProperty prop)) {
            throw new McpException("该节点类型不支持 set_sound: " + obj.getClass().getSimpleName());
        }
        prop.setSound(Base64.getDecoder().decode(base64Mp3));
        markChanged(prop);
    }

    private String normalizeUpdateOp(String op, Map<String, Object> operation) {
        if (op != null && !op.isBlank()) {
            return switch (op.trim().toLowerCase(Locale.ROOT)) {
                case "create", "create_child", "add", "add_child" -> "create_child";
                case "delete", "delete_node", "remove", "remove_node" -> "delete";
                case "rename", "set_name" -> "rename";
                case "set_value", "value" -> "set_value";
                case "set_vector", "vector" -> "set_vector";
                case "set_png", "png" -> "set_png";
                case "set_sound", "sound" -> "set_sound";
                case "save", "save_node" -> "save";
                case "save_as" -> "save_as";
                default -> throw new McpException("未知的批量修改 op: " + op);
            };
        }
        if (operation.containsKey("base64Png")) {
            return "legacy";
        }
        if (operation.containsKey("base64Mp3")) {
            return "legacy";
        }
        if (operation.containsKey("x") || operation.containsKey("y")) {
            return "legacy";
        }
        if (operation.containsKey("value")) {
            return "legacy";
        }
        if (operation.containsKey("name")) {
            return "legacy";
        }
        throw new McpException("batch_update_nodes 缺少 op");
    }

    private Map<String, Object> executeBatchFindQuery(McpSessionState session, Map<String, Object> query) {
        String op = normalizeFindOp(optionalString(query.get("op")), query);
        boolean autoParse = booleanValue(query.get("autoParse"), true);
        return switch (op) {
            case "find_by_path" -> executeFindByPath(session, query, autoParse, op);
            case "list_children" -> executeListChildren(session, query, autoParse, op);
            case "get_detail" -> executeGetDetail(session, query, autoParse, op);
            case "get_tree" -> executeGetTree(session, query, autoParse, op);
            case "search_by_keyword" -> executeSearchByKeyword(session, query, autoParse, op);
            case "search_by_value" -> executeSearchByValue(session, query, autoParse, op);
            case "match_type" -> executeMatchType(session, query, autoParse, op);
            default -> throw new McpException("不支持的批量查询 op: " + op);
        };
    }

    private Map<String, Object> executeFindByPath(McpSessionState session, Map<String, Object> query, boolean autoParse, String op) {
        NodeReference reference = nodeReference(query);
        WzObject obj = resolver.resolveFromRoots(session.getRoots(), reference, autoParse);
        Map<String, Object> result = new HashMap<>();
        result.put("op", op);
        result.put("rootPath", reference.rootPath());
        result.put("nodePath", reference.nodePath());
        result.put("matches", List.of(NodeSummary.from(obj)));
        if (booleanValue(query.get("includeTree"), false)) {
            int maxDepth = integerValue(query.get("maxDepth")) == null ? 0 : integerValue(query.get("maxDepth"));
            result.put("tree", serializeTree(obj, autoParse, maxDepth <= 0 ? Integer.MAX_VALUE : maxDepth, 0));
        }
        return result;
    }

    private Map<String, Object> executeListChildren(McpSessionState session, Map<String, Object> query, boolean autoParse, String op) {
        NodeReference reference = nodeReference(query);
        return Map.of(
                "op", op,
                "rootPath", reference.rootPath(),
                "nodePath", reference.nodePath(),
                "children", listChildren(session, reference, autoParse)
        );
    }

    private Map<String, Object> executeGetDetail(McpSessionState session, Map<String, Object> query, boolean autoParse, String op) {
        NodeReference reference = nodeReference(query);
        return Map.of(
                "op", op,
                "rootPath", reference.rootPath(),
                "nodePath", reference.nodePath(),
                "detail", getNodeDetail(session, reference, autoParse)
        );
    }

    private Map<String, Object> executeGetTree(McpSessionState session, Map<String, Object> query, boolean autoParse, String op) {
        NodeReference reference = nodeReference(query);
        int maxDepth = integerValue(query.get("maxDepth")) == null ? 0 : integerValue(query.get("maxDepth"));
        return Map.of(
                "op", op,
                "rootPath", reference.rootPath(),
                "nodePath", reference.nodePath(),
                "tree", getNodeTreeJson(session, reference, autoParse, maxDepth)
        );
    }

    private Map<String, Object> executeSearchByKeyword(McpSessionState session, Map<String, Object> query, boolean autoParse, String op) {
        NodeReference start = nodeReference(query);
        String keyword = stringValue(query.get("keyword"));
        List<NodeSummary> matches = searchNodeByName(session, start, keyword, autoParse);
        Map<String, Object> result = new HashMap<>();
        result.put("op", op);
        result.put("rootPath", start.rootPath());
        result.put("nodePath", start.nodePath());
        result.put("keyword", keyword);
        result.put("matches", matches);
        if (booleanValue(query.get("includeTree"), false) && !matches.isEmpty()) {
            int maxDepth = integerValue(query.get("maxDepth")) == null ? 0 : integerValue(query.get("maxDepth"));
            List<Map<String, Object>> trees = new ArrayList<>();
            for (NodeSummary match : matches) {
                WzObject obj = resolver.resolveFromRoots(session.getRoots(), new NodeReference(match.rootPath(), match.nodePath()), autoParse);
                trees.add(serializeTree(obj, autoParse, maxDepth <= 0 ? Integer.MAX_VALUE : maxDepth, 0));
            }
            result.put("trees", trees);
        }
        return result;
    }

    private Map<String, Object> executeSearchByValue(McpSessionState session, Map<String, Object> query, boolean autoParse, String op) {
        NodeReference start = nodeReference(query);
        String keyword = stringValue(query.get("keyword"));
        List<Map<String, Object>> matches = searchNodeByValue(session, start, keyword, autoParse);
        Map<String, Object> result = new HashMap<>();
        result.put("op", op);
        result.put("rootPath", start.rootPath());
        result.put("nodePath", start.nodePath());
        result.put("keyword", keyword);
        result.put("matches", matches);
        if (booleanValue(query.get("includeTree"), false) && !matches.isEmpty()) {
            int maxDepth = integerValue(query.get("maxDepth")) == null ? 0 : integerValue(query.get("maxDepth"));
            List<Map<String, Object>> trees = new ArrayList<>();
            for (Map<String, Object> match : matches) {
                Object rootPath = match.get("rootPath");
                if (!(rootPath instanceof String matchRootPath)) {
                    continue;
                }
                String matchNodePath = optionalString(match.get("nodePath"));
                WzObject obj = resolver.resolveFromRoots(session.getRoots(), new NodeReference(matchRootPath, matchNodePath), autoParse);
                trees.add(serializeTree(obj, autoParse, maxDepth <= 0 ? Integer.MAX_VALUE : maxDepth, 0));
            }
            result.put("trees", trees);
        }
        return result;
    }

    private Map<String, Object> executeMatchType(McpSessionState session, Map<String, Object> query, boolean autoParse, String op) {
        NodeReference start = nodeReference(query);
        String type = stringValue(query.get("type")).toUpperCase(Locale.ROOT);
        WzObject root = resolver.resolveFromRoots(session.getRoots(), start, autoParse);
        List<NodeSummary> matches = new ArrayList<>();
        walkByType(root, type, autoParse, matches);
        return Map.of(
                "op", op,
                "rootPath", start.rootPath(),
                "nodePath", start.nodePath(),
                "type", type,
                "matches", matches
        );
    }

    private void walkByType(WzObject node, String type, boolean autoParse, List<NodeSummary> result) {
        if (node.getType().name().equalsIgnoreCase(type)) {
            result.add(NodeSummary.from(node));
        }
        for (WzObject child : getChildrenForSearch(node, autoParse)) {
            walkByType(child, type, autoParse, result);
        }
    }

    private String normalizeFindOp(String op, Map<String, Object> query) {
        if (op != null && !op.isBlank()) {
            return switch (op.trim().toLowerCase(Locale.ROOT)) {
                case "find_by_path" -> "find_by_path";
                case "list_children", "children" -> "list_children";
                case "get_detail", "detail" -> "get_detail";
                case "get_tree", "tree", "get_node_tree_json" -> "get_tree";
                case "search_by_keyword", "keyword", "search" -> "search_by_keyword";
                case "search_by_value", "value" -> "search_by_value";
                case "match_type", "type" -> "match_type";
                default -> throw new McpException("未知的批量查询 op: " + op);
            };
        }
        if (query.containsKey("keyword")) {
            Object searchIn = query.get("searchIn");
            if (searchIn != null && "value".equalsIgnoreCase(String.valueOf(searchIn))) {
                return "search_by_value";
            }
            return "search_by_keyword";
        }
        if (query.containsKey("type")) {
            return "match_type";
        }
        if (query.containsKey("rootPath")) {
            return "find_by_path";
        }
        throw new McpException("batch_find_nodes 缺少 op");
    }

    private NodeReference nodeReference(Map<String, Object> source) {
        return new NodeReference(stringValue(source.get("rootPath")), optionalString(source.get("nodePath")));
    }

    private void renameProperty(WzImageProperty prop, String newName) {
        if (newName == null || newName.equals(prop.getName())) {
            return;
        }
        if (!prop.setName(newName)) {
            throw new McpException("存在同名节点，保存失败: " + newName);
        }
        markChanged(prop);
    }

    private void markChanged(WzImageProperty prop) {
        prop.setTempChanged(true);
        if (prop.getWzImage() != null) {
            prop.getWzImage().setChanged(true);
            prop.getWzImage().setTempChanged(true);
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            throw new McpException("缺少必要参数");
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            throw new McpException("参数不能为空");
        }
        return text;
    }

    private String optionalString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer integerValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new McpException("整数参数无效: " + value, e);
        }
    }

    private WzSavableFile toSavableFile(WzObject obj) {
        if (obj instanceof WzDirectory dir && dir.isWzFile()) {
            return dir.getWzFile();
        }
        if (obj instanceof WzSavableFile file) {
            return file;
        }
        return null;
    }

    private WzImage getWzImage(WzObject parent) {
        if (parent instanceof WzImage image) return image;
        if (parent instanceof WzImageProperty prop) return prop.getWzImage();
        throw new McpException("该父节点不是 Image 或 List: " + parent.getClass().getSimpleName());
    }

    private String normalizeNodeType(String type) {
        if (type == null || type.isBlank()) {
            throw new McpException("type 不能为空");
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private short parseShort(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Short.parseShort(value.trim());
        } catch (NumberFormatException e) {
            throw new McpException("short 值无效: " + value, e);
        }
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new McpException("int 值无效: " + value, e);
        }
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new McpException("long 值无效: " + value, e);
        }
    }

    private float parseFloat(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new McpException("float 值无效: " + value, e);
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new McpException("double 值无效: " + value, e);
        }
    }

    private WzPngFormat parsePngFormat(String format) {
        String f = format.trim();
        if (f.toUpperCase(Locale.ROOT).startsWith("FORMAT")) {
            f = f.substring("FORMAT".length());
        }
        try {
            return WzPngFormat.getByValue(Integer.parseInt(f));
        } catch (NumberFormatException e) {
            try {
                return WzPngFormat.valueOf(format.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new McpException("pngFormat 无效: " + format);
            }
        }
    }

    private BufferedImage decodeBase64Png(String base64) {
        String pure = base64;
        int index = base64.indexOf(",");
        if (index > 0) {
            pure = base64.substring(index + 1);
        }
        byte[] bytes = Base64.getDecoder().decode(pure);
        try {
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new McpException("png base64 解码失败", e);
        }
    }

    private void setPasteWzFileAndReader(List<WzObject> items, WzFile wzFile) {
        for (WzObject item : items) {
            if (item instanceof WzDirectory dir) {
                dir.setWzFile(wzFile);
                setPasteWzFileAndReader(dir.getChildren(), wzFile);
            } else if (item instanceof WzImage img) {
                img.setReader(wzFile.getReader());
                setPasteWzImage(img.getChildren(), img);
            } else {
                throw new McpException("无法设置 WzFile: " + item.getClass().getSimpleName());
            }
        }
    }

    private void setPasteWzImage(List<? extends WzObject> items, WzImage image) {
        for (WzObject item : items) {
            if (item instanceof WzImageProperty prop) {
                prop.setWzImage(image);
                prop.setChildrenWzImage(image);
            } else {
                throw new McpException("无法设置 WzImage: " + item.getClass().getSimpleName());
            }
        }
    }
}
