package orange.wz;

import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzXmlFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static orange.wz.provider.WzAESConstant.DEFAULT_KEY;
import static orange.wz.provider.WzAESConstant.WZ_GMS_IV;

@Slf4j
public class Xml2Img {

    private static final String INPUT_DIR = "D:\\Code\\maplefire\\maplefire-reborn-game-data";
    private static final String OUTPUT_DIR = "D:\\MapleStory\\MaplefireReborn\\Data3";
    private static final AtomicInteger counter = new AtomicInteger();
    private static final Set<Path> createdDirs = ConcurrentHashMap.newKeySet();

    private static final ConcurrentHashMap<String, FileCache> CACHE = new ConcurrentHashMap<>();
    private static final Path CACHE_FILE = Paths.get(OUTPUT_DIR, "cache.txt");


    // ForkJoin线程数（建议 核数 * 2）
    private static final int PARALLELISM = Math.min(Runtime.getRuntime().availableProcessors() * 2, 20);

    // 每批处理多少文件
    private static final int CHUNK_SIZE = 100;

    public static void main(String[] args) throws Exception {

        long start = System.currentTimeMillis();

        log.info("Start");

        loadCache();

        ForkJoinPool pool = new ForkJoinPool(PARALLELISM);

        Path root = Paths.get(INPUT_DIR);

        // 最终需要处理的 img.xml
        Set<Path> changedFiles = ConcurrentHashMap.newKeySet();

        // 已经被 media 标记过的 xxx.img.xml
        Set<String> mediaChangedNames = ConcurrentHashMap.newKeySet();

        /*
         * 第一阶段：
         * 扫描 media/xxx.img
         *
         * 只要里面任意 png 真正变化，
         * 就标记 xxx.img.xml
         */
        try (Stream<Path> stream = Files.walk(root)) {

            stream.parallel()
                    .filter(Files::isDirectory)
                    .filter(Xml2Img::isMediaImgDirectory)
                    .forEach(imgDir -> {

                        try (Stream<Path> files = Files.walk(imgDir)) {

                            AtomicBoolean changed = new AtomicBoolean(false);

                            files.filter(Files::isRegularFile)
                                    .filter(p -> p.toString().endsWith(".png"))
                                    .forEach(p -> {

                                        if (hasFileReallyChanged(p)) {
                                            changed.set(true);
                                        }
                                    });

                            if (changed.get()) {

                                String imgXmlName =
                                        imgDir.getFileName().toString() + ".xml";

                                mediaChangedNames.add(imgXmlName);

                                Path imgXml = imgDir.getParent()
                                        .getParent()
                                        .resolve(imgXmlName);

                                changedFiles.add(imgXml);

                                log.info("Media changed: {}", imgXml);
                            }

                        } catch (Exception e) {
                            log.error("Scan media failed: {}", imgDir, e);
                        }
                    });
        }

        /*
         * 第二阶段：
         * 扫描普通 .img.xml
         */
        try (Stream<Path> stream = Files.walk(root)) {

            stream.parallel()
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".img.xml"))
                    .forEach(path -> {

                        String fileName =
                                path.getFileName().toString();

                        // 已经被 media 标记过
                        if (mediaChangedNames.contains(fileName)) {
                            return;
                        }

                        if (hasFileReallyChanged(path)) {

                            changedFiles.add(path);

                            log.info("XML changed: {}", path);
                        }
                    });
        }

        List<Path> allFiles = new ArrayList<>(changedFiles);

        log.info("Total changed files: {}", allFiles.size());

        List<List<Path>> chunks =
                chunk(allFiles, CHUNK_SIZE);

        pool.submit(() -> chunks.parallelStream().forEach(batch -> { batch.forEach(Xml2Img::processFile); }) ).get();

        long end = System.currentTimeMillis();

        saveCache();

        log.info("Done in {} ms", end - start);
    }

    /*
     * 判断是否是:
     *
     * A/media/001.img
     */
    private static boolean isMediaImgDirectory(Path path) {

        Path parent = path.getParent();

        if (parent == null) {
            return false;
        }

        return parent.getFileName().toString().equals("media") && path.getFileName().toString().endsWith(".img");
    }

    /*
     * 真正判断文件是否变化
     *
     * 逻辑：
     *
     * 1. 先比较 size + mtime
     * 2. 如果不同
     *    再比较 hash
     *
     * 这样：
     *
     * - 普通启动极快
     * - git pull 不会误判
     */
    private static boolean hasFileReallyChanged(Path path) {

        try {

            long size = Files.size(path);

            long modified = Files.getLastModifiedTime(path)
                            .toMillis();

            String quick = size + "_" + modified;

            FileCache old = CACHE.get(path.toString());

            /*
             * size + mtime 没变
             * 直接认为没变
             */
            if (old != null && old.quick.equals(quick)) {
                return false;
            }

            /*
             * 计算真正 hash
             */
            String hash = fastHash(path);

            /*
             * hash 没变
             * 说明只是 git pull
             */
            if (old != null
                    && old.hash.equals(hash)) {

                old.quick = quick;

                return false;
            }

            /*
             * 真正变化
             */
            CACHE.put(path.toString(),
                    new FileCache(quick, hash));

            return true;

        } catch (Exception e) {

            log.error("Check file failed: {}", path, e);

            return true;
        }
    }

    /*
     * 小文件场景下足够快
     *
     * 如果以后还嫌慢：
     * 可以换 xxHash
     */
    private static String fastHash(Path path)
            throws Exception {

        MessageDigest md =
                MessageDigest.getInstance("MD5");

        try (InputStream in =
                     Files.newInputStream(path)) {

            byte[] buffer = new byte[8192];

            int len;

            while ((len = in.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
        }

        byte[] digest = md.digest();

        StringBuilder sb = new StringBuilder();

        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private static class FileCache {

        String quick;

        String hash;

        FileCache(String quick, String hash) {
            this.quick = quick;
            this.hash = hash;
        }
    }

    private static void processFile(Path path) {
        try {
            String filepath = path.toString();

            String filename = path.getFileName().toString()
                    .replace(".xml", "");

            Path relative = Paths.get(INPUT_DIR).relativize(path);

            Path outputPath = Paths.get(OUTPUT_DIR, relative.toString())
                    .normalize();

            String s = outputPath.toString();
            s = s.substring(0, s.length() - 8) + ".img";
            outputPath = Path.of(s);

            Path parent = outputPath.getParent();
            if (createdDirs.add(parent)) {
                Files.createDirectories(parent);
            }

            WzXmlFile xmlFile = new WzXmlFile(filename, filepath, "", WZ_GMS_IV, DEFAULT_KEY);
            xmlFile.saveFromXml(outputPath);

            if (counter.incrementAndGet() % 1000 == 0) {
                log.info("Processed: {}", counter.get());
            }
        } catch (Exception e) {
            System.err.println("Failed: " + path);
            e.printStackTrace();
        }
    }

    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        int total = list.size();

        for (int i = 0; i < total; i += size) {
            result.add(list.subList(i, Math.min(total, i + size)));
        }

        return result;
    }

    private static void loadCache() {

        if (!Files.exists(CACHE_FILE)) {
            return;
        }

        long start = System.currentTimeMillis();

        try (BufferedReader reader =
                     Files.newBufferedReader(CACHE_FILE)) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] split =
                        line.split("\\|", 3);

                if (split.length != 3) {
                    continue;
                }

                String path = split[0];
                String quick = split[1];
                String hash = split[2];

                CACHE.put(path,
                        new FileCache(quick, hash));
            }

            log.info("Cache loaded: {} entries ({} ms)",
                    CACHE.size(),
                    System.currentTimeMillis() - start);

        } catch (Exception e) {

            log.error("Load cache failed", e);
        }
    }

    private static void saveCache() {

        long start = System.currentTimeMillis();

        try {

            Files.createDirectories(
                    CACHE_FILE.getParent());

            try (BufferedWriter writer =
                         Files.newBufferedWriter(
                                 CACHE_FILE,
                                 StandardOpenOption.CREATE,
                                 StandardOpenOption.TRUNCATE_EXISTING,
                                 StandardOpenOption.WRITE)) {

                for (Map.Entry<String, FileCache> e
                        : CACHE.entrySet()) {

                    FileCache c = e.getValue();

                    writer.write(
                            e.getKey()
                                    + "|"
                                    + c.quick
                                    + "|"
                                    + c.hash
                    );

                    writer.newLine();
                }
            }

            log.info("Cache saved: {} entries ({} ms)",
                    CACHE.size(),
                    System.currentTimeMillis() - start);

        } catch (Exception e) {

            log.error("Save cache failed", e);
        }
    }
}
