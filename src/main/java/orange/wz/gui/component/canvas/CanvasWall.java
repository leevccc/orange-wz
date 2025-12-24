package orange.wz.gui.component.canvas;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.CanvasUtilData;
import orange.wz.gui.utils.JMessageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public final class CanvasWall extends JDialog {
    public CanvasWall(List<CanvasUtilData> data, String title, DefaultMutableTreeNode node, EditPane editPane) {
        setTitle("图片嗅探 " + title);
        setModal(false); // 不要阻塞主窗口
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(810, 600);
        setLocationRelativeTo(null);

        // 顶层容器
        JPanel mainPanel = new JPanel(new BorderLayout());
        setContentPane(mainPanel);

        // --- 1. 顶部按钮面板 ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveAllBtn = new JButton("保存全部");
        topPanel.add(saveAllBtn);
        mainPanel.add(topPanel, BorderLayout.SOUTH);

        saveAllBtn.addActionListener(e -> {
            File folder = FileDialog.chooseOpenFolder("选择保存的目录");
            if (folder == null) return;
            for (CanvasUtilData c : data) {
                // 构造目标文件
                File file = new File(folder.toPath().resolve(c.getPath()) + ".png");

                // 确保父目录存在
                File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    boolean created = parentDir.mkdirs(); // 递归创建目录
                    if (!created) {
                        log.warn("目录创建失败: {}", parentDir.getAbsolutePath());
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    ImageIO.write(c.getImage(), "PNG", stream);
                    byte[] imageBytes = stream.toByteArray();
                    fos.write(imageBytes);
                } catch (IOException ex) {
                    log.error("保存失败: {}", ex.getMessage());
                }
            }
            JMessageUtil.info("保存成功");
        });

        // --- 2. 图片网格面板 ---
        ImageGridPanel gridPanel = new ImageGridPanel(data, node, editPane);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
    }
}
