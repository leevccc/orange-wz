package orange.wz.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import orange.wz.manager.ServerManager;
import orange.wz.provider.WzXmlFile;
import orange.wz.provider.tools.wzkey.WzKey;
import orange.wz.provider.tools.wzkey.WzKeyStorage;

public final class Xml2ImgCli {

    private static final String OPT_KEY_ID = "key-id";
    private static final String OPT_JOBS = "jobs";
    private static final int DEFAULT_JOBS = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 8));

    private Xml2ImgCli() {
    }

    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } catch (Exception e) {
            System.err.println("[错误] " + e.getMessage());
            code = 1;
        }
        System.exit(code);
    }

    private static int run(String[] args) throws IOException {
        if (args.length == 0 || isHelp(args[0])) {
            printHelp();
            return 0;
        }
        return runExportImgFolder(args);
    }

    private static int runExportImgFolder(String[] args) throws IOException {
        initCliRuntime();

        Map<String, String> opts = parseOptions(args);
        if (opts.containsKey("help")) {
            printHelp();
            return 0;
        }

        String inputStr = required(opts, "input");
        String outputStr = required(opts, "output");
        int jobs = resolveJobs(opts);

        Path input = Path.of(inputStr).toAbsolutePath().normalize();
        Path output = Path.of(outputStr).toAbsolutePath().normalize();
        if (!Files.exists(input) || !Files.isDirectory(input)) {
            throw new IllegalArgumentException("输入目录不存在: " + input);
        }

        boolean overwrite = opts.containsKey("overwrite");
        if (Files.exists(output)) {
            if (!overwrite) {
                throw new IllegalArgumentException("输出目录已存在，请加 --overwrite 覆盖: " + output);
            }
            deleteDirectory(output);
        }
        Files.createDirectories(output);

        WzKey key = resolveKeyById(opts);
        if (key == null) {
            throw new IllegalArgumentException("找不到密钥，请使用 --key-id");
        }

        // [xml转img数量, 直接复制img数量, 跳过文件数量]
        int[] counters = new int[]{0, 0, 0};
        Map<Path, ExportTask> tasksByOutput = new LinkedHashMap<>();
        collectExportTasks(input, output, tasksByOutput, counters);
        List<ExportTask> tasks = new ArrayList<>(tasksByOutput.values());

        System.out.println("[信息] 输入目录=" + input);
        System.out.println("[信息] 输出目录=" + output);
        System.out.println("[信息] 密钥=" + key.getName() + " (id=" + key.getId() + ")");
        System.out.println("[信息] 并行线程=" + jobs);
        System.out.println("[信息] 待处理任务=" + tasks.size());

        executeTasks(tasks, key, jobs, counters);

        System.out.println("[信息] 新生成 img 数量=" + counters[0]);
        System.out.println("[信息] 直接复制 img 数量=" + counters[1]);
        System.out.println("[信息] 跳过文件数量=" + counters[2]);
        System.out.println("[完成] " + output);
        return 0;
    }

    private static void collectExportTasks(Path src, Path out, Map<Path, ExportTask> tasksByOutput, int[] counters) throws IOException {
        try (Stream<Path> stream = Files.list(src)
                .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(p)) {
                    String folderName = normalizeExportFolderName(p.getFileName().toString());
                    Path subOut = out.resolve(folderName);
                    Files.createDirectories(subOut);
                    collectExportTasks(p, subOut, tasksByOutput, counters);
                    continue;
                }

                String name = p.getFileName().toString();
                if (name.endsWith(".img.xml")) {
                    String imgName = name.substring(0, name.length() - 4);
                    registerTask(tasksByOutput, new ExportTask(p, out.resolve(imgName), imgName, true));
                    continue;
                }

                if (name.endsWith(".xml")) {
                    String imgName = name.substring(0, name.length() - 4);
                    registerTask(tasksByOutput, new ExportTask(p, out.resolve(imgName), imgName, true));
                    continue;
                }

                if (name.endsWith(".img")) {
                    registerTask(tasksByOutput, new ExportTask(p, out.resolve(name), null, false));
                    continue;
                }

                counters[2]++;
            }
        }
    }

    private static void registerTask(Map<Path, ExportTask> tasksByOutput, ExportTask task) {
        Path key = task.target().toAbsolutePath().normalize();
        ExportTask exists = tasksByOutput.putIfAbsent(key, task);
        if (exists != null) {
            throw new IllegalArgumentException("输出路径冲突: " + key + "\n  来源1=" + exists.source() + "\n  来源2=" + task.source());
        }
    }

    private static void executeTasks(List<ExportTask> tasks, WzKey key, int jobs, int[] counters) {
        AtomicInteger xmlCount = new AtomicInteger();
        AtomicInteger copyCount = new AtomicInteger();

        if (jobs <= 1 || tasks.size() <= 1) {
            for (ExportTask task : tasks) {
                runTask(task, key, xmlCount, copyCount);
            }
        } else {
            ExecutorService executor = Executors.newFixedThreadPool(jobs);
            List<Future<?>> futures = new ArrayList<>(tasks.size());
            try {
                for (ExportTask task : tasks) {
                    futures.add(executor.submit(() -> runTask(task, key, xmlCount, copyCount)));
                }
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("并行导出被中断", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("并行导出失败: " + cause.getMessage(), cause);
            } finally {
                executor.shutdownNow();
            }
        }

        counters[0] = xmlCount.get();
        counters[1] = copyCount.get();
    }

    private static void runTask(ExportTask task, WzKey key, AtomicInteger xmlCount, AtomicInteger copyCount) {
        try {
            if (task.xmlToImg()) {
                saveXmlAsImg(task.source(), task.target(), task.imgName(), key);
                xmlCount.incrementAndGet();
            } else {
                Files.copy(task.source(), task.target());
                copyCount.incrementAndGet();
            }
        } catch (IOException e) {
            throw new IllegalStateException("文件处理失败: " + task.source() + " -> " + task.target(), e);
        }
    }

    // 去掉以.wz结尾的文件夹后缀，方便直接导出成img端能启动的目录结构
    private static String normalizeExportFolderName(String folderName) {
        if (folderName.toLowerCase(Locale.ROOT).endsWith(".wz")) {
            return folderName.substring(0, folderName.length() - 3);
        }
        return folderName;
    }

    private static void saveXmlAsImg(Path xmlPath, Path outImgPath, String imgName, WzKey key) {
        WzXmlFile xml = new WzXmlFile(imgName, xmlPath.toString(), key.getName(), key.getIv(), key.getUserKey());
        if (!xml.parse()) {
            throw new IllegalStateException("xml 解析失败: " + xmlPath);
        }
        if (!xml.save(outImgPath)) {
            throw new IllegalStateException("img 保存失败: " + outImgPath);
        }
    }

    // 递归删除目录（用于--overwrite）
    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException io) {
                throw io;
            }
            throw ex;
        }
    }

    // 暂时只支持按key-id传入密钥，后面考虑加上自定义密钥文件的方式
    private static WzKey resolveKeyById(Map<String, String> opts) {
        String idValue = opts.get(OPT_KEY_ID);
        if (idValue == null || idValue.isBlank()) {
            return null;
        }

        try {
            int id = Integer.parseInt(idValue);
            return new WzKeyStorage().findById(id);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("无效的密钥 id: " + idValue);
        }
    }

    private static int resolveJobs(Map<String, String> opts) {
        String value = opts.get(OPT_JOBS);
        if (value == null || value.isBlank()) {
            return DEFAULT_JOBS;
        }

        try {
            int jobs = Integer.parseInt(value);
            if (jobs < 1 || jobs > 64) {
                throw new IllegalArgumentException("--jobs 取值范围必须是 1~64");
            }
            return jobs;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("无效的 --jobs 值: " + value);
        }
    }

    // CLI直接运行时不会经过Spring注入。这里手动初始化slog。
    private static void initCliRuntime() {
        String fallback = "logging.pattern.dateformat=yyyy-MM-dd HH:mm:ss.SSS";
        String slog = fallback;

        try (InputStream in = Xml2ImgCli.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                slog = p.getProperty("server.logging", fallback);
            }
        } catch (Exception ignored) {
            slog = fallback;
        }

        new ServerManager().setSlog(slog);
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (!token.startsWith("--")) {
                throw new IllegalArgumentException("非法参数: " + token);
            }

            String key = token.substring(2);
            if ("overwrite".equals(key) || "help".equals(key)) {
                opts.put(key, "true");
                continue;
            }

            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("参数缺少值: --" + key);
            }
            opts.put(key, args[++i]);
        }
        return opts;
    }

    private static String required(Map<String, String> opts, String key) {
        String value = opts.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少必填参数: --" + key);
        }
        return value;
    }

    private static boolean isHelp(String arg) {
        return "-h".equals(arg) || "--help".equals(arg) || "help".equalsIgnoreCase(arg);
    }

    private static void printHelp() {
        System.out.println("xml2img-cli");
        System.out.println("将xml目录导出为.img目录");
        System.out.println();
        System.out.println("用法:");
        System.out.println("  xml2img-cli --input <dir> --output <dir> --key-id <int> [--jobs <int>] [--overwrite]");
        System.out.println("  xml2img-cli --help");
        System.out.println();
        System.out.println("参数说明:");
        System.out.println("  --jobs <int>    并行线程数，范围 1~64，默认 " + DEFAULT_JOBS);
    }

    private record ExportTask(Path source, Path target, String imgName, boolean xmlToImg) {

    }
}
