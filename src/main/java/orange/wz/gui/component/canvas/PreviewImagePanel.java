package orange.wz.gui.component.canvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

public final class PreviewImagePanel extends JPanel {

    private final BufferedImage image;

    private double scale = 1.0;

    private static final double SCALE_STEP = 1.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 10.0;

    public PreviewImagePanel(BufferedImage image) {
        this.image = image;
        setBackground(Color.DARK_GRAY);

        // 初始尺寸 = 原图尺寸
        setPreferredSize(new Dimension(
                image.getWidth(),
                image.getHeight()
        ));

        addMouseWheelListener(new ZoomHandler());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int panelW = getWidth();
        int panelH = getHeight();

        int drawW = (int) (image.getWidth() * scale);
        int drawH = (int) (image.getHeight() * scale);

        int x = (panelW - drawW) / 2;
        int y = (panelH - drawH) / 2;

        g2.drawImage(image, x, y, drawW, drawH, null);
        g2.dispose();
    }

    private class ZoomHandler implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {

            double oldScale = scale;

            if (e.getWheelRotation() < 0) {
                scale *= SCALE_STEP;
            } else {
                scale /= SCALE_STEP;
            }

            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));

            // 没变就直接返回
            if (oldScale == scale) return;

            // 缩放后尺寸
            int newW = (int) (image.getWidth() * scale);
            int newH = (int) (image.getHeight() * scale);

            // 鼠标在图像上的相对位置
            Point mouse = e.getPoint();
            double relX = mouse.x / (double) (image.getWidth() * oldScale);
            double relY = mouse.y / (double) (image.getHeight() * oldScale);

            setPreferredSize(new Dimension(newW, newH));
            revalidate();

            // 调整滚动条，使鼠标指向的点保持不动
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(
                    JViewport.class, PreviewImagePanel.this);

            if (viewport != null) {
                Point viewPos = viewport.getViewPosition();
                int newX = (int) (relX * newW - mouse.x);
                int newY = (int) (relY * newH - mouse.y);

                viewport.setViewPosition(new Point(
                        Math.max(0, newX),
                        Math.max(0, newY)
                ));
            }

            repaint();
        }
    }
}
