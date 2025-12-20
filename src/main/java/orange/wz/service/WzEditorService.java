package orange.wz.service;

import lombok.extern.slf4j.Slf4j;
import orange.wz.config.ServerConfig;
import orange.wz.exception.BizException;
import orange.wz.exception.ExceptionEnum;
import orange.wz.model.Pair;
import orange.wz.model.WzNode;
import orange.wz.model.WzNodeType;
import orange.wz.model.WzNodeValueDto;
import orange.wz.provider.*;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.WzMutableKey;
import orange.wz.utils.FileUtils;
import orange.wz.utils.wzkey.WzKey;
import orange.wz.utils.wzkey.WzKeyStorage;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public final class WzEditorService {
    private final WzKeyStorage wzKeyRepository = new WzKeyStorage();

    private final static Map<Integer, WzNode> folderCache = new HashMap<>();
    private final static Set<Integer> views = new HashSet<>();
    private final static Map<Integer, WzNode> nodeCache = new HashMap<>();
    private final static AtomicInteger nextId = new AtomicInteger(0);
    private final static List<WzFile> cavWzFiles = new ArrayList<>();
    private static String lastCavPath = null;
    private final static List<WzObject> clipboard = new ArrayList<>();

    public String gc(boolean deleteCache) {
        Runtime rt = Runtime.getRuntime();
        long beforeUsed = rt.totalMemory() - rt.freeMemory();

        if (deleteCache) {
            folderCache.clear();
            views.clear();
            nodeCache.clear();
            nextId.set(0);
            cavWzFiles.clear();
            lastCavPath = null;
            clipboard.clear();
        }

        System.gc();

        // GC 后的内存情况（稍微停一下让 GC 有时间运行）
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        long afterUsed = rt.totalMemory() - rt.freeMemory();
        long reclaimed = beforeUsed - afterUsed;
        if (reclaimed < 0) reclaimed = 0; // 避免出现负数

        String result = "缓存已清空，回收内存约: " + (reclaimed / 1024 / 1024) + " MB";
        log.info(result);
        return result;
    }

    /* 视图 -----------------------------------------------------------------------------------------------------------*/
    public Set<Integer> getViews() {
        if (views.isEmpty()) {
            WzNode wzNode = new WzNode(nextId.getAndIncrement(), "root", WzNodeType.FOLDER, null, true);
            views.add(wzNode.getId());
            nodeCache.put(wzNode.getId(), wzNode);
        }

        return views;
    }

    public Integer addView() {
        WzNode wzNode = new WzNode(nextId.getAndIncrement(), "root", WzNodeType.FOLDER, null, true);
        views.add(wzNode.getId());
        nodeCache.put(wzNode.getId(), wzNode);

        return wzNode.getId();
    }

    public void moveView(int node, int view) {
        if (views.contains(node)) throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "操作对象是视图");
        WzNode wzNode = nodeCache.get(node);
        if (wzNode == null) throw new BizException(ExceptionEnum.NOT_FOUND);
        if (!views.contains(view)) throw new BizException(ExceptionEnum.NOT_FOUND);
        WzNode viewNode = nodeCache.get(view);
        WzNode parentNode = wzNode.getParent();
        if (parentNode.unlinkChild(node)) {
            wzNode.setParent(viewNode);
            viewNode.addChild(wzNode);
        }
    }

    public void removeView(int view) {
        if (!views.contains(view)) throw new BizException(ExceptionEnum.NOT_FOUND);
        unload(view, true);
        views.remove(view);
    }

    /* 查看Wz资源 ------------------------------------------------------------------------------------------------------*/
    public List<WzNode> getFolder(int id) {
        if (folderCache.isEmpty()) {
            WzNode wzFolder = new WzNode(-1, "wzFolder", WzNodeType.FOLDER, Path.of(ServerConfig.WZ_DIRECTORY), false);
            folderCache.put(wzFolder.getId(), wzFolder);
        }

        WzNode folder = folderCache.get(id);
        if (folder == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        if (folder.getType() == WzNodeType.FOLDER && folder.getChildren().isEmpty()) {
            loadFolder(folder, folderCache);
        }

        return folder.getChildren();
    }

    public void reloadFolder() {
        folderCache.clear();
    }

    private void loadFolder(WzNode folder, Map<Integer, WzNode> cache) {
        Path folderPath = folder.getPath();
        if (folderPath == null) return;
        log.debug("加载目录: {}", folderPath);
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new BizException(ExceptionEnum.NOT_FOUND_FILE_OR_FOLDER);
        }
        try (var paths = Files.list(folderPath)) {
            List<WzNode> children = new ArrayList<>();
            paths.forEach(path -> {
                WzNodeType type;
                String filename = path.getFileName().toString();
                if (Files.isDirectory(path)) {
                    type = WzNodeType.FOLDER;
                } else if (filename.endsWith(".wz")) {
                    type = WzNodeType.WZ;
                } else if (filename.endsWith(".img")) {
                    type = WzNodeType.IMAGE;
                } else {
                    return;
                }
                WzNode child = new WzNode(folder, nextId.getAndIncrement(), filename, type, path);
                children.add(child);
                cache.put(child.getId(), child);
            });
            folder.addChildren(children);
        } catch (IOException e) {
            log.error("读取目录时出错: {}", e.getMessage());
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR);
        }
    }

    /* 加载文件 --------------------------------------------------------------------------------------------------------*/
    public void load(int folderId, int viewId, Short gameVersion, String key) {
        if (gameVersion == null || key == null) throw new BizException(ExceptionEnum.WZ_FILE_NO_VERSION_OR_KEY);

        WzNode view = nodeCache.get(viewId);
        if (view == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        WzNode node = folderCache.get(folderId);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND_FILE_OR_FOLDER);
        if (nodeCache.get(folderId) != null) throw new BizException(ExceptionEnum.IS_OPENED);

        WzKey wzKey = wzKeyRepository.findByName(key);
        if (wzKey == null) throw new BizException(ExceptionEnum.NOT_FOUND_WZ_KEY);

        if (node.getType() == WzNodeType.FOLDER) {
            WzNode folder = new WzNode(view, nextId.getAndIncrement(), node.getName(), WzNodeType.FOLDER, node.getPath());
            view.addChild(folder);
            nodeCache.put(folder.getId(), folder);
            loadSub(folder, gameVersion, wzKey);
        } else if (node.getType() == WzNodeType.WZ) {
            WzFile wzFile = new WzFile(node.getPath().toString(), gameVersion, wzKey.getIv(), wzKey.getUserKey());
            WzNode file = new WzNode(view, nextId.getAndIncrement(), node.getName(), WzNodeType.WZ, node.getPath(), wzFile);
            view.addChild(file);
            nodeCache.put(file.getId(), file);
        } else if (node.getType() == WzNodeType.IMAGE) {
            WzImage wzImage = new WzImageFile(node.getName(), node.getPath().toString(), wzKey.getIv(), wzKey.getUserKey());
            WzNode file = new WzNode(view, nextId.getAndIncrement(), node.getName(), WzNodeType.IMAGE, node.getPath(), wzImage);
            view.addChild(file);
            nodeCache.put(file.getId(), file);
        } else {
            throw new BizException(ExceptionEnum.WZ_FILE_TYPE_ERROR);
        }
    }

    private void loadSub(WzNode folder, short gameVersion, WzKey wzKey) {
        Path folderPath = folder.getPath();
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            log.error("目录不存在: {}", folderPath);
            return;
        }
        try (var paths = Files.list(folderPath)) {
            List<WzNode> children = new ArrayList<>();
            paths.forEach(path -> {
                String filename = path.getFileName().toString();
                if (Files.isDirectory(path)) {
                    WzNode child = new WzNode(folder, nextId.getAndIncrement(), filename, WzNodeType.FOLDER, path);
                    children.add(child);
                    nodeCache.put(child.getId(), child);
                    loadSub(child, gameVersion, wzKey);
                } else if (filename.endsWith(".wz")) {
                    WzFile wzFile = new WzFile(String.valueOf(path), gameVersion, wzKey.getIv(), wzKey.getUserKey());
                    WzNode file = new WzNode(folder, nextId.getAndIncrement(), filename, WzNodeType.WZ, path, wzFile);
                    children.add(file);
                    nodeCache.put(file.getId(), file);
                } else if (filename.endsWith(".img")) {
                    WzImage wzImage = new WzImageFile(path.getFileName().toString(), String.valueOf(path), wzKey.getIv(), wzKey.getUserKey());
                    WzNode file = new WzNode(folder, nextId.getAndIncrement(), filename, WzNodeType.IMAGE, path, wzImage);
                    children.add(file);
                    nodeCache.put(file.getId(), file);
                }
            });
            folder.addChildren(children);
        } catch (IOException e) {
            log.error("读取目录时出错: {}", e.getMessage());
        }
    }

    /* 加载节点 --------------------------------------------------------------------------------------------------------*/
    public List<WzNode> getNode(int id) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        if (node.getChildren().isEmpty()) {
            if (node.getType() == WzNodeType.FOLDER) {
                loadFolder(node, nodeCache);
            } else if (node.getType() == WzNodeType.WZ) {
                loadWzNode(node);
            } else if (node.getType() == WzNodeType.WZ_DIRECTORY) {
                loadWzDirNode(node);
            } else if (node.getType() == WzNodeType.IMAGE) {
                loadImgNode(node);
            } else if (node.getType() == WzNodeType.IMAGE_LIST) {
                loadListNode(node);
            } else if (node.getType() == WzNodeType.IMAGE_CANVAS) {
                loadCanvasNode(node);
            } else if (node.getType() == WzNodeType.IMAGE_CONVEX) {
                loadConvexNode(node);
            } else {
                throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "未编码类型: " + node.getType());
            }
        }

        return node.getChildren();
    }

    private void loadWzNode(WzNode node) {
        if (node.getWzObject() instanceof WzFile wz) {
            wz.load();
            if (node.getChildren().isEmpty()) {
                List<WzNode> children = new ArrayList<>();
                wz.getWzDirectory().getDirectories().forEach(dir -> {
                    WzNode child = new WzNode(node, nextId.getAndIncrement(), dir.getName(), WzNodeType.WZ_DIRECTORY, null, dir);
                    children.add(child);
                    nodeCache.put(child.getId(), child);
                });
                wz.getWzDirectory().getImages().forEach(img -> {
                    WzNode child = new WzNode(node, nextId.getAndIncrement(), img.getName(), WzNodeType.IMAGE, null, img);
                    children.add(child);
                    nodeCache.put(child.getId(), child);
                });
                node.addChildren(children);
            }
        } else {
            log.error("错误的文件类型：WZ");
        }
    }

    private void loadWzDirNode(WzNode node) {
        if (node.getWzObject() instanceof WzDirectory wzDir) {
            if (node.getChildren().isEmpty()) {
                List<WzNode> children = new ArrayList<>();
                wzDir.getDirectories().forEach(subDir -> {
                    WzNode child = new WzNode(node, nextId.getAndIncrement(), subDir.getName(), WzNodeType.WZ_DIRECTORY, null, subDir);
                    children.add(child);
                    nodeCache.put(child.getId(), child);
                });
                wzDir.getImages().forEach(subImg -> {
                    WzNode child = new WzNode(node, nextId.getAndIncrement(), subImg.getName(), WzNodeType.IMAGE, null, subImg);
                    children.add(child);
                    nodeCache.put(child.getId(), child);
                });
                node.addChildren(children);
            }
        } else {
            log.error("错误的文件类型：WZ_DIRECTORY");
        }
    }

    private void loadImgNode(WzNode node) {
        if (node.getWzObject() instanceof WzImage image) {
            image.parse();
            loadNode(node, image.getChildren());
        } else {
            log.error("错误的文件类型：IMAGE");
        }
    }

    private void loadListNode(WzNode node) {
        if (node.getWzObject() instanceof WzListProperty list) {
            loadNode(node, list.getChildren());
        } else {
            log.error("错误的文件类型：IMAGE_LIST");
        }
    }

    private void loadCanvasNode(WzNode node) {
        if (node.getWzObject() instanceof WzCanvasProperty canvas) {
            loadNode(node, canvas.getChildren());
        } else {
            log.error("错误的文件类型：IMAGE_CANVAS");
        }
    }

    private void loadConvexNode(WzNode node) {
        if (node.getWzObject() instanceof WzConvexProperty convex) {
            loadNode(node, convex.getChildren());
        } else {
            log.error("错误的文件类型：IMAGE_CONVEX");
        }
    }

    private void loadNode(WzNode node, List<WzImageProperty> properties) {
        if (node.getChildren().isEmpty()) {
            List<WzNode> children = new ArrayList<>();
            properties.forEach(property -> {
                WzNodeType type = null;
                if (property instanceof WzCanvasProperty) {
                    type = WzNodeType.IMAGE_CANVAS;
                } else if (property instanceof WzConvexProperty) {
                    type = WzNodeType.IMAGE_CONVEX;
                } else if (property instanceof WzDoubleProperty) {
                    type = WzNodeType.IMAGE_DOUBLE;
                } else if (property instanceof WzFloatProperty) {
                    type = WzNodeType.IMAGE_FLOAT;
                } else if (property instanceof WzIntProperty) {
                    type = WzNodeType.IMAGE_INT;
                } else if (property instanceof WzListProperty) {
                    type = WzNodeType.IMAGE_LIST;
                } else if (property instanceof WzLongProperty) {
                    type = WzNodeType.IMAGE_LONG;
                } else if (property instanceof WzNullProperty) {
                    type = WzNodeType.IMAGE_NULL;
                } else if (property instanceof WzShortProperty) {
                    type = WzNodeType.IMAGE_SHORT;
                } else if (property instanceof WzSoundProperty) {
                    type = WzNodeType.IMAGE_SOUND;
                } else if (property instanceof WzStringProperty) {
                    type = WzNodeType.IMAGE_STRING;
                } else if (property instanceof WzUOLProperty) {
                    type = WzNodeType.IMAGE_UOL;
                } else if (property instanceof WzVectorProperty) {
                    type = WzNodeType.IMAGE_VECTOR;
                } else if (property instanceof WzRawDataProperty) {
                    type = WzNodeType.IMAGE_RAW_DATA;
                }

                WzNode child = new WzNode(node, nextId.getAndIncrement(), property.getName(), type, null, property);
                children.add(child);
                nodeCache.put(child.getId(), child);
            });
            node.addChildren(children);
        }
    }

    /* 卸载节点 --------------------------------------------------------------------------------------------------------*/
    public void unload(int id, boolean gc) {
        unload(id);
        if (gc) System.gc();
    }

    private void unload(int id) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND_FILE_OR_FOLDER);

        if (views.contains(id)) {
            Set<Integer> children = new HashSet<>();
            node.getChildren().forEach(child -> children.add(child.getId()));
            children.forEach(this::unload);
            return;
        }

        if (!node.isFile()) throw new BizException(ExceptionEnum.IS_NOT_FILE_OR_FOLDER);

        WzNode pNode = node.getParent();
        delChildNode(pNode, node.getId());
    }

    /* 节点值操作 ------------------------------------------------------------------------------------------------------*/
    public WzNodeValueDto getValue(int id) {
        WzNode node = nodeCache.get(id);
        if (node == null) return null;

        // 单独处理
        if (node.getWzObject() instanceof WzUOLProperty obj) {
            String uol = obj.getValue();
            WzCanvasProperty cav = getUolCanvas(obj.getParent(), uol.split("/"), 0);
            if (cav == null) return new WzNodeValueDto(null, WzNodeType.IMAGE_UOL, uol, null, null, null, null, null);
            return new WzNodeValueDto(null, WzNodeType.IMAGE_UOL, uol, cav.getWidth(), cav.getHeight(), cav.getBase64(), cav.getPngFormat(), null);
        }
        // 通用处理
        return switch (node.getWzObject()) {
            case WzCanvasProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_CANVAS, null, obj.getWidth(), obj.getHeight(), obj.getBase64(), obj.getPngFormat(), null);
            case WzDoubleProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_DOUBLE, String.valueOf(obj.getValue()), null, null, null, null);
            case WzFloatProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_FLOAT, String.valueOf(obj.getValue()), null, null, null, null);
            case WzIntProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_INT, String.valueOf(obj.getValue()), null, null, null, null);
            case WzLongProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_LONG, String.valueOf(obj.getValue()), null, null, null, null);
            case WzNullProperty ignored ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_NULL, null, null, null, null, null);
            case WzShortProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_SHORT, String.valueOf(obj.getValue()), null, null, null, null);
            case WzSoundProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_SOUND, null, null, null, null, obj.getBase64());
            case WzStringProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_STRING, obj.getValue(), null, null, null, null);
            case WzVectorProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_VECTOR, null, obj.getX(), obj.getY(), null, null);
            case null, default -> throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "未受支持的类型");
        };
    }

    private WzCanvasProperty getUolCanvas(WzObject wzObject, String[] path, int step) {
        if (path.length == 0) return null;

        if (wzObject instanceof WzListProperty list) {
            if (path.length == step + 1) {
                WzObject obj = list.getChild(path[step]);
                if (obj instanceof WzCanvasProperty cav) {
                    return cav;
                } else if (obj instanceof WzListProperty listObj && listObj.getChild("0") instanceof WzCanvasProperty cav) {
                    return cav;
                } else {
                    log.warn("UOL 对象是个奇怪的东西 : {}", String.join("/", path));
                    return null;
                }
            } else {
                String childName = path[step];
                if (childName.equalsIgnoreCase("..")) {
                    return getUolCanvas(list.getParent(), path, step + 1);
                } else {
                    return getUolCanvas(list.getChild(childName), path, step + 1);
                }
            }
        } else if (wzObject instanceof WzImage img) {
            if (path.length == step + 1) {
                return (WzCanvasProperty) (img.getChild(path[step]));
            } else {
                String childName = path[step];
                if (childName.equalsIgnoreCase("..")) {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "Img节点无法再往上查询 : " + String.join("/", Arrays.copyOfRange(path, 0, step + 1)));
                } else {
                    return getUolCanvas(img.getChild(childName), path, step + 1);
                }
            }
        } else {
            log.warn("UOL 加载到 [{}] 目标不是List或者Image, 无法继续下去了", String.join("/", Arrays.copyOfRange(path, 0, step + 1)));
            return null;
        }
    }

    public void updateValue(int id, WzNodeValueDto data) {
        WzNode node = nodeCache.get(id);
        if (node == null || node.getWzObject() == null) throw new BizException(ExceptionEnum.NOT_FOUND);
        if (data.getName().isEmpty())
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "名称不能为空");

        if (!node.getWzObject().getName().equalsIgnoreCase(data.getName())) { // 修改节点名
            // 检查同名
            WzNode parent = node.getParent();
            if (parent == null) throw new BizException(ExceptionEnum.NOT_FOUND);
            parent.getChildren().forEach(child -> {
                if (!child.getId().equals(node.getId()) && child.getName().equalsIgnoreCase(data.getName())) {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已经存在同名节点了！");
                }
            });

            node.getWzObject().setName(data.getName());
            node.setName(data.getName());
        }

        WzImage parentImg = getParentImg(node.getWzObject());
        if (parentImg == null) throw new BizException(ExceptionEnum.NOT_FOUND, "找不到所属IMG");

        switch (node.getWzObject()) {
            case WzCanvasProperty obj ->
                    obj.setPng(data.getPng(), parentImg.getReader().getWzMutableKey(), data.getPngFormat());
            case WzDoubleProperty obj -> obj.setValue(Double.parseDouble(data.getValue()));
            case WzFloatProperty obj -> obj.setValue(Float.parseFloat(data.getValue()));
            case WzIntProperty obj -> obj.setValue(Integer.parseInt(data.getValue()));
            case WzLongProperty obj -> obj.setValue(Long.parseLong(data.getValue()));
            case WzShortProperty obj -> obj.setValue(Short.parseShort(data.getValue()));
            case WzSoundProperty obj -> obj.setSound(data.getMp3(), parentImg.getReader().getWzMutableKey());
            case WzStringProperty obj -> obj.setValue(data.getValue());
            case WzUOLProperty obj -> obj.setValue(data.getValue());
            case WzVectorProperty obj -> {
                obj.setX(data.getX());
                obj.setY(data.getY());
            }
            default -> {
            }
        }
        parentImg.setChanged(true);
    }

    /* 节点操作 --------------------------------------------------------------------------------------------------------*/
    public void copy(int[] ids) {
        clipboard.clear();
        for (int id : ids) {
            WzNode node = nodeCache.get(id);
            if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);

            WzObject obj = node.getWzObject();

            switch (obj) {
                case WzDirectory wzDirectory -> clipboard.add(wzDirectory.deepClone(null));
                case WzImage wzImage -> clipboard.add(wzImage.deepClone(null));
                case WzImageProperty property -> clipboard.add(property.deepClone(null));
                default -> log.warn("无法复制对象: {}", obj.getName());
            }
        }
    }

    public void paste(int id) {
        if (clipboard.isEmpty()) throw new BizException(ExceptionEnum.CLIPBOARD_IS_EMPTY);
        getNode(id); // 确保子节点已加载
        WzNode targetNode = nodeCache.get(id);
        if (targetNode == null) throw new BizException(ExceptionEnum.NOT_FOUND);
        WzObject targetObj = targetNode.getWzObject();
        switch (targetObj) {
            case WzFile wzFile -> pasteToWzFile(targetNode, wzFile);
            case WzDirectory wzDir -> pasteToWzDir(targetNode, wzDir);
            case WzImage wzImage -> pasteToWzImage(targetNode, targetObj, wzImage);
            case WzListProperty property -> pasteToWzList(targetNode, targetObj, property);
            case WzConvexProperty property -> pasteToWzConvex(targetNode, targetObj, property);
            case WzCanvasProperty property -> pasteToWzCanvas(targetNode, targetObj, property);
            case null, default -> throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "该节点无法插入子节点");
        }

        clipboard.clear();
    }

    private void pasteToWzFile(WzNode targetNode, WzFile wzFile) {
        checkClipboardType(WzNodeType.WZ);
        setWzFileToCP(clipboard, wzFile);
        delRepeatDirFromCb(targetNode, wzFile.getWzDirectory());
        addDirFromCb(targetNode, wzFile.getWzDirectory());
    }

    private void pasteToWzDir(WzNode targetNode, WzDirectory wzDir) {
        checkClipboardType(WzNodeType.WZ_DIRECTORY);
        setWzFileToCP(clipboard, wzDir.getWzFile());
        delRepeatDirFromCb(targetNode, wzDir);
        addDirFromCb(targetNode, wzDir);
    }

    private void pasteToWzImage(WzNode targetNode, WzObject targetObj, WzImage wzImage) {
        checkClipboardType(WzNodeType.IMAGE);
        setWzImageToCP(clipboard, wzImage);
        delRepeatPropFromCb(targetNode, wzImage);
        addPropFromCb(targetNode, targetObj, wzImage);
    }

    private void pasteToWzList(WzNode targetNode, WzObject targetObj, WzListProperty property) {
        checkClipboardType(WzNodeType.IMAGE_LIST);
        setWzImageToCP(clipboard, property.getWzImage());
        delRepeatPropFromCb(targetNode, property);
        addPropFromCb(targetNode, targetObj, property);
    }

    private void pasteToWzConvex(WzNode targetNode, WzObject targetObj, WzConvexProperty property) {
        checkClipboardType(WzNodeType.IMAGE_CONVEX);
        setWzImageToCP(clipboard, property.getWzImage());
        delRepeatPropFromCb(targetNode, property);
        addPropFromCb(targetNode, targetObj, property);
    }

    private void pasteToWzCanvas(WzNode targetNode, WzObject targetObj, WzCanvasProperty property) {
        checkClipboardType(WzNodeType.IMAGE_CANVAS);
        setWzImageToCP(clipboard, property.getWzImage());
        delRepeatPropFromCb(targetNode, property);
        addPropFromCb(targetNode, targetObj, property);
    }

    private void setWzFileToCP(List<? extends WzObject> clipboard, WzFile wzFile) {
        if (clipboard == null) return;
        for (WzObject wzObject : clipboard) {
            if (wzObject instanceof WzDirectory directory) {
                directory.setWzFile(wzFile);
                setWzFileToCP(directory.getDirectories(), wzFile);
            }
        }
    }

    private void setWzImageToCP(List<? extends WzObject> clipboard, WzImage wzImage) {
        if (clipboard == null) return;
        for (WzObject wzObject : clipboard) {
            if (wzObject instanceof WzImageProperty property) {
                property.setWzImage(wzImage);
                setWzImageToCP(property.getChildren(), wzImage);
            }
        }
    }

    private void checkClipboardType(WzNodeType type) {
        Predicate<Object> validator;
        String errorMessage = switch (type) {
            case WZ, WZ_DIRECTORY -> {
                validator = child -> child instanceof WzImage || child instanceof WzDirectory;
                yield getTypeSpecificErrorMessage(type);
            }
            case IMAGE, IMAGE_LIST, IMAGE_CONVEX, IMAGE_CANVAS -> {
                validator = child -> child instanceof WzImageProperty;
                yield getTypeSpecificErrorMessage(type);
            }
            default -> throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "禁止的类型");
        };

        clipboard.stream()
                .filter(child -> !validator.test(child))
                .findFirst()
                .ifPresent(invalidChild -> {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, errorMessage);
                });
    }

    private String getTypeSpecificErrorMessage(WzNodeType type) {
        return switch (type) {
            case WZ -> "wz 文件只能粘贴 directory 或者 image";
            case WZ_DIRECTORY -> "directory 只能粘贴 directory 或者 image";
            case IMAGE -> "image 只能粘贴 property";
            case IMAGE_LIST -> "list 只能粘贴 property";
            case IMAGE_CONVEX -> "convex 只能粘贴 property";
            case IMAGE_CANVAS -> "canvas 只能粘贴 property";
            default -> "禁止的类型";
        };
    }

    private void delRepeatDirFromCb(WzNode targetNode, WzDirectory directory) {
        clipboard.forEach(child -> {
            if (child instanceof WzDirectory) {
                if (directory.removeDirectoryChild(child.getName())) {
                    delChildNodeByName(targetNode, child.getName());
                }
            } else if (child instanceof WzImage) {
                if (directory.removeImageChild(child.getName())) {
                    delChildNodeByName(targetNode, child.getName());
                }
            }
        });
    }

    private void delRepeatPropFromCb(WzNode targetNode, WzImage wzImage) {
        clipboard.forEach(child -> {
            if (wzImage.removeChild(child.getName())) {
                delChildNodeByName(targetNode, child.getName());
            }
        });
    }

    private void delRepeatPropFromCb(WzNode targetNode, WzImageProperty prop) {
        clipboard.forEach(child -> {
            if (prop.removeChild(child.getName())) {
                delChildNodeByName(targetNode, child.getName());
            }
        });
    }

    private void delChildNodeByName(WzNode targetNode, String name) {
        for (WzNode c : targetNode.getChildren()) {
            if (c.getName().equalsIgnoreCase(name)) {
                delChildNode(targetNode, c.getId());
                break;
            }
        }
    }

    private void delChildNode(WzNode pNode, int cId) {
        Set<Integer> ids = new HashSet<>();
        pNode.removeChild(ids, cId);
        ids.forEach(nodeCache::remove);
    }

    private void addPropFromCb(WzNode pNode, WzObject pObj, WzImage wzImage) {
        List<WzNode> children = new ArrayList<>();
        WzMutableKey wzKey = getWzKey(pObj);
        clipboard.forEach(child -> {
            WzImageProperty property = (WzImageProperty) child;
            property.setParent(pObj);
            initSpProp(property, wzKey);
            wzImage.addChild(property);

            WzNodeType type = WzNodeType.getByWzObjectType(property);
            WzNode childNode = new WzNode(pNode, nextId.getAndIncrement(), property.getName(), type, null, property);
            children.add(childNode);
            nodeCache.put(childNode.getId(), childNode);
        });
        pNode.addChildren(children);
        Objects.requireNonNull(getParentImg(pObj)).setChanged(true);
    }

    private void addPropFromCb(WzNode pNode, WzObject pObj, WzImageProperty prop) {
        List<WzNode> children = new ArrayList<>();
        WzMutableKey wzKey = getWzKey(pObj);
        clipboard.forEach(child -> {
            WzImageProperty property = (WzImageProperty) child;
            property.setParent(pObj);
            initSpProp(property, wzKey);
            prop.addChild(property);

            WzNodeType type = WzNodeType.getByWzObjectType(property);
            WzNode childNode = new WzNode(pNode, nextId.getAndIncrement(), property.getName(), type, null, property);
            children.add(childNode);
            nodeCache.put(childNode.getId(), childNode);
        });
        pNode.addChildren(children);
        Objects.requireNonNull(getParentImg(pObj)).setChanged(true);
    }

    private void addDirFromCb(WzNode pNode, WzDirectory pDir) {
        List<WzNode> children = new ArrayList<>();
        WzMutableKey wzKey = pDir.getWzMutableKey();
        clipboard.forEach(child -> {
            child.setParent(pDir);

            if (child instanceof WzDirectory directory) {
                pDir.addChild(directory);
                WzNode childNode = new WzNode(pNode, nextId.getAndIncrement(), directory.getName(), WzNodeType.WZ_DIRECTORY, null, directory);
                children.add(childNode);
                nodeCache.put(childNode.getId(), childNode);
            } else if (child instanceof WzImage image) {
                image.setReader(pDir.getWzFile().getReader()); // 为了避免从 Image 中取 key 取不到
                image.getChildren().forEach(property -> initSpProp(property, wzKey));
                pDir.addChild(image);
                WzNode childNode = new WzNode(pNode, nextId.getAndIncrement(), image.getName(), WzNodeType.IMAGE, null, image);
                children.add(childNode);
                nodeCache.put(childNode.getId(), childNode);
            }
        });

        pNode.addChildren(children);
    }

    private void initSpProp(WzImageProperty property, WzMutableKey wzKey) {
        if (property instanceof WzListProperty list) {
            list.getChildren().forEach(child -> initSpProp(child, wzKey));
        } else if (property instanceof WzCanvasProperty canvas) {
            canvas.compressPng(wzKey, Objects.requireNonNull(WzPngFormat.getByValue(canvas.getFormat() + canvas.getFormat2())));
        } else if (property instanceof WzSoundProperty sound) {
            sound.rebuildHeader(wzKey);
        }
    }

    public WzNode addNode(int parentId, WzNodeValueDto data) {
        WzNode pNode = nodeCache.get(parentId);
        if (pNode == null || pNode.getWzObject() == null) throw new BizException(ExceptionEnum.NOT_FOUND);
        if (!canAddNodeType(pNode.getWzObject(), data.getType()))
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "非法提交节点");
        if (data.getName().isEmpty())
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "名称不能为空");

        WzNode node;
        switch (pNode.getWzObject()) {
            case WzFile wzFile -> {
                wzFile.load();
                if (data.getType() == WzNodeType.WZ_DIRECTORY) {
                    if (wzFile.getWzDirectory().existDirectory(data.getName())) {
                        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                    WzDirectory obj = new WzDirectory(data.getName(), wzFile.getWzDirectory(), wzFile);
                    wzFile.getWzDirectory().addChild(obj);
                    node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), WzNodeType.WZ_DIRECTORY, null, obj);
                    pNode.addChild(node);
                    nodeCache.put(node.getId(), node);
                } else if (data.getType() == WzNodeType.IMAGE) {
                    if (wzFile.getWzDirectory().existImage(data.getName())) {
                        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                    WzImage img = new WzImage(data.getName(), wzFile.getReader(), wzFile);
                    img.setParsed(true);
                    img.setChanged(true);
                    wzFile.getWzDirectory().addChild(img);
                    node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), WzNodeType.IMAGE, null, img);
                    pNode.addChild(node);
                    nodeCache.put(node.getId(), node);
                } else {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "wz文件下只能添加wz目录和image类型的节点");
                }
            }
            case WzDirectory wzDirectory -> {
                if (data.getType() == WzNodeType.WZ_DIRECTORY) {
                    if (wzDirectory.existDirectory(data.getName())) {
                        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                    WzDirectory obj = new WzDirectory(data.getName(), wzDirectory, wzDirectory.getWzFile());
                    wzDirectory.addChild(obj);
                    node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), WzNodeType.WZ_DIRECTORY, null, obj);
                    pNode.addChild(node);
                    nodeCache.put(node.getId(), node);
                } else if (data.getType() == WzNodeType.IMAGE) {
                    if (wzDirectory.existImage(data.getName())) {
                        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                    WzImage img = new WzImage(data.getName(), wzDirectory.getWzFile().getReader(), wzDirectory);
                    img.setParsed(true);
                    img.setChanged(true);
                    wzDirectory.addChild(img);
                    node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), WzNodeType.IMAGE, null, img);
                    pNode.addChild(node);
                    nodeCache.put(node.getId(), node);
                } else {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "只能添加wz目录和image类型的节点");
                }
            }
            case WzImage img -> {
                if (img.existChild(data.getName())) {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                }
                WzImageProperty obj = genNewImageProperty(img, img, data);
                img.addChild(obj);
                node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), data.getType(), null, obj);
                pNode.addChild(node);
                nodeCache.put(node.getId(), node);
                img.setChanged(true);
            }
            case WzListProperty list -> {
                if (list.existChild(data.getName())) {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                }
                WzImageProperty obj = genNewImageProperty(getParentImg(list), list, data);
                list.addChild(obj);
                node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), data.getType(), null, obj);
                pNode.addChild(node);
                nodeCache.put(node.getId(), node);
                Objects.requireNonNull(getParentImg(list)).setChanged(true);
            }
            case WzCanvasProperty canvas -> {
                if (canvas.existChild(data.getName())) {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                }
                WzImageProperty obj = genNewImageProperty(getParentImg(canvas), canvas, data);
                canvas.addChild(obj);
                node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), data.getType(), null, obj);
                pNode.addChild(node);
                nodeCache.put(node.getId(), node);
                Objects.requireNonNull(getParentImg(canvas)).setChanged(true);
            }
            case WzConvexProperty convex -> {
                if (convex.existChild(data.getName())) {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                }
                WzImageProperty obj = genNewImageProperty(getParentImg(convex), convex, data);
                convex.addChild(obj);
                node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), data.getType(), null, obj);
                pNode.addChild(node);
                nodeCache.put(node.getId(), node);
                Objects.requireNonNull(getParentImg(convex)).setChanged(true);
            }
            default -> throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "该类型不可创建子节点");
        }

        return node;
    }

    public void deleteNode(int id) {
        WzNode node = nodeCache.get(id);
        if (node == null || node.getWzObject() == null) throw new BizException(ExceptionEnum.NOT_FOUND);
        if (node.getWzObject() instanceof WzFile)
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "该操作无法对wz文件进行");

        WzNode pNode = node.getParent();
        if (pNode == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        WzObject obj = node.getWzObject();
        WzObject pObj = obj.getParent();
        switch (pObj) {
            case null -> throw new BizException(ExceptionEnum.NOT_FOUND);
            case WzFile wzFile -> {
                if (obj instanceof WzDirectory) {
                    wzFile.getWzDirectory().removeDirectoryChild(obj.getName());
                } else if (obj instanceof WzImage) {
                    wzFile.getWzDirectory().removeImageChild(obj.getName());
                }
            }
            case WzDirectory wzDir -> {
                if (obj instanceof WzDirectory) {
                    wzDir.removeDirectoryChild(obj.getName());
                } else if (obj instanceof WzImage) {
                    wzDir.removeImageChild(obj.getName());
                }
            }
            case WzImage img -> {
                img.removeChild(obj.getName());
                img.setChanged(true);
            }
            case WzListProperty list -> {
                list.removeChild(obj.getName());
                Objects.requireNonNull(getParentImg(list)).setChanged(true);
            }
            case WzCanvasProperty canvas -> {
                canvas.removeChild(obj.getName());
                Objects.requireNonNull(getParentImg(canvas)).setChanged(true);
            }
            case WzConvexProperty convex -> {
                convex.removeChild(obj.getName());
                Objects.requireNonNull(getParentImg(convex)).setChanged(true);
            }
            default -> throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "不受支持的类型");
        }

        delChildNode(pNode, id);
    }

    /* 文件操作 --------------------------------------------------------------------------------------------------------*/
    public void saveNode(int id) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        if (node.getType() == WzNodeType.FOLDER) {
            saveNode(node);
            return;
        }

        WzObject obj = node.getWzObject();
        switch (obj) {
            case WzFile wzFile -> wzFile.save();
            case WzImageFile wzImage -> wzImage.save(Path.of(wzImage.getFilePath()));
            default -> throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "目标不是wz或则img文件");
        }

    }

    private void saveNode(WzNode wzNode) {
        if (wzNode.getType() == WzNodeType.FOLDER) {
            wzNode.getChildren().forEach(this::saveNode);
            return;
        }
        WzObject obj = wzNode.getWzObject();
        if (obj instanceof WzFile wzFile) {
            wzFile.save();
        } else if (obj instanceof WzImageFile wzImage) {
            wzImage.save(Path.of(wzImage.getFilePath()));
        }
    }

    /* 工具 -----------------------------------------------------------------------------------------------------------*/
    public void exportWzFileToImg(int id, Path basePath) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        if (basePath == null) basePath = Path.of(ServerConfig.WZ_DIRECTORY, "export");

        try {
            FileUtils.createDirectory(basePath);
        } catch (IOException e) {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (node.getType() == WzNodeType.FOLDER) {
            Path p = basePath.resolve(node.getName());
            try {
                FileUtils.createDirectory(p);
            } catch (IOException e) {
                throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, e.getMessage());
            }
            node.getChildren().forEach(child -> exportWzFileToImg(child.getId(), p));
            return;
        }

        WzObject obj = node.getWzObject();
        if (obj instanceof WzFile file) {
            if (file.getName().equalsIgnoreCase("List.wz")) return;
            file.load();

            file.exportFileToImg(basePath);
        }
    }

    public void exportWzFileToXml(int id, Path basePath, boolean indent) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        if (basePath == null) basePath = Path.of(ServerConfig.WZ_DIRECTORY, "export", "xml");

        try {
            FileUtils.createDirectory(basePath);
        } catch (IOException e) {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (node.getType() == WzNodeType.FOLDER) {
            Path p = basePath.resolve(node.getName());
            try {
                FileUtils.createDirectory(p);
            } catch (IOException e) {
                throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, e.getMessage());
            }
            node.getChildren().forEach(child -> exportWzFileToXml(child.getId(), p, indent));
            return;
        }

        WzObject obj = node.getWzObject();
        if (obj instanceof WzFile file) {
            if (file.getName().equalsIgnoreCase("List.wz")) return;
            file.load();

            file.exportFileToXml(basePath, indent);
        } else if (obj instanceof WzImage image) {
            image.exportToXml(basePath.resolve(image.getName() + ".xml"), indent);
        }
    }

    public void fixOutLink(int[] ids) {
        Instant now = Instant.now();
        WzMutableKey wzKey = null;
        List<Pair<WzCanvasProperty, List<String>>> cavCollect = new ArrayList<>();
        for (int id : ids) {
            WzNode node = nodeCache.get(id);
            if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);
            loadCavWzFiles(node);
            if (wzKey == null) wzKey = getWzKey(node.getWzObject());
            fixOutlink(cavCollect, node);
        }

        for (Pair<WzCanvasProperty, List<String>> pair : cavCollect) {
            fixOutlink(pair.getLeft(), pair.getRight(), wzKey);
        }

        lastCavPath = null;
        cavWzFiles.clear();
        Instant end = Instant.now();
        log.info("Outlink 结束，耗时 {} 秒", Duration.between(now, end).toSeconds());
    }

    private void fixOutlink(List<Pair<WzCanvasProperty, List<String>>> collector, WzNode node) {
        if (node.getType() == WzNodeType.WZ || node.getType() == WzNodeType.IMAGE || node.getType() == WzNodeType.IMAGE_LIST || node.getType() == WzNodeType.WZ_DIRECTORY) { // WZ_DIRECTORY 应该不存在吧？
            List<WzNode> children = getNode(node.getId());
            children.forEach(child -> fixOutlink(collector, child));
        } else if (node.getType() == WzNodeType.IMAGE_CANVAS) {
            WzCanvasProperty canvas = (WzCanvasProperty) node.getWzObject();

            List<String> outlinkPaths = getOutlinkPaths(canvas);
            if (outlinkPaths.isEmpty()) {
                return;
            }

            collector.add(new Pair<>((WzCanvasProperty) node.getWzObject(), outlinkPaths));
        }
    }

    private void loadCavWzFiles(WzNode node) {
        // getCavRelPath
        WzFile wzFile;
        Path cavFolderPath;
        short fileVersion;
        byte[] iv;
        byte[] key;
        if (node.getType() == WzNodeType.WZ) {
            wzFile = (WzFile) node.getWzObject();
        } else if (node.getType() == WzNodeType.IMAGE || node.getType() == WzNodeType.IMAGE_LIST || node.getType() == WzNodeType.IMAGE_CANVAS) {
            WzImage wzImage;
            if (node.getType() == WzNodeType.IMAGE) {
                wzImage = (WzImage) node.getWzObject();
            } else {
                wzImage = getParentImg(node.getWzObject());
            }
            if (wzImage == null) return;
            WzDirectory wzDirectory = (WzDirectory) wzImage.getParent();
            if (wzDirectory == null) return;
            wzFile = wzDirectory.getWzFile();
        } else {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "该类型 " + node.getType() + " 无法识别出_canvas目录在哪");
        }

        if (wzFile == null) return;
        File file = new File(wzFile.getFilePath());
        String dirPath = file.getParent();
        cavFolderPath = Path.of(dirPath, "_Canvas");
        fileVersion = wzFile.getHeader().getFileVersion();
        iv = wzFile.getWzIv();
        key = wzFile.getUserKey();
        String sPath = cavFolderPath.toString();
        if (lastCavPath != null && lastCavPath.equalsIgnoreCase(sPath)) return;

        lastCavPath = sPath;
        cavWzFiles.clear();
        List<Path> cavFiles;
        try (Stream<Path> pathStream = Files.walk(cavFolderPath)) {
            cavFiles = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("_Canvas_"))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("收集Canvas文件时出错: " + e.getMessage(), e);
        }

        cavFiles.forEach(cavFile -> cavWzFiles.add(new WzFile(cavFile.toString(), fileVersion, iv, key)));
        for (WzFile cavWzFile : cavWzFiles) {
            cavWzFile.load();
        }
    }

    private void fixOutlink(WzCanvasProperty canvas, List<String> outlinkPaths, WzMutableKey wzKey) {
        // 搜索 Canvas
        WzObject nodeReady;
        for (WzObject obj : cavWzFiles) {
            WzFile wzFile = (WzFile) obj;
            obj = wzFile.getWzDirectory();
            for (String p : outlinkPaths) {
                nodeReady = null;
                if (p.endsWith(".img") && obj instanceof WzDirectory dir) {
                    nodeReady = dir.getImage(p);
                } else if (obj instanceof WzImage img) {
                    img.parse();
                    nodeReady = img.getChild(p);
                } else if (obj instanceof WzListProperty listProp) {
                    nodeReady = listProp.getChild(p);
                }
                obj = nodeReady;
                if (obj == null) {
                    if (p.endsWith(".img")) break;

                    log.warn("canvas 路径查找失败 : 错误点 {} 完整路径 {} 文件 {}", p, outlinkPaths, wzFile.getName());
                    break;
                }
            }

            if (obj != null) {
                if (obj instanceof WzCanvasProperty n) {
                    if (n.getPngImage() == null) {
                        log.error("目标canvas的图片为空, 完整路径 {} 文件 {}", outlinkPaths, wzFile.getName());
                        break;
                    }
                    getParentImg(canvas).setChanged(true);
                    canvas.setPng(n.getPngImage(), wzKey);
                    break;
                }
                log.warn("找到了节点，但不是 WzCanvas 类型 : {}", outlinkPaths);
            }
        }
    }

    private List<String> getOutlinkPaths(WzImageProperty prop) {
        WzImageProperty outlinkNode = prop.getChild("_outlink");
        if (outlinkNode == null) {
            log.warn("不存在 _outlink 节点");
            return new ArrayList<>();
        }

        String outlink = ((WzStringProperty) outlinkNode).getValue();

        List<String> outlinkPaths =
                Arrays.stream(outlink.split("/"))
                        .map(p -> {
                            if (p.equals("??")) return "碟喻";
                            if (p.equals("奢辨_??00")) return "奢辨_碟喻00";
                            return p;
                        })
                        .collect(Collectors.toList());
        int rootIndex = outlinkPaths.indexOf("_Canvas") + 1;
        if (rootIndex == 0) {
            log.warn("outlink 路径中找不到 _Canvas");
            return new ArrayList<>();
        }

        return outlinkPaths.subList(rootIndex, outlinkPaths.size());
    }

    public void updateKey(int id, short gameVersion, String key) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND_FILE_OR_FOLDER);

        if (node.getType() == WzNodeType.FOLDER) {
            node.getChildren().forEach(child -> updateKey(child.getId(), gameVersion, key));
        }

        WzKey wzKey = wzKeyRepository.findByName(key);
        if (wzKey == null) throw new BizException(ExceptionEnum.NOT_FOUND_WZ_KEY);

        WzObject wz = node.getWzObject();
        if (wz instanceof WzFile wzFile) {
            wzFile.changeKey(gameVersion, wzKey.getIv(), wzKey.getUserKey());
        } else if (wz instanceof WzImageFile wzImage && wzImage.getFilePath() != null) {
            wzImage.changeKey(wzKey.getIv(), wzKey.getUserKey());
        } else {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "只能对 WZ 或者 IMG 文件使用");
        }
    }

    public void localization(int fromId, int toId) {
        WzNode fromNode = nodeCache.get(fromId);
        WzNode toNode = nodeCache.get(toId);
        if (fromNode == null || toNode == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        WzObject from = fromNode.getWzObject();
        WzObject to = toNode.getWzObject();

        localization(from, to);
    }

    public void localization(WzObject from, WzObject to) {
        if (from == null || to == null) return;

        if (to instanceof WzFile toFile && from instanceof WzFile fromFile) {
            toFile.load();
            fromFile.load();
            toFile.getWzDirectory().getDirectories().forEach(toDir -> localization(fromFile.getWzDirectory().getDirectory(toDir.getName()), toDir));
            toFile.getWzDirectory().getImages().forEach(toImage -> localization(fromFile.getWzDirectory().getImage(toImage.getName()), toImage));
        } else if (to instanceof WzDirectory toDirectory && from instanceof WzDirectory fromDirectory) {
            toDirectory.getDirectories().forEach(toDir -> localization(fromDirectory.getDirectory(toDir.getName()), toDir));
            toDirectory.getImages().forEach(toImage -> localization(fromDirectory.getImage(toImage.getName()), toImage));
        } else if (to instanceof WzImage toImage && from instanceof WzImage fromImage) {
            toImage.parse();
            fromImage.parse();
            toImage.getChildren().forEach(img -> localization(fromImage.getChild(img.getName()), img));
        } else if (to instanceof WzListProperty toListProperty && from instanceof WzListProperty fromList) {
            toListProperty.getChildren().forEach(prop -> localization(fromList.getChild(prop.getName()), prop));
        } else if (to instanceof WzStringProperty toString && from instanceof WzStringProperty fromString) {
            String fromValue = fromString.getValue();
            if (fromValue != null && !isChinese(toString.getValue()) && isChinese(fromValue)) {
                getParentImg(toString).setChanged(true);
                toString.setValue(fromValue);
            }
        }
    }

    private boolean isChinese(String str) {
        return !str.matches(".*[\\uAC00-\\uD7A3].*")  // 不能有韩文
                && str.matches(".*[\\u4e00-\\u9fa5].*");  // 有中文字符
    }

    public void packet(short fileVersion, int id) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);
        if (node.getType() != WzNodeType.FOLDER) throw new BizException(ExceptionEnum.ONLY_FOLDER);

        try {
            Path basePath = Path.of(ServerConfig.WZ_DIRECTORY, "export", "打包wz");
            FileUtils.createDirectory(basePath);
        } catch (IOException e) {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        // Data 一种打法，常规Folder一种打法
        if (node.getName().equalsIgnoreCase("Data")) {
            packetData(fileVersion, node);
        } else {
            packetFolder(fileVersion, node);
        }
    }

    private void packetData(short fileVersion, WzNode dataNode) {
        Set<String> directories = new HashSet<>();
        List<WzImageFile> images = new ArrayList<>();
        for (WzNode node : dataNode.getChildren()) {
            if (node.getType() == WzNodeType.FOLDER) {
                directories.add(node.getName());
            } else if (node.getType() == WzNodeType.IMAGE) {
                images.add((WzImageFile) node.getWzObject());
            }
        }

        byte[] iv = images.getFirst().getIv();
        byte[] key = images.getFirst().getKey();


        // Packet Base.wz
        Path filePath = Path.of(ServerConfig.WZ_DIRECTORY, "export", "打包wz", "Base.wz");
        WzFile wzFile = WzFile.createNewFile(filePath.toString(), fileVersion, iv, key);
        directories.forEach(director -> wzFile.getWzDirectory().addChild(new WzDirectory(director, wzFile.getWzDirectory(), wzFile)));
        images.forEach(image -> {
            image.parse(false);
            wzFile.getWzDirectory().addChild(image);
        });
        wzFile.save();

        // Packet Other.wz
        for (WzNode node : dataNode.getChildren()) {
            if (node.getType() == WzNodeType.FOLDER) {
                packetFolder(fileVersion, node, iv, key);
            }
        }
    }

    private void packetFolder(short fileVersion, WzNode dataNode) {
        Map<String, byte[]> ik = searchIvKey(dataNode);
        if (ik.isEmpty()) throw new BizException(ExceptionEnum.NOT_FOUND_IV_KEY);

        packetFolder(fileVersion, dataNode, ik.get("iv"), ik.get("key"));
    }

    private void packetFolder(short fileVersion, WzNode dataNode, byte[] iv, byte[] key) {
        Path filePath = Path.of(ServerConfig.WZ_DIRECTORY, "export", "打包wz", dataNode.getName() + ".wz");
        WzFile wzFile = WzFile.createNewFile(filePath.toString(), fileVersion, iv, key);

        packetSubToWz(dataNode, wzFile.getWzDirectory());

        wzFile.save();
    }

    private void packetSubToWz(WzNode dataNode, WzDirectory wzDir) {
        for (WzNode node : dataNode.getChildren()) {
            if (node.getType() == WzNodeType.FOLDER) {
                WzDirectory subDir = new WzDirectory(node.getName(), wzDir, wzDir.getWzFile());
                packetSubToWz(node, subDir);
                wzDir.addChild(subDir);
            } else if (node.getType() == WzNodeType.IMAGE) {
                WzImage image = (WzImage) node.getWzObject();
                image.parse(false);
                wzDir.addChild(image);
            }
        }
    }

    private Map<String, byte[]> searchIvKey(WzNode dataNode) {
        Map<String, byte[]> result = new HashMap<>();

        for (WzNode subNode : dataNode.getChildren()) {
            if (subNode.getType() == WzNodeType.IMAGE) {
                WzImageFile image = (WzImageFile) subNode.getWzObject();
                result.put("iv", image.getIv());
                result.put("key", image.getKey());
                return result;
            } else if (subNode.getType() == WzNodeType.FOLDER) {
                return searchIvKey(subNode);
            }
        }

        return result;
    }

    public List<WzNodeValueDto> getAllCanvas(int id) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        List<WzNodeValueDto> result = new ArrayList<>();
        if (node.getWzObject() instanceof WzImage image) {
            image.parse();
            for (WzImageProperty prop : image.getChildren()) {
                if (prop instanceof WzListProperty listProperty) {
                    searchAllCanvas(result, listProperty, listProperty.getName() + "/");
                }
            }
            return result;
        } else if (node.getWzObject() instanceof WzListProperty listProperty) {
            searchAllCanvas(result, listProperty, "");
            return result;
        } else {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "查询对象不是 List");
        }
    }

    private void searchAllCanvas(List<WzNodeValueDto> result, WzListProperty list, String path) {
        for (WzObject sub : list.getChildren()) {
            if (sub instanceof WzListProperty subList) {
                searchAllCanvas(result, subList, path + subList.getName() + "/");
            } else if (sub instanceof WzCanvasProperty subCav) {
                int w = subCav.getWidth();
                int h = subCav.getHeight();
                String b = subCav.getBase64();
                WzPngFormat f = subCav.getPngFormat();
                String p = path + subCav.getName();
                result.add(new WzNodeValueDto(null, WzNodeType.IMAGE_CANVAS, p, w, h, b, f, null));
            }
        }
    }

    /* 通用方法 --------------------------------------------------------------------------------------------------------*/
    private WzImage getParentImg(WzObject wzObject) {
        if (wzObject instanceof WzImage obj) {
            return obj;
        } else if (wzObject == null || wzObject.getParent() == null) {
            return null;
        } else {
            return getParentImg(wzObject.getParent());
        }
    }

    private WzMutableKey getWzKey(WzObject wzObject) {
        if (wzObject instanceof WzFile wzFile) {
            wzFile.load();
            return wzFile.getReader().getWzMutableKey();
        }

        WzImage wzImage = getParentImg(wzObject);
        if (wzImage == null) return null;
        return wzImage.getReader().getWzMutableKey();
    }

    private boolean canAddNodeType(WzObject object, WzNodeType targetType) {
        if (object instanceof WzFile || object instanceof WzDirectory)
            return targetType == WzNodeType.WZ_DIRECTORY || targetType == WzNodeType.IMAGE;
        else
            return object instanceof WzImage || object instanceof WzListProperty || object instanceof WzCanvasProperty || object instanceof WzConvexProperty;
    }

    private WzImageProperty genNewImageProperty(WzImage img, WzObject parent, WzNodeValueDto data) {
        if (data.getType() == WzNodeType.IMAGE_CANVAS) {
            WzCanvasProperty canvasProp = new WzCanvasProperty(data.getName(), parent, img);
            canvasProp.initPngProperty(data.getName(), canvasProp, img);
            canvasProp.setPng(data.getPng(), img.getReader().getWzMutableKey(), data.getPngFormat());
            return canvasProp;
        } else if (data.getType() == WzNodeType.IMAGE_CONVEX) {
            return new WzConvexProperty(data.getName(), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_DOUBLE) {
            return new WzDoubleProperty(data.getName(), Double.parseDouble(data.getValue()), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_FLOAT) {
            return new WzFloatProperty(data.getName(), Float.parseFloat(data.getValue()), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_INT) {
            return new WzIntProperty(data.getName(), Integer.parseInt(data.getValue()), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_LIST) {
            return new WzListProperty(data.getName(), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_LONG) {
            return new WzLongProperty(data.getName(), Long.parseLong(data.getValue()), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_NULL) {
            return new WzNullProperty(data.getName(), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_SHORT) {
            return new WzShortProperty(data.getName(), Short.parseShort(data.getValue()), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_SOUND) {
            WzSoundProperty sound = new WzSoundProperty(data.getName(), parent, img);
            sound.setSound(data.getMp3(), img.getReader().getWzMutableKey());
            return sound;
        } else if (data.getType() == WzNodeType.IMAGE_STRING) {
            return new WzStringProperty(data.getName(), data.getValue(), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_UOL) {
            return new WzUOLProperty(data.getName(), data.getValue(), parent, img);
        } else if (data.getType() == WzNodeType.IMAGE_VECTOR) {
            return new WzVectorProperty(data.getName(), data.getX(), data.getY(), parent, img);
        }
        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "节点生成失败");
    }
}
