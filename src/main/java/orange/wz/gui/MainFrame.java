package orange.wz.gui;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.dialog.LogDialog;
import orange.wz.gui.component.key.KeyBox;
import orange.wz.gui.component.key.KeyManager;
import orange.wz.gui.component.panel.CenterPane;
import orange.wz.gui.utils.UrlUtil;
import orange.wz.manager.ServerManager;
import orange.wz.provider.tools.wzkey.WzKey;
import orange.wz.provider.tools.wzkey.WzKeyStorage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static orange.wz.gui.Icons.*;

@Slf4j
@Getter
public class MainFrame extends JFrame {
    private final WzKeyStorage wzKeyStorage = new WzKeyStorage();
    private static MainFrame instance;

    private KeyBox keyBox;
    private KeyManager keyManager;
    private JMenuItem viewShow;

    private CenterPane centerPane;

    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel newVerLabel;

    private final Clipboard clipboard = new Clipboard();

    private LogDialog logDialog;

    public static MainFrame getInstance() {
        if (instance == null) {
            instance = new MainFrame();
            instance.checkUpdate();
        }
        return instance;
    }

    public MainFrame() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {
        }
        setTitle("OrzRepacker");
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawPanel();
    }

    private void drawPanel() {
        setJMenuBar(createMenuBar());

        centerPane = new CenterPane();
        add(centerPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件
        JMenu fileMenu = new JMenu("文件");

        JMenuItem openFiles = new JMenuItem("加载文件 wz/img/xml", FcFileIcon);
        JMenuItem openFolders = new JMenuItem("加载文件夹...", FcFolderIcon);
        JMenuItem newWz = new JMenuItem("新建 Wz", AiOutlineFileWordIcon);
        JMenuItem newImg = new JMenuItem("新建 Img", AiOutlineFileMarkdownIcon);
        JMenuItem unloadAll = new JMenuItem("卸载全部", AiOutlineCloseIcon);

        fileMenu.add(openFiles);
        fileMenu.add(openFolders);
        fileMenu.add(newWz);
        fileMenu.add(newImg);
        fileMenu.add(unloadAll);

        // 工具
        JMenu tools = new JMenu("工具");

        JMenu view = new JMenu("视图");
        view.setIcon(AiOutlineEye);
        viewShow = new JMenuItem("显示");
        JMenuItem viewSync = new JMenuItem("禁用同步");
        view.add(viewShow);
        view.add(viewSync);

        JMenuItem clearCB = new JMenuItem("清空剪贴板");
        JMenuItem gc = new JMenuItem("内存回收");

        tools.add(view);
        tools.add(clearCB);
        tools.add(gc);

        // 帮助
        JMenu help = new JMenu("帮助");

        JMenuItem bbs = new JMenuItem("论坛");
        JMenuItem log = new JMenuItem("日志");

        help.add(bbs);
        help.add(log);

        // 密钥
        keyBox = new KeyBox(wzKeyStorage.loadAll().toArray(new WzKey[0])); // 选择框
        JButton keyManager = new JButton("密钥管理");


        menuBar.add(fileMenu);
        menuBar.add(tools);
        menuBar.add(help);
        menuBar.add(Box.createHorizontalStrut(2));
        menuBar.add(keyBox);
        menuBar.add(Box.createHorizontalStrut(2));
        menuBar.add(keyManager);


        openFiles.addActionListener(e -> {
            List<File> files = orange.wz.gui.component.FileDialog.chooseOpenFiles(new String[]{"wz", "img", "xml"});
            centerPane.getLeftEditPane().loadFiles(files);
        });
        openFolders.addActionListener(e -> {
            List<File> files = FileDialog.chooseOpenFolders();
            centerPane.getLeftEditPane().loadFiles(files);
        });
        unloadAll.addActionListener(e -> {
            centerPane.getLeftEditPane().unloadAll();
            centerPane.getRightEditPane().unloadAll();
            System.gc();
        });
        viewShow.addActionListener(e -> {
            centerPane.showRightEditPane(!centerPane.isRightShowing());
        });
        viewSync.addActionListener(e -> {
            if (centerPane.isSync()) {
                centerPane.switchSync();
                viewSync.setText("启用同步");
            } else {
                centerPane.switchSync();
                viewSync.setText("禁用同步");
            }
        });
        bbs.addActionListener(e -> UrlUtil.open("https://moguwuyu.com/"));
        log.addActionListener(e -> {
            if (logDialog == null) {
                logDialog = new LogDialog(this);
            }
            logDialog.setVisible(true);
        });
        keyManager.addActionListener(e -> {
            if (this.keyManager == null) {
                Window owner = SwingUtilities.getWindowAncestor(keyManager);
                this.keyManager = new KeyManager(owner);

                this.keyManager.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        MainFrame.this.keyManager = null;
                    }
                });
            }
            this.keyManager.setVisible(true);
        });
        clearCB.addActionListener(e -> clearClipboard());
        gc.addActionListener(e -> gc());
        newWz.addActionListener(e -> centerPane.getLeftEditPane().createWz());
        newImg.addActionListener(e -> centerPane.getLeftEditPane().createImg());

        return menuBar;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        // 进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusBar.add(progressBar, BorderLayout.WEST);

        // 状态文字
        statusLabel = new JLabel("准备就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        statusBar.add(statusLabel, BorderLayout.CENTER);

        // 右下角的标签面板
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS)); // 水平排列
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // 新版本
        newVerLabel = new JLabel("");
        newVerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newVerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String url = "https://moguwuyu.com/t/orzrepacker";
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    log.error("无法打开系统浏览器，请自行访问 {}", url, ex);
                }
            }
        });
        rightPanel.add(newVerLabel);
        rightPanel.add(Box.createHorizontalStrut(10)); // 间距

        // 内存
        JLabel memLabel = new JLabel("");
        rightPanel.add(memLabel);
        rightPanel.add(Box.createHorizontalStrut(10)); // 间距

        // 当前版本
        JLabel versionLabel = new JLabel("OrzRepacker " + ServerManager.getVersion());
        rightPanel.add(versionLabel);

        // 添加到右侧
        statusBar.add(rightPanel, BorderLayout.EAST);

        Timer timer = new Timer(1000, e -> {
            Runtime rt = Runtime.getRuntime();

            long used = rt.totalMemory() - rt.freeMemory();
            long max = rt.maxMemory();

            memLabel.setText(String.format(
                    "内存 %.1f MB / %.1f MB",
                    used / 1024.0 / 1024.0,
                    max / 1024.0 / 1024.0
            ));
        });
        timer.start();

        return statusBar;
    }

    /**
     * 更新进度条
     *
     * @param current 当前进度
     * @param total   总量
     */
    public void updateProgress(int current, int total) {
        int percent = (int) ((double) current / total * 100);
        progressBar.setValue(percent);
        progressBar.setString(current + "/" + total);
    }

    /**
     * 设置底部状态文字
     *
     * @param format 文字格式
     * @param args   参数
     */
    public void setStatusText(String format, Object... args) {
        if (statusLabel != null) {
            statusLabel.setText(String.format(format, args));
        }
    }

    private void gc() {
        System.gc();
        setStatusText("已向系统建议回收内存");
    }

    private void clearClipboard() {
        clipboard.lock();
        clipboard.clear();
        clipboard.unlock();
        setStatusText("剪贴板已清空");
    }

    private void checkUpdate() {
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() {
                try {
                    String version = ServerManager.getVersion();
                    String key = ServerManager.getKey();
                    long timestamp = Instant.now().getEpochSecond();
                    String token = generateHmacSha256(timestamp, key);

                    String urlStr = String.format(
                            "https://moguwuyu.com/api/checkUpdate?name=%s&version=%s&timestamp=%d&token=%s",
                            "OrzRepacker", version, timestamp, token
                    );

                    URL url = URI.create(urlStr).toURL();
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000); // 连接超时
                    conn.setReadTimeout(5000);    // 读取超时

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200) {
                        log.warn("检查更新失败 Code {}", responseCode);
                        return null;
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    final Gson gson = new Gson();
                    JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
                    JsonObject data = json.getAsJsonObject("data");
                    JsonObject attributes = data.getAsJsonObject("attributes");

                    int code = attributes.get("code").getAsInt();
                    String message = attributes.get("message").getAsString();

                    if (code == 200) return null;
                    if (code == 201) {
                        newVerLabel.setText("<html><a href='' style='color:red;'>有新版本 " + message + "</a></html>");
                    }
                    return null;

                } catch (ConnectException ignored) {
                    return null;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }
        }.execute();
    }

    private String generateHmacSha256(long timestamp, String key) throws Exception {
        String message = String.valueOf(timestamp);
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hashBytes = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));

        // 转成16进制字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
