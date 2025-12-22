package orange.wz.gui;

import com.formdev.flatlaf.FlatLightLaf;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.key.KeyBox;
import orange.wz.gui.component.key.KeyManager;
import orange.wz.gui.component.panel.CenterPane;
import orange.wz.gui.utils.UrlUtil;
import orange.wz.manager.ServerManager;
import orange.wz.utils.wzkey.WzKey;
import orange.wz.utils.wzkey.WzKeyStorage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

import static orange.wz.gui.Icons.*;

@Slf4j
@Getter
public class MainFrame extends JFrame {
    private final WzKeyStorage wzKeyStorage = new WzKeyStorage();
    private static MainFrame instance;

    private KeyBox keyBox;
    private KeyManager keyManager;

    private CenterPane centerPane;

    private JProgressBar progressBar;
    private JLabel statusLabel;

    private final Clipboard clipboard = new Clipboard();

    public static MainFrame getInstance() {
        if (instance == null) {
            instance = new MainFrame();
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

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");

        JMenu openItem = new JMenu("加载");
        openItem.setIcon(FcFolderIcon);

        JMenuItem openFiles = new JMenuItem("文件 wz/img", FcFileIcon);
        openFiles.addActionListener(e -> {
            List<File> files = orange.wz.gui.component.FileDialog.chooseOpenFiles(new String[]{"wz", "img"});
            centerPane.getLeftEditPane().open(files);
        });
        openItem.add(openFiles);

        JMenuItem openFolders = new JMenuItem("文件夹...", FcFolderIcon);
        openFolders.addActionListener(e -> {
            List<File> files = FileDialog.chooseOpenFolders();
            centerPane.getLeftEditPane().open(files);
        });
        openItem.add(openFolders);

        WzKey[] keys = wzKeyStorage.loadAll().toArray(new WzKey[0]);
        keyBox = new KeyBox(keys);

        JButton keyManager = new JButton("密钥管理");
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

        fileMenu.add(openItem);


        JMenu tools = new JMenu("工具");
        JMenu view = new JMenu("视图");
        view.setIcon(AiOutlineEye);

        JMenuItem viewShow = new JMenuItem("显示");
        viewShow.addActionListener(e -> {
            if (centerPane.isRightShowing()) {
                centerPane.showRightEditPane(false);
                viewShow.setText("显示");
            } else {
                centerPane.showRightEditPane(true);
                viewShow.setText("隐藏");
            }
        });

        JMenuItem viewSync = new JMenuItem("禁用同步");
        viewSync.addActionListener(e -> {
            if (centerPane.isSync()) {
                centerPane.switchSync();
                viewSync.setText("启用同步");
            } else {
                centerPane.switchSync();
                viewSync.setText("禁用同步");
            }
        });

        view.add(viewShow);
        view.add(viewSync);
        tools.add(view);


        JMenu help = new JMenu("帮助");
        JMenuItem bbs = new JMenuItem("论坛");
        bbs.addActionListener(e -> UrlUtil.open("https://moguwuyu.com/"));
        help.add(bbs);

        menuBar.add(fileMenu);
        menuBar.add(tools);
        menuBar.add(help);
        menuBar.add(Box.createHorizontalStrut(2));
        menuBar.add(keyBox);
        menuBar.add(Box.createHorizontalStrut(2));
        menuBar.add(keyManager);

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

        // 状态文字
        JLabel versionLabel = new JLabel("OrzRepacker " + ServerManager.getVersion());
        versionLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        statusBar.add(versionLabel, BorderLayout.EAST);

        Timer timer = new Timer(1000, e -> {
            Runtime rt = Runtime.getRuntime();

            long used = rt.totalMemory() - rt.freeMemory();
            long max = rt.maxMemory();

            versionLabel.setText(String.format(
                    "%.1f MB / %.1f MB    OrzRepacker" + ServerManager.getVersion(),
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

    public void setStatusText(String format, Object... args) {
        if (statusLabel != null) {
            statusLabel.setText(String.format(format, args));
        }
    }
}
