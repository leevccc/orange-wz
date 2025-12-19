package orange.wz;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public final class UpdateVersionNumber {

    private static final Path PROPERTIES_PATH =
            Paths.get("src/main/resources/application.properties");

    public static void main(final String[] args) throws Exception {

        // 1. 最新 tag 作为当前版本
        String latestTag = getLatestTag();
        System.out.println("Latest tag: " + latestTag);

        Version current = Version.parse(latestTag);

        // 2. 读取 tag 到 HEAD 的 commit
        List<String> commits = getCommitsSinceTag(latestTag);
        if (commits.isEmpty()) {
            System.out.println("No new commits since last tag.");
            return;
        }

        // 3. 计算新版本
        Version next = calculateNextVersion(current, commits);
        System.out.println("Next version: " + next);

        // 4. 更新 application.properties
        updateProperties(next);

        // 5. 提交并打 tag
        gitCommit(next);
        gitTag(next);

        System.out.println("✔ Version update completed.");
    }

    /* ================= Git ================= */

    private static String getLatestTag() throws Exception {
        return exec("git", "describe", "--tags", "--abbrev=0").trim();
    }

    private static List<String> getCommitsSinceTag(String tag) throws Exception {
        String out = exec(
                "git", "log", tag + "..HEAD", "--pretty=format:%s"
        );
        return Arrays.stream(out.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static void gitCommit(Version version) throws Exception {
        exec("git", "add", PROPERTIES_PATH.toString());
        exec("git", "commit", "-m", "Version: " + version);
    }

    private static void gitTag(Version version) throws Exception {
        exec("git", "tag", version.toString());
    }

    /* ================= Version ================= */

    private static Version calculateNextVersion(
            Version current,
            List<String> commits
    ) {
        int fixCount = 0;
        int minorCount = 0;

        int sp =0;
        for (String msg : commits) {
            if (msg.startsWith("Fix")) {
                fixCount++;
            } else if (
                    msg.startsWith("Feat")
                            || msg.startsWith("Refactor")
                            || msg.startsWith("Chore")
            ) {
                minorCount++;
            }else{
                sp++;
            }
        }

        if (sp > 0) {
            System.out.println(sp + " sp");
            System.exit(0);
        }

        return new Version(
                current.major,
                current.minor + minorCount,
                current.patch + fixCount
        );
    }

    /* ================= Properties ================= */

    private static void updateProperties(Version version) throws Exception {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(PROPERTIES_PATH)) {
            p.load(in);
        }

        p.setProperty("version", version.toString());

        try (OutputStream out = Files.newOutputStream(PROPERTIES_PATH)) {
            p.store(out, null);
        }
    }

    /* ================= Utils ================= */

    private static String exec(String... cmd) throws Exception {
        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String out = reader.lines().collect(Collectors.joining("\n"));
            int code = process.waitFor();
            if (code != 0) {
                throw new RuntimeException(
                        "Command failed: " + String.join(" ", cmd)
                );
            }
            return out;
        }
    }

    /* ================= Model ================= */
    static final class Version {
        final int major;
        final int minor;
        final int patch;

        Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        static Version parse(String s) {
            // v1.2.3
            if (!s.startsWith("v")) {
                throw new IllegalArgumentException("Invalid tag: " + s);
            }
            String[] arr = s.substring(1).split("\\.");
            return new Version(
                    Integer.parseInt(arr[0]),
                    Integer.parseInt(arr[1]),
                    Integer.parseInt(arr[2])
            );
        }

        @Override
        public String toString() {
            return "v" + major + "." + minor + "." + patch;
        }
    }
}
