package orange.wz.provider;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.tools.WzChildrenFolder;
import orange.wz.provider.tools.WzType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class WzFolder extends WzObject {
    @Getter
    private final String filePath;
    private final WzChildrenFolder children = new WzChildrenFolder();
    @Getter
    private final String keyBoxName;
    @Getter
    private final byte[] iv;
    @Getter
    private final byte[] key;

    public WzFolder(String filePath, String keyBoxName, byte[] iv, byte[] key) {
        super(Path.of(filePath).getFileName().toString(), WzType.FOLDER, null);
        this.filePath = filePath;
        this.keyBoxName = keyBoxName;
        this.iv = iv;
        this.key = key;
    }

    public List<WzObject> getChildren() {
        List<WzObject> result = children.getAllChildren();
        if (result.isEmpty()) {
            loadFolder();
        }

        return children.getAllChildren();
    }

    public void add(WzObject wzObject) {
        if (wzObject instanceof WzFolder wzFolder) {
            add(wzFolder);
        } else if (wzObject instanceof WzDirectory wzDirectory) {
            add(wzDirectory);
        } else if (wzObject instanceof WzImageFile wzImg) {
            add(wzImg);
        } else if (wzObject instanceof WzXmlFile wzXml) {
            add(wzXml);
        }
    }

    public void add(WzFolder folder) {
        children.add(folder);
    }

    public void add(WzDirectory wzFile) {
        children.add(wzFile);
    }

    public void add(WzImageFile wzImage) {
        children.add(wzImage);
    }

    public void add(WzXmlFile wzXmlFile) {
        children.add(wzXmlFile);
    }

    public boolean remove(WzObject wzObject) {
        if (wzObject instanceof WzFolder) {
            return children.removeFolder(wzObject.getName());
        } else if (wzObject instanceof WzDirectory) {
            return children.removeWzFile(wzObject.getName());
        } else if (wzObject instanceof WzImageFile) {
            return children.removeWzImageFile(wzObject.getName());
        } else if (wzObject instanceof WzXmlFile) {
            return children.removeWzXmlFile(wzObject.getName());
        }

        return false;
    }

    public void loadFolder() {
        Path folderPath = Path.of(filePath);
        log.debug("加载目录: {}", folderPath);
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new RuntimeException("文件或目录不存在！");
        }

        try (var paths = Files.list(folderPath)) {
            paths.forEach(path -> {
                String filename = path.getFileName().toString();
                String pathStr = path.toAbsolutePath().toString();
                if (Files.isDirectory(path)) {
                    children.add(new WzFolder(pathStr, keyBoxName, iv, key));
                } else if (filename.endsWith("List.wz")) {
                    log.info("展开目录跳过 List.wz 文件");
                } else if (filename.endsWith(".wz")) {
                    children.add(new WzFile(pathStr, (short) -1, keyBoxName, iv, key).getWzDirectory());
                } else if (filename.endsWith(".img")) {
                    children.add(new WzImageFile(filename, pathStr, keyBoxName, iv, key));
                } else if (filename.endsWith(".xml")) {
                    children.add(new WzXmlFile(filename, pathStr, keyBoxName, iv, key));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
