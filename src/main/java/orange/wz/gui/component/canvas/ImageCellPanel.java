package orange.wz.gui.component.canvas;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.CanvasUtilData;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.WzObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class ImageCellPanel extends JPanel {

    private static ImageCellPanel selected;

    private final ImageGridPanel imageGridPanel;
    private final CanvasUtilData data;
    private boolean isSelected;

    public ImageCellPanel(CanvasUtilData data, DefaultMutableTreeNode node, EditPane editPane, ImageGridPanel imageGridPanel) {
        this.data = data;
        this.imageGridPanel = imageGridPanel;
        setPreferredSize(new Dimension(180, 180));
        setBackground(Color.BLACK);
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Tooltip
        setToolTipText(buildTooltip());

        // --- 添加右键菜单 ---
        JPopupMenu cellMenu = new JPopupMenu();
        JMenuItem jumpBtn = new JMenuItem("打开节点");
        JMenuItem saveBtn = new JMenuItem("保存图片");

        cellMenu.add(jumpBtn);
        cellMenu.add(saveBtn);

        // 事件逻辑
        jumpBtn.addActionListener(e -> {
            TreeNode[] nodePaths = node.getPath();
            String[] wzPaths = data.getPath().split("/");
            List<String> paths = new ArrayList<>();

            // 1. 把 nodePaths 去掉 root 并放入 paths
            for (int i = 1; i < nodePaths.length; i++) { // 跳过 root
                DefaultMutableTreeNode n = (DefaultMutableTreeNode) nodePaths[i];
                paths.add(((WzObject) n.getUserObject()).getName());
            }

            // 2. 找到 nodePaths 和 wzPaths 的公共起始点
            int commonStart;
            boolean found = false;
            for (commonStart = 0; commonStart < paths.size(); commonStart++) {
                if (paths.get(commonStart).equals(wzPaths[0])) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                JMessageUtil.error("路径异常");
                return;
            }

            // 3. 把 wzPaths 多出的部分追加到 paths
            for (int i = 0; i < wzPaths.length; i++) {
                int pIndex = commonStart + i;
                if (pIndex < paths.size()) {
                    paths.set(pIndex, wzPaths[i]);
                } else {
                    paths.add(wzPaths[i]);
                }
            }

            editPane.selectTreeNodeByPath(paths);
        });
        saveBtn.addActionListener(e -> {
            String[] p = data.getPath().split("/");
            String fileName = p[p.length - 1] + ".png";
            File preFile = new File(fileName);
            File file = FileDialog.chooseSaveFile(null, "保存 " + fileName, preFile, new String[]{"png"});
            if (file == null) return;

            try (FileOutputStream fos = new FileOutputStream(file)) {

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ImageIO.write(data.getImage(), "PNG", stream);
                byte[] imageBytes = stream.toByteArray();
                fos.write(imageBytes);
            } catch (IOException ex) {
                log.error("保存失败: {}", ex.getMessage());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {  // Windows/Linux mouseReleased, Mac mousePressed
                    cellMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // 单击选中
                if (e.getClickCount() == 1) {
                    select();
                }
                // 双击放大
                else if (e.getClickCount() == 2) {
                    showPreview();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    setBorder(BorderFactory.createLineBorder(Color.ORANGE, 1));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected) {
                    setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                }
            }
        });
    }

    private void select() {
        if (selected != null) {
            selected.isSelected = false;
            selected.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            selected.repaint();
        }

        selected = this;
        isSelected = true;
        setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        repaint();
    }

    private String buildTooltip() {
        return "<html>"
                + data.getPath() + "<br>"
                + data.getWidth() + " × " + data.getHeight() + "<br>"
                + data.getFormat()
                + "</html>";
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage image = data.getImage();
        if (image == null) return;

        // 抗锯齿
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int panelW = getWidth();
        int panelH = getHeight();

        int imgW = image.getWidth();
        int imgH = image.getHeight();

        double fitScale = Math.min(
                panelW / (double) imgW,
                panelH / (double) imgH
        );

        // 只缩小，不放大
        double scale = Math.min(1.0, fitScale);

        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);

        int x = (panelW - drawW) / 2;
        int y = (panelH - drawH) / 2;

        g2.drawImage(image, x, y, drawW, drawH, this);
        g2.dispose();
    }

    private void showPreview() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "预览 - " + data.getPath(),
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(imageGridPanel);

        PreviewImagePanel panel = new PreviewImagePanel(data.getImage());
        JScrollPane scrollPane = new JScrollPane(panel);
        dialog.setContentPane(scrollPane);

        // 绑定 ESC
        KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESCAPE");
        panel.getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }
}
