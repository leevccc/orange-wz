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
    private final String filePath;
    private final WzChildrenFolder children = new WzChildrenFolder();
    @Getter
    private final byte[] iv;
    @Getter
    private final byte[] key;

    public WzFolder(String filePath, byte[] iv, byte[] key) {
        super(Path.of(filePath).getFileName().toString(), WzType.FOLDER, null);
        this.filePath = filePath;
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
                    children.add(new WzFolder(pathStr, iv, key));
                } else if (filename.endsWith(".wz")) {
                    children.add(new WzFile(pathStr, (short) -1, iv, key).getWzDirectory());
                } else if (filename.endsWith(".img")) {
                    children.add(new WzImageFile(filename, pathStr, iv, key));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
