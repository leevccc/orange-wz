package orange.wz.gui.component.canvas;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.CanvasUtilData;
import orange.wz.gui.utils.JMessageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public final class CanvasWall extends JFrame {
    public CanvasWall(List<CanvasUtilData> data, String title, DefaultMutableTreeNode node, EditPane editPane) {
        super(MainFrame.i18n.get("canvas_wall") + " " + title);
        setIconImage(MainFrame.getInstance().getIconImage()); // 继承主窗口 icon
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // 最大化
        setExtendedState(JFrame.NORMAL); // 默认不是最大化
        setSize(810, 600);
        setLocationRelativeTo(null);

        // esc 关闭
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(esc, "ESC_CLOSE");
        getRootPane().getActionMap().put("ESC_CLOSE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });


        // 顶层容器
        JPanel mainPanel = new JPanel(new BorderLayout());
        setContentPane(mainPanel);

        // --- 1. 顶部按钮面板 ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveAllBtn = new JButton(MainFrame.i18n.get("canvas_wall.save_all"));
        topPanel.add(saveAllBtn);
        mainPanel.add(topPanel, BorderLayout.SOUTH);

        saveAllBtn.addActionListener(e -> {
            File folder = FileDialog.chooseOpenFolder(MainFrame.i18n.get("canvas_wall.save_path"));
            if (folder == null) return;
            for (CanvasUtilData c : data) {
                // 构造目标文件
                File file = new File(folder.toPath().resolve(c.getPath()) + ".png");

                // 确保父目录存在
                File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    boolean created = parentDir.mkdirs(); // 递归创建目录
                    if (!created) {
                        log.warn(MainFrame.i18n.get("warn.canvas_wall.folder_create", parentDir.getAbsolutePath()));
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    ImageIO.write(c.getImage(), "PNG", stream);
                    byte[] imageBytes = stream.toByteArray();
                    fos.write(imageBytes);
                } catch (IOException ex) {
                    log.error(MainFrame.i18n.get("error.canvas_wall.save", ex.getMessage()));
                }
            }
            JMessageUtil.info(MainFrame.i18n.get("canvas_wall.saved"));
        });

        // --- 2. 图片网格面板 ---
        ImageGridPanel gridPanel = new ImageGridPanel(data, node, editPane);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
    }
}
