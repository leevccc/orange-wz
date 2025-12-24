package orange.wz.gui.component.canvas;

import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.CanvasUtilData;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ImageGridPanel extends JScrollPane {
    private static final int CELL_SIZE = 180;
    private static final int GAP = 10;

    private final Set<Integer> loadedIndices = new HashSet<>();

    public ImageGridPanel(List<CanvasUtilData> data, DefaultMutableTreeNode node, EditPane editPane) {
        JScrollPane scrollPane = this;
        JPanel panel = new JPanel();
        panel.setLayout(new WrapLayout(FlowLayout.LEFT, GAP, GAP));
        panel.setBackground(Color.DARK_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

        scrollPane.setViewportView(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30); // 滚动速度
        // 监听滚动事件，实现懒加载
        // scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> loadVisibleImages(panel, data, scrollPane, node, editPane));
        // 监听窗口大小变化，实现懒加载
        scrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                resizePanel(panel, data, scrollPane);
            }
        });

        // 初始加载
        SwingUtilities.invokeLater(() -> loadVisibleImages(panel, data, scrollPane, node, editPane));
    }


    private void loadVisibleImages(JPanel gridPanel, List<CanvasUtilData> data, JScrollPane scrollPane, DefaultMutableTreeNode node, EditPane editPane) {
        int panelWidth = scrollPane.getViewport().getWidth();
        int columns = Math.max(1, (panelWidth - GAP) / (CELL_SIZE + GAP));
        int x, y;

        int fullRows = data.size() / columns;
        int remainder = data.size() % columns;
        int panelHeight = fullRows * (CELL_SIZE + GAP) + (remainder > 0 ? CELL_SIZE : 0) + GAP * 3;
        gridPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));

        for (int i = 0; i < data.size(); i++) {
            if (loadedIndices.contains(i)) continue; // 已加载过的跳过

            int pre = Math.max(i - 10, 0);
            int row = pre / columns;
            int col = pre % columns;
            x = GAP + col * (CELL_SIZE + GAP);
            y = GAP + row * (CELL_SIZE + GAP);

            ImageCellPanel cell = new ImageCellPanel(data.get(i), node, editPane);
            cell.setBounds(x, y, CELL_SIZE, CELL_SIZE);
            gridPanel.add(cell);
            loadedIndices.add(i);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void resizePanel(JPanel gridPanel, List<CanvasUtilData> data, JScrollPane scrollPane) {
        int panelWidth = scrollPane.getViewport().getWidth();
        int columns = Math.max(1, (panelWidth - GAP) / (CELL_SIZE + GAP));

        int fullRows = data.size() / columns;
        int remainder = data.size() % columns;
        int panelHeight = fullRows * (CELL_SIZE + GAP) + (remainder > 0 ? CELL_SIZE : 0) + GAP * 3;
        gridPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));

        gridPanel.revalidate();
        gridPanel.repaint();
    }
}
