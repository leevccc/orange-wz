package orange.wz.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class FileUtils {
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
}
