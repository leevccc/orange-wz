package orange.wz.service;

import lombok.extern.slf4j.Slf4j;
import orange.wz.config.ServerConfig;
import orange.wz.exception.BizException;
import orange.wz.exception.ExceptionEnum;
import orange.wz.lib.*;
import orange.wz.lib.properties.*;
import orange.wz.model.*;
import orange.wz.utils.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
        unload(view);
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
            WzImage wzImage = new WzImage(node.getName(), node.getPath().toString(), wzKey.getIv(), wzKey.getUserKey());
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
                    WzImage wzImage = new WzImage(path.getFileName().toString(), String.valueOf(path), wzKey.getIv(), wzKey.getUserKey());
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
            wz.parse();
            if (node.getChildren().isEmpty()) {
                List<WzNode> children = new ArrayList<>();
                wz.getWzDirectory().getDirectories().forEach((s, wzDirectory) -> {
                    WzNode child = new WzNode(node, nextId.getAndIncrement(), s, WzNodeType.WZ_DIRECTORY, null, wzDirectory);
                    children.add(child);
                    nodeCache.put(child.getId(), child);
                });
                wz.getWzDirectory().getImages().forEach((s, wzImage) -> {
                    WzNode child = new WzNode(node, nextId.getAndIncrement(), s, WzNodeType.IMAGE, null, wzImage);
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
                wzDir.getDirectories().forEach((s, subDir) -> {
                    WzNode child = new WzNode(node, nextId.getAndIncrement(), s, WzNodeType.WZ_DIRECTORY, null, subDir);
                    children.add(child);
                    nodeCache.put(child.getId(), child);
                });
                wzDir.getImages().forEach((s, wzImage) -> {
                    WzNode child = new WzNode(node, nextId.getAndIncrement(), s, WzNodeType.IMAGE, null, wzImage);
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
            loadNode(node, image.getProperties());
        } else {
            log.error("错误的文件类型：IMAGE");
        }
    }

    private void loadListNode(WzNode node) {
        if (node.getWzObject() instanceof WzListProperty list) {
            loadNode(node, list.getProperties());
        } else {
            log.error("错误的文件类型：IMAGE_LIST");
        }
    }

    private void loadCanvasNode(WzNode node) {
        if (node.getWzObject() instanceof WzCanvasProperty canvas) {
            loadNode(node, canvas.getProperties());
        } else {
            log.error("错误的文件类型：IMAGE_CANVAS");
        }
    }

    private void loadConvexNode(WzNode node) {
        if (node.getWzObject() instanceof WzConvexProperty convex) {
            loadNode(node, convex.getProperties());
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
                }

                WzNode child = new WzNode(node, nextId.getAndIncrement(), property.getName(), type, null, property);
                children.add(child);
                nodeCache.put(child.getId(), child);
            });
            node.addChildren(children);
        }
    }

    /* 卸载节点 --------------------------------------------------------------------------------------------------------*/
    public void unload(int id) {
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
        Set<Integer> ids = new HashSet<>();
        pNode.removeChild(ids, node.getId());
        ids.forEach(nodeCache::remove);
    }

    /* 节点值操作 ------------------------------------------------------------------------------------------------------*/
    public WzNodeValueDto getValue(int id) {
        WzNode node = nodeCache.get(id);
        if (node == null) return null;

        return switch (node.getWzObject()) {
            case WzCanvasProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_CANVAS, null, null, null, obj.getPng().getBase64(), obj.getPng().getPngFormat(), null);
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
            case WzUOLProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_UOL, obj.getUol(), null, null, null, null);
            case WzVectorProperty obj ->
                    new WzNodeValueDto(null, WzNodeType.IMAGE_VECTOR, null, obj.getX().getValue(), obj.getY().getValue(), null, null);
            case null, default -> throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "未受支持的类型");
        };
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
                    obj.getPng().setPng(data.getPng(), parentImg.getReader().getWzKey(), data.getPngFormat());
            case WzDoubleProperty obj -> obj.setValue(Double.parseDouble(data.getValue()));
            case WzFloatProperty obj -> obj.setValue(Float.parseFloat(data.getValue()));
            case WzIntProperty obj -> obj.setValue(Integer.parseInt(data.getValue()));
            case WzLongProperty obj -> obj.setValue(Long.parseLong(data.getValue()));
            case WzShortProperty obj -> obj.setValue(Short.parseShort(data.getValue()));
            case WzSoundProperty obj -> obj.setSound(data.getMp3(), parentImg.getReader().getWzKey());
            case WzStringProperty obj -> obj.setValue(data.getValue());
            case WzUOLProperty obj -> obj.setUol(data.getValue());
            case WzVectorProperty obj -> {
                obj.getX().setValue(data.getX());
                obj.getY().setValue(data.getY());
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
        byte[] wzKey = getWzKey(targetObj);
        if (targetObj instanceof WzListProperty property) {
            List<WzImageProperty> props = new ArrayList<>();
            List<WzNode> children = new ArrayList<>();
            clipboard.forEach(child -> {
                if (child instanceof WzImageProperty imageProperty) {
                    // 删除同名
                    for (WzImageProperty p : property.getProperties()) {
                        if (p.getName().equalsIgnoreCase(imageProperty.getName())) {
                            property.getProperties().remove(p);
                            for (WzNode c : targetNode.getChildren()) {
                                if (c.getName().equalsIgnoreCase(imageProperty.getName())) {
                                    Set<Integer> ids = new HashSet<>();
                                    targetNode.removeChild(ids, c.getId());
                                    ids.forEach(nodeCache::remove);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // 执行插入操作
                    imageProperty.setParent(targetObj);
                    props.add(imageProperty);
                    if (imageProperty instanceof WzPngProperty pngProperty) {
                        pngProperty.compressPng(wzKey, Objects.requireNonNull(WzPngFormat.getByValue(pngProperty.getFormat() + pngProperty.getFormat2())));
                    } else if (imageProperty instanceof WzSoundProperty sound) {
                        sound.rebuildHeader(wzKey);
                    }

                    WzNodeType type = WzNodeType.getByWzObjectType(imageProperty);
                    WzNode childNode = new WzNode(targetNode, nextId.getAndIncrement(), imageProperty.getName(), type, null, imageProperty);
                    children.add(childNode);
                    nodeCache.put(childNode.getId(), childNode);
                } else {
                    log.warn("WzImage 只能粘贴 WzImageProperty");
                    throw new BizException(ExceptionEnum.ONLY_WZ_IMAGE);
                }
            });
            targetNode.addChildren(children);
            property.addProperties(props);
            Objects.requireNonNull(getParentImg(property)).setChanged(true);
        } else if (targetObj instanceof WzFile wzFile) {
            List<WzNode> children = new ArrayList<>();
            clipboard.forEach(child -> {
                if (child instanceof WzImage wzImage) {
                    // 删除同名
                    for (WzImage i : wzFile.getWzDirectory().getImages().values()) {
                        if (i.getName().equalsIgnoreCase(wzImage.getName())) {
                            wzFile.getWzDirectory().getImages().remove(i.getName());
                            for (WzNode c : targetNode.getChildren()) {
                                if (c.getName().equalsIgnoreCase(wzImage.getName())) {
                                    Set<Integer> ids = new HashSet<>();
                                    targetNode.removeChild(ids, c.getId());
                                    ids.forEach(nodeCache::remove);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // 执行插入操作
                    wzImage.setParent(wzFile.getWzDirectory());
                    wzImage.setReader(wzFile.getWzDirectory().getReader()); // 为了避免从 Image 中取 key 取不到
                    wzFile.getWzDirectory().getImages().put(wzImage.getName(), wzImage);
                    WzNode childNode = new WzNode(targetNode, nextId.getAndIncrement(), wzImage.getName(), WzNodeType.IMAGE, null, wzImage);
                    children.add(childNode);
                    nodeCache.put(childNode.getId(), childNode);
                } else if (child instanceof WzDirectory wzDir) {
                    // 删除同名
                    for (WzDirectory i : wzFile.getWzDirectory().getDirectories().values()) {
                        if (i.getName().equalsIgnoreCase(wzDir.getName())) {
                            wzFile.getWzDirectory().getDirectories().remove(i.getName());
                            for (WzNode c : targetNode.getChildren()) {
                                if (c.getName().equalsIgnoreCase(wzDir.getName())) {
                                    Set<Integer> ids = new HashSet<>();
                                    targetNode.removeChild(ids, c.getId());
                                    ids.forEach(nodeCache::remove);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // 执行插入操作
                    wzDir.setParent(wzFile.getWzDirectory());
                    wzDir.setReader(wzFile.getWzDirectory().getReader()); // 为了避免从 Image 中取 key 取不到
                    wzFile.getWzDirectory().getDirectories().put(wzDir.getName(), wzDir);
                    WzNode childNode = new WzNode(targetNode, nextId.getAndIncrement(), wzDir.getName(), WzNodeType.WZ_DIRECTORY, null, wzDir);
                    children.add(childNode);
                    nodeCache.put(childNode.getId(), childNode);
                } else {
                    log.warn("Wz 只能粘贴 WzDirectory 或 WzImage");
                    throw new BizException(ExceptionEnum.ONLY_WZ_DIR_OR_IMAGE);
                }
            });

            targetNode.addChildren(children);
        } else if (targetObj instanceof WzDirectory wzDir) {
            List<WzNode> children = new ArrayList<>();
            clipboard.forEach(child -> {
                if (child instanceof WzImage wzImage) {
                    // 删除同名
                    for (WzImage i : wzDir.getImages().values()) {
                        if (i.getName().equalsIgnoreCase(wzImage.getName())) {
                            wzDir.getImages().remove(i.getName());
                            for (WzNode c : targetNode.getChildren()) {
                                if (c.getName().equalsIgnoreCase(wzImage.getName())) {
                                    Set<Integer> ids = new HashSet<>();
                                    targetNode.removeChild(ids, c.getId());
                                    ids.forEach(nodeCache::remove);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // 执行插入操作
                    wzImage.setParent(wzDir);
                    wzImage.setReader(wzDir.getReader()); // 为了避免从 Image 中取 key 取不到
                    wzDir.getImages().put(wzImage.getName(), wzImage);
                    WzNode childNode = new WzNode(targetNode, nextId.getAndIncrement(), wzImage.getName(), WzNodeType.IMAGE, null, wzImage);
                    children.add(childNode);
                    nodeCache.put(childNode.getId(), childNode);
                } else if (child instanceof WzDirectory childDir) {
                    // 删除同名
                    for (WzDirectory i : wzDir.getDirectories().values()) {
                        if (i.getName().equalsIgnoreCase(childDir.getName())) {
                            wzDir.getDirectories().remove(i.getName());
                            for (WzNode c : targetNode.getChildren()) {
                                if (c.getName().equalsIgnoreCase(childDir.getName())) {
                                    Set<Integer> ids = new HashSet<>();
                                    targetNode.removeChild(ids, c.getId());
                                    ids.forEach(nodeCache::remove);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // 执行插入操作
                    childDir.setParent(wzDir);
                    childDir.setReader(wzDir.getReader()); // 为了避免从 Image 中取 key 取不到
                    wzDir.getDirectories().put(childDir.getName(), childDir);
                    WzNode childNode = new WzNode(targetNode, nextId.getAndIncrement(), childDir.getName(), WzNodeType.WZ_DIRECTORY, null, childDir);
                    children.add(childNode);
                    nodeCache.put(childNode.getId(), childNode);
                } else {
                    log.warn("Wz 只能粘贴 WzDirectory 或 WzImage");
                    throw new BizException(ExceptionEnum.ONLY_WZ_DIR_OR_IMAGE);
                }
            });

            targetNode.addChildren(children);
        } else if (targetObj instanceof WzImage wzImage) {
            List<WzImageProperty> props = new ArrayList<>();
            List<WzNode> children = new ArrayList<>();
            clipboard.forEach(child -> {
                if (child instanceof WzImageProperty imageProperty) {
                    // 删除同名
                    for (WzImageProperty p : wzImage.getProperties()) {
                        if (p.getName().equalsIgnoreCase(imageProperty.getName())) {
                            wzImage.getProperties().remove(p);
                            for (WzNode c : targetNode.getChildren()) {
                                if (c.getName().equalsIgnoreCase(imageProperty.getName())) {
                                    Set<Integer> ids = new HashSet<>();
                                    targetNode.removeChild(ids, c.getId());
                                    ids.forEach(nodeCache::remove);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // 执行插入操作
                    imageProperty.setParent(targetObj);
                    props.add(imageProperty);
                    if (imageProperty instanceof WzPngProperty pngProperty) {
                        pngProperty.compressPng(wzKey, Objects.requireNonNull(WzPngFormat.getByValue(pngProperty.getFormat() + pngProperty.getFormat2())));
                    } else if (imageProperty instanceof WzSoundProperty sound) {
                        sound.rebuildHeader(wzKey);
                    }

                    WzNodeType type = WzNodeType.getByWzObjectType(imageProperty);
                    WzNode childNode = new WzNode(targetNode, nextId.getAndIncrement(), imageProperty.getName(), type, null, imageProperty);
                    children.add(childNode);
                    nodeCache.put(childNode.getId(), childNode);
                } else {
                    log.warn("WzImage 只能粘贴 WzImageProperty");
                    throw new BizException(ExceptionEnum.ONLY_WZ_IMAGE);
                }
            });
            targetNode.addChildren(children);
            wzImage.getProperties().addAll(props);
            wzImage.setChanged(true);
        } else if (targetObj instanceof WzConvexProperty property) {
            List<WzImageProperty> props = new ArrayList<>();
            List<WzNode> children = new ArrayList<>();
            clipboard.forEach(child -> {
                if (child instanceof WzImageProperty imageProperty) {
                    // 删除同名
                    for (WzImageProperty p : property.getProperties()) {
                        if (p.getName().equalsIgnoreCase(imageProperty.getName())) {
                            property.getProperties().remove(p);
                            for (WzNode c : targetNode.getChildren()) {
                                if (c.getName().equalsIgnoreCase(imageProperty.getName())) {
                                    Set<Integer> ids = new HashSet<>();
                                    targetNode.removeChild(ids, c.getId());
                                    ids.forEach(nodeCache::remove);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // 执行插入操作
                    imageProperty.setParent(targetObj);
                    props.add(imageProperty);
                    if (imageProperty instanceof WzPngProperty pngProperty) {
                        pngProperty.compressPng(wzKey, Objects.requireNonNull(WzPngFormat.getByValue(pngProperty.getFormat() + pngProperty.getFormat2())));
                    } else if (imageProperty instanceof WzSoundProperty sound) {
                        sound.rebuildHeader(wzKey);
                    }

                    WzNodeType type = WzNodeType.getByWzObjectType(imageProperty);
                    WzNode childNode = new WzNode(targetNode, nextId.getAndIncrement(), imageProperty.getName(), type, null, imageProperty);
                    children.add(childNode);
                    nodeCache.put(childNode.getId(), childNode);
                } else {
                    log.warn("WzImage 只能粘贴 WzImageProperty");
                    throw new BizException(ExceptionEnum.ONLY_WZ_IMAGE);
                }
            });
            targetNode.addChildren(children);
            property.getProperties().addAll(props);
            Objects.requireNonNull(getParentImg(property)).setChanged(true);
        } else if (targetObj instanceof WzCanvasProperty property) {
            List<WzImageProperty> props = new ArrayList<>();
            List<WzNode> children = new ArrayList<>();
            clipboard.forEach(child -> {
                if (child instanceof WzImageProperty imageProperty) {
                    // 删除同名
                    for (WzImageProperty p : property.getProperties()) {
                        if (p.getName().equalsIgnoreCase(imageProperty.getName())) {
                            property.getProperties().remove(p);
                            for (WzNode c : targetNode.getChildren()) {
                                if (c.getName().equalsIgnoreCase(imageProperty.getName())) {
                                    Set<Integer> ids = new HashSet<>();
                                    targetNode.removeChild(ids, c.getId());
                                    ids.forEach(nodeCache::remove);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // 执行插入操作
                    imageProperty.setParent(targetObj);
                    props.add(imageProperty);
                    if (imageProperty instanceof WzPngProperty pngProperty) {
                        pngProperty.compressPng(wzKey, Objects.requireNonNull(WzPngFormat.getByValue(pngProperty.getFormat() + pngProperty.getFormat2())));
                    } else if (imageProperty instanceof WzSoundProperty sound) {
                        sound.rebuildHeader(wzKey);
                    }

                    WzNodeType type = WzNodeType.getByWzObjectType(imageProperty);
                    WzNode childNode = new WzNode(targetNode, nextId.getAndIncrement(), imageProperty.getName(), type, null, imageProperty);
                    children.add(childNode);
                    nodeCache.put(childNode.getId(), childNode);
                } else {
                    log.warn("WzImage 只能粘贴 WzImageProperty");
                    throw new BizException(ExceptionEnum.ONLY_WZ_IMAGE);
                }
            });
            targetNode.addChildren(children);
            property.addProperties(props);
            Objects.requireNonNull(getParentImg(property)).setChanged(true);
        } else {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "只能在 Img List 下粘贴");
        }

        clipboard.clear();
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
                wzFile.parse();
                if (data.getType() == WzNodeType.WZ_DIRECTORY) {
                    for (WzDirectory directory : wzFile.getWzDirectory().getDirectories().values()) {
                        if (directory.getName().equalsIgnoreCase(data.getName()))
                            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                    WzDirectory obj = new WzDirectory(wzFile.getWzDirectory().getReader(), data.getName(), wzFile.getVersionHash(), wzFile.getWzIv(), wzFile);
                    wzFile.getWzDirectory().getDirectories().put(data.getName(), obj);
                    node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), WzNodeType.WZ_DIRECTORY, null, obj);
                    pNode.addChild(node);
                    nodeCache.put(node.getId(), node);
                } else if (data.getType() == WzNodeType.IMAGE) {
                    for (WzImage image : wzFile.getWzDirectory().getImages().values()) {
                        if (image.getName().equalsIgnoreCase(data.getName())) {
                            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                        }
                    }
                    WzImage img = new WzImage(data.getName(), wzFile.getWzDirectory().getReader());
                    img.setParsed(true);
                    img.setChanged(true);
                    wzFile.getWzDirectory().getImages().put(data.getName(), img);
                    node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), WzNodeType.IMAGE, null, img);
                    pNode.addChild(node);
                    nodeCache.put(node.getId(), node);
                } else {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "wz文件下只能添加wz目录和image类型的节点");
                }
            }
            case WzDirectory wzDirectory -> {
                if (data.getType() == WzNodeType.WZ_DIRECTORY) {
                    for (WzDirectory directory : wzDirectory.getDirectories().values()) {
                        if (directory.getName().equalsIgnoreCase(data.getName()))
                            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                    WzDirectory obj = new WzDirectory(wzDirectory.getReader(), data.getName(), wzDirectory.getWzFile().getVersionHash(), wzDirectory.getWzFile().getWzIv(), wzDirectory.getWzFile());
                    wzDirectory.getDirectories().put(data.getName(), obj);
                    node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), WzNodeType.WZ_DIRECTORY, null, obj);
                    pNode.addChild(node);
                    nodeCache.put(node.getId(), node);
                } else if (data.getType() == WzNodeType.IMAGE) {
                    for (WzImage image : wzDirectory.getImages().values()) {
                        if (image.getName().equalsIgnoreCase(data.getName())) {
                            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                        }
                    }
                    WzImage img = new WzImage(data.getName(), wzDirectory.getReader());
                    img.setParsed(true);
                    img.setChanged(true);
                    wzDirectory.getImages().put(data.getName(), img);
                    node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), WzNodeType.IMAGE, null, img);
                    pNode.addChild(node);
                    nodeCache.put(node.getId(), node);
                } else {
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "只能添加wz目录和image类型的节点");
                }
            }
            case WzImage img -> {
                img.getProperties().forEach(prop -> {
                    if (prop.getName().equalsIgnoreCase(data.getName())) {
                        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                });
                WzImageProperty obj = genNewImageProperty(img, img, data);
                img.getProperties().add(obj);
                node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), data.getType(), null, obj);
                pNode.addChild(node);
                nodeCache.put(node.getId(), node);
                img.setChanged(true);
            }
            case WzListProperty list -> {
                list.getProperties().forEach(prop -> {
                    if (prop.getName().equalsIgnoreCase(data.getName())) {
                        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                });
                WzImageProperty obj = genNewImageProperty(getParentImg(list), list, data);
                list.getProperties().add(obj);
                node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), data.getType(), null, obj);
                pNode.addChild(node);
                nodeCache.put(node.getId(), node);
                Objects.requireNonNull(getParentImg(list)).setChanged(true);
            }
            case WzCanvasProperty canvas -> {
                canvas.getProperties().forEach(prop -> {
                    if (prop.getName().equalsIgnoreCase(data.getName())) {
                        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                });
                WzImageProperty obj = genNewImageProperty(getParentImg(canvas), canvas, data);
                canvas.getProperties().add(obj);
                node = new WzNode(pNode, nextId.getAndIncrement(), data.getName(), data.getType(), null, obj);
                pNode.addChild(node);
                nodeCache.put(node.getId(), node);
                Objects.requireNonNull(getParentImg(canvas)).setChanged(true);
            }
            case WzConvexProperty convex -> {
                convex.getProperties().forEach(prop -> {
                    if (prop.getName().equalsIgnoreCase(data.getName())) {
                        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "已有相同节点名了");
                    }
                });
                WzImageProperty obj = genNewImageProperty(getParentImg(convex), convex, data);
                convex.getProperties().add(obj);
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
                    wzFile.getWzDirectory().getDirectories().remove(obj.getName());
                } else if (obj instanceof WzImage) {
                    wzFile.getWzDirectory().getImages().remove(obj.getName());
                }
            }
            case WzDirectory wzDir -> {
                if (obj instanceof WzDirectory) {
                    wzDir.getDirectories().remove(obj.getName());
                } else if (obj instanceof WzImage) {
                    wzDir.getImages().remove(obj.getName());
                }
            }
            case WzImage img -> {
                img.getProperties().remove((WzImageProperty) obj);
                img.setChanged(true);
            }
            case WzListProperty list -> {
                list.getProperties().remove((WzImageProperty) obj);
                Objects.requireNonNull(getParentImg(list)).setChanged(true);
            }
            case WzCanvasProperty canvas -> {
                canvas.getProperties().remove((WzImageProperty) obj);
                Objects.requireNonNull(getParentImg(canvas)).setChanged(true);
            }
            case WzConvexProperty convex -> {
                convex.getProperties().remove((WzImageProperty) obj);
                Objects.requireNonNull(getParentImg(convex)).setChanged(true);
            }
            default -> throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "不受支持的类型");
        }

        Set<Integer> ids = new HashSet<>();
        pNode.removeChild(ids, id);
        ids.forEach(nodeCache::remove);
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
            case WzFile wzFile -> wzFile.save(wzFile.getPath());
            case WzImage wzImage -> wzImage.save(Path.of(wzImage.getPath()));
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
            wzFile.save(wzFile.getPath());
        } else if (obj instanceof WzImage wzImage) {
            wzImage.save(Path.of(wzImage.getPath()));
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
            file.parse();

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
            file.parse();

            file.exportFileToXml(basePath, indent);
        } else if (obj instanceof WzImage image) {
            image.exportToXml(basePath.resolve(image.getName() + ".xml"), indent);
        }
    }

    public void fixOutLinkApi(int id) {
        fixOutlink(id);
        lastCavPath = null;
        cavWzFiles.clear();
        log.debug("结束");
    }

    public void fixOutlink(int id) {
        WzNode node = nodeCache.get(id);
        if (node == null) throw new BizException(ExceptionEnum.NOT_FOUND);

        if (node.getType() == WzNodeType.FOLDER ||
                node.getType() == WzNodeType.WZ ||
                node.getType() == WzNodeType.WZ_DIRECTORY ||
                node.getType() == WzNodeType.IMAGE ||
                node.getType() == WzNodeType.IMAGE_LIST) {
            List<WzNode> children = getNode(node.getId());
            children.forEach(child -> fixOutlink(child.getId()));
        } else if (node.getType() == WzNodeType.IMAGE_CANVAS) {
            WzCanvasProperty canvas = (WzCanvasProperty) node.getWzObject();

            List<String> outlinkPaths = getOutlinkPaths(canvas.getProperties());
            if (outlinkPaths.isEmpty()) {
                log.warn("不存在 outlink 节点或者 outlink 路径中找不到 _Canvas");
                return;
            }
            WzImage wzImage = getParentImg(canvas);
            if (wzImage == null) return;
            WzDirectory wzDirectory = (WzDirectory) wzImage.getParent();
            if (wzDirectory == null) return;
            WzFile wzFile = wzDirectory.getWzFile();
            if (wzFile == null) return;

            File file = new File(wzFile.getPath());
            String dirPath = file.getParent();
            Path cavFolderPath = Path.of(dirPath, "_Canvas");
            loadCavWzFiles(cavFolderPath, wzFile.getFileVersion(), wzFile.getWzIv(), wzFile.getUserKey());

            fixOutlink(node);
        }
    }

    private void loadCavWzFiles(Path cavFolderPath, short fileVersion, byte[] iv, byte[] key) {
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
            cavWzFile.parse();
        }
    }

    private void fixOutlink(WzNode node) {
        WzCanvasProperty canvas = (WzCanvasProperty) node.getWzObject();

        List<String> outlinkPaths = getOutlinkPaths(canvas.getProperties());
        if (outlinkPaths.isEmpty()) {
            log.warn("不存在 outlink 节点或者 outlink 路径中找不到 _Canvas");
            return;
        }

        WzImage wzImage = getParentImg(canvas);

        // 搜索 Canvas
        WzObject nodeReady;
        for (WzObject obj : cavWzFiles) {
            boolean inImg = false;
            obj = ((WzFile) obj).getWzDirectory();
            for (String p : outlinkPaths) {
                nodeReady = null;
                if (!inImg) {
                    if (p.endsWith(".img")) {
                        nodeReady = ((WzDirectory) obj).getImages().get(p);
                        inImg = true;
                    } else {
                        nodeReady = ((WzDirectory) obj).getDirectories().get(p);
                    }
                } else {
                    if (obj instanceof WzImage img) {
                        img.parse();
                        for (WzImageProperty prop : img.getProperties()) {
                            if (prop.getName().equalsIgnoreCase(p)) {
                                nodeReady = prop;
                                break;
                            }
                        }
                    } else if (obj instanceof WzListProperty listProp) {
                        for (WzImageProperty prop : listProp.getProperties()) {
                            if (prop.getName().equalsIgnoreCase(p)) {
                                nodeReady = prop;
                                break;
                            }
                        }
                    }
                }
                obj = nodeReady;
                if (obj == null) break;
            }

            if (obj != null) {
                if (obj instanceof WzCanvasProperty n) {
                    String base64 = n.getPng().getBase64();
                    canvas.getPng().setPng(base64, wzImage.getReader().getWzKey(), n.getPng().getPngFormat());
                    break;
                }
                log.warn("找到了节点，但不是 WzCanvas 类型");
            }
        }
    }

    private List<String> getOutlinkPaths(List<WzImageProperty> properties) {
        String outlink = "";
        for (WzImageProperty prop : properties) {
            if (prop.getName().equalsIgnoreCase("_outlink")) {
                outlink = ((WzStringProperty) prop).getValue();
                break;
            }
        }

        if (outlink.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> outlinkPaths = List.of(outlink.split("/"));
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
        } else if (wz instanceof WzImage wzImage && wzImage.getPath() != null) {
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
            toFile.parse();
            fromFile.parse();
            toFile.getWzDirectory().getDirectories().forEach((s, toDir) -> localization(fromFile.getWzDirectory().getDirectories().get(s), toDir));
            toFile.getWzDirectory().getImages().forEach((s, toImage) -> localization(fromFile.getWzDirectory().getImages().get(s), toImage));
        } else if (to instanceof WzDirectory toDirectory && from instanceof WzDirectory fromDirectory) {
            toDirectory.getDirectories().forEach((s, toDir) -> localization(fromDirectory.getDirectories().get(s), toDir));
            toDirectory.getImages().forEach((s, toImage) -> localization(fromDirectory.getImages().get(s), toImage));
        } else if (to instanceof WzImage toImage && from instanceof WzImage fromImage) {
            toImage.parse();
            fromImage.parse();
            toImage.getProperties().forEach(img -> localization(fromImage.get(img.getName()), img));
        } else if (to instanceof WzListProperty toListProperty && from instanceof WzListProperty fromList) {
            toListProperty.getProperties().forEach(prop -> localization(fromList.get(prop.getName()), prop));
        } else if (to instanceof WzStringProperty toString && from instanceof WzStringProperty fromString) {
            String fromValue = fromString.getValue();
            if (fromValue != null && !isChinese(toString.getValue()) && isChinese(fromValue)) {
                toString.setValue(fromValue);
            }
        }
    }

    private boolean isChinese(String str) {
        return !str.matches(".*[\\uAC00-\\uD7A3].*")  // 不能有韩文
                && str.matches(".*[\\u4e00-\\u9fa5].*");  // 有中文字符
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

    private byte[] getWzKey(WzObject wzObject) {
        WzImage wzImage = getParentImg(wzObject);
        if (wzImage == null) return null;
        return wzImage.getReader().getWzKey();
    }

    private boolean canAddNodeType(WzObject object, WzNodeType targetType) {
        if (object instanceof WzFile || object instanceof WzDirectory)
            return targetType == WzNodeType.WZ_DIRECTORY || targetType == WzNodeType.IMAGE;
        else
            return object instanceof WzImage || object instanceof WzListProperty || object instanceof WzCanvasProperty || object instanceof WzConvexProperty;
    }

    private WzImageProperty genNewImageProperty(WzImage img, WzObject parent, WzNodeValueDto data) {
        if (data.getType() == WzNodeType.IMAGE_CANVAS) {
            WzCanvasProperty canvasProp = WzCanvasProperty.builder().name(data.getName()).parent(parent).build();
            WzPngProperty png = WzPngProperty.builder().name(data.getName()).parent(canvasProp).build();
            png.setPng(data.getPng(), img.getReader().getWzKey(), data.getPngFormat());
            canvasProp.setPng(png);
            return canvasProp;
        } else if (data.getType() == WzNodeType.IMAGE_CONVEX) {
            return WzConvexProperty.builder().name(data.getName()).parent(parent).build();
        } else if (data.getType() == WzNodeType.IMAGE_DOUBLE) {
            return WzDoubleProperty.builder().name(data.getName()).parent(parent).value(Double.parseDouble(data.getValue())).build();
        } else if (data.getType() == WzNodeType.IMAGE_FLOAT) {
            return WzFloatProperty.builder().name(data.getName()).parent(parent).value(Float.parseFloat(data.getValue())).build();
        } else if (data.getType() == WzNodeType.IMAGE_INT) {
            return WzIntProperty.builder().name(data.getName()).parent(parent).value(Integer.parseInt(data.getValue())).build();
        } else if (data.getType() == WzNodeType.IMAGE_LIST) {
            return WzListProperty.builder().name(data.getName()).parent(parent).build();
        } else if (data.getType() == WzNodeType.IMAGE_LONG) {
            return WzLongProperty.builder().name(data.getName()).parent(parent).value(Long.parseLong(data.getValue())).build();
        } else if (data.getType() == WzNodeType.IMAGE_NULL) {
            return WzNullProperty.builder().name(data.getName()).parent(parent).build();
        } else if (data.getType() == WzNodeType.IMAGE_SHORT) {
            return WzShortProperty.builder().name(data.getName()).parent(parent).value(Short.parseShort(data.getValue())).build();
        } else if (data.getType() == WzNodeType.IMAGE_SOUND) {
            WzSoundProperty sound = WzSoundProperty.builder().name(data.getName()).parent(parent).build();
            sound.setSound(data.getMp3(), img.getReader().getWzKey());
            return sound;
        } else if (data.getType() == WzNodeType.IMAGE_STRING) {
            return WzStringProperty.builder().name(data.getName()).parent(parent).value(data.getValue()).build();
        } else if (data.getType() == WzNodeType.IMAGE_UOL) {
            return WzUOLProperty.builder().name(data.getName()).parent(parent).uol(data.getValue()).build();
        } else if (data.getType() == WzNodeType.IMAGE_VECTOR) {
            WzVectorProperty vecProp = WzVectorProperty.builder().name(data.getName()).parent(parent).build();
            WzIntProperty xProp = WzIntProperty.builder().name("X").value(data.getX()).parent(vecProp).build();
            WzIntProperty yProp = WzIntProperty.builder().name("Y").value(data.getY()).parent(vecProp).build();
            vecProp.setX(xProp);
            vecProp.setY(yProp);
            return vecProp;
        }
        throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "节点生成失败");
    }
}
