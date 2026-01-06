package orange.wz.provider.tools;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

@Slf4j
public final class FileTool {
    /**
     * 确保目录是一个新目录（空）
     */
    public static void createNewDirectory(Path p) throws IOException {
        if (Files.exists(p)) {
            if (!Files.isDirectory(p)) {
                throw new RuntimeException("路径已存在但不是目录: " + p);
            }

            try (Stream<Path> stream = Files.list(p)) {
                if (stream.findAny().isPresent()) {
                    throw new RuntimeException("目录已存在且不为空: " + p);
                }
            }
        } else {
            Files.createDirectories(p);
        }
    }

    /**
     * 确保目录存在
     */
    public static void createDirectory(Path p) throws IOException {
        if (Files.exists(p)) {
            if (!Files.isDirectory(p)) {
                throw new RuntimeException("路径已存在但不是目录: " + p);
            }
        } else {
            Files.createDirectories(p);
        }
    }

    public static boolean ensureFileExists(Path path) {
        // 如果文件已经存在，直接返回 true
        if (Files.exists(path)) {
            return true;
        }

        try {
            // 确保父目录存在
            if (path.getParent() != null) {
                createDirectory(path.getParent());
            }

            // 创建空白文件
            Files.createFile(path);
            return true;
        } catch (IOException e) {
            log.error("创建文件失败 {}", e.getMessage());
            return false;
        }
    }

    public static boolean saveFile(Path path, byte[] bytes) {
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            fos.write(bytes);
            return true;
        } catch (IOException ex) {
            log.error("保存文件失败 {} : {}", path, ex.getMessage());
            return false;
        }
    }

    public static byte[] readFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return in.readAllBytes();
        } catch (IOException ex) {
            log.error("读取文件失败 {} : {}", path, ex.getMessage());
            return null;
        }
    }

    public static boolean deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            log.error("删除旧文件失败: {}", path, e);
            return false;
        }
    }

    public static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {

            @Override
            @NonNull
            public FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            @NonNull
            public FileVisitResult postVisitDirectory(@NonNull Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static String safeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed";
        }

        // 替换非法字符为下划线
        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 去除控制字符
        safe = safe.replaceAll("[\\p{Cntrl}]", "");

        // 去除结尾的空格和点（Windows 不允许）
        safe = safe.replaceAll("[\\.\\s]+$", "");

        // 处理 Windows 保留名
        String upper = safe.toUpperCase();
        if (upper.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
            safe = "_" + safe;
        }

        return safe.isEmpty() ? "unnamed" : safe;
    }
}
