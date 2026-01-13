package orange.wz.gui.component.form.impl;

import com.formdev.flatlaf.util.SystemFileChooser;
import lombok.Setter;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.form.base.DisabledItemComboBox;
import orange.wz.gui.component.form.data.CanvasFormData;
import orange.wz.gui.component.panel.CenterPane;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.model.TransferableImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.WzPngFormat;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;

public class CanvasForm extends AbstractValueForm {
    private JTextField widthField;
    private JTextField heightField;
    protected DisabledItemComboBox<WzPngFormat> formatField;
    private JTextField scaleField;
    private ImagePanel imagePanel;
    private JSlider zoomSlider; // 缩放条
    private double zoomFactor = 1.0; // 当前缩放比例

    public CanvasForm(EditPane editPane) {
        super();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createValuePanel(), createImagePanelPanel());
        splitPane.setDividerLocation(150);
        splitPane.setDividerSize(0);
        splitPane.setEnabled(false);

        valuePane.add(splitPane, BorderLayout.CENTER);


        JButton downloadBtn = new JButton("下载");
        JButton uploadBtn = new JButton("上传");
        JButton copyBtn = new JButton("复制");
        JButton transferBtn = new JButton("转移");

        downloadBtn.addActionListener(e -> {
            byte[] imageBytes;

            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ImageIO.write(imagePanel.image, "PNG", stream);
                imageBytes = stream.toByteArray();
            } catch (Exception ex) {
                throw new RuntimeException();
            }

            if (imageBytes.length == 0) {
                JMessageUtil.warn("没有可保存的图片数据");
                return;
            }

            SystemFileChooser chooser = new SystemFileChooser();
            chooser.setDialogTitle("保存图片文件");
            chooser.setSelectedFile(new File(nameInput.getText() + ".png"));
            chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter("PNG 文件 (*.png)", "png"));

            File file = null;
            int res = chooser.showSaveDialog(valuePane);
            if (res == SystemFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
            }

            if (file == null) {
                return;
            }

            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(imageBytes);

                JMessageUtil.info("保存成功：\n" + file.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
                JMessageUtil.error("保存失败: " + ex.getMessage());
            }
        });

        uploadBtn.addActionListener(e -> {
            File file = FileDialog.chooseOpenFile(new String[]{"png"});
            if (file == null) {
                return;
            }

            try {
                setData(Files.readAllBytes(file.toPath()));

            } catch (IOException ex) {
                ex.printStackTrace();
                JMessageUtil.error("读取文件失败: " + ex.getMessage());
            }
        });

        copyBtn.addActionListener(e -> {
            TransferableImage trans = new TransferableImage(imagePanel.image);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(trans, null);
            MainFrame.getInstance().setStatusText("图片已经复制到系统剪贴板");
        });

        transferBtn.addActionListener(e -> {
            CenterPane centerPane = MainFrame.getInstance().getCenterPane();
            EditPane anotherPane = centerPane.getAnotherPane(editPane);
            if (centerPane.getRightEditPane() == anotherPane && !centerPane.isRightShowing()) {
                JMessageUtil.error("另一个视图未激活");
                return;
            }
            if (!anotherPane.getCurrentFormName().equals("canvas")) {
                JMessageUtil.error("另一个视图的编辑器未处于Canvas节点");
                return;
            }

            anotherPane.getCanvasForm().transferData(imagePanel.image, widthField.getText(), heightField.getText(), (WzPngFormat) formatField.getSelectedItem(), scaleField.getText());
        });

        addButton(downloadBtn);
        addButton(uploadBtn);
        addButton(copyBtn);
        addButton(transferBtn);
    }

    private JPanel createValuePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new GridBagLayout());
        panel.add(topPanel, BorderLayout.NORTH);

        // topPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 宽度
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        topPanel.add(new JLabel("宽度:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        widthField = new JTextField(6);
        widthField.setEditable(false);
        topPanel.add(widthField, gbc);

        // 高度
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        topPanel.add(new JLabel("高度:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        heightField = new JTextField(6);
        heightField.setEditable(false);
        topPanel.add(heightField, gbc);

        // format
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        topPanel.add(new JLabel("格式:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formatField = new DisabledItemComboBox<>(WzPngFormat.values());
        topPanel.add(formatField, gbc);

        // scale
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        topPanel.add(new JLabel("缩放:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        scaleField = new JTextField(6);
        scaleField.setEditable(false);
        topPanel.add(scaleField, gbc);

        return panel;
    }

    private JPanel createImagePanelPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        imagePanel = new ImagePanel();
        imagePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // 给图片面板加边框

        panel.add(imagePanel, BorderLayout.CENTER);

        // 缩放条
        zoomSlider = new JSlider(10, 300, 100); // 10% - 300%，初始100%
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.addChangeListener(e -> {
            zoomFactor = zoomSlider.getValue() / 100.0;
            imagePanel.repaint();
        });

        panel.add(zoomSlider, BorderLayout.SOUTH);

        return panel;
    }

    private void setData(byte[] imageBytes) {
        BufferedImage image;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
            image = ImageIO.read(bis);
            if (image == null) {
                throw new IOException("无法解码图片数据，可能不是支持的图片格式");
            }
        } catch (IOException ex) {
            throw new RuntimeException();
        }

        widthField.setText(image.getWidth() + "");
        heightField.setText(image.getHeight() + "");
        imagePanel.setImage(image);
        imagePanel.repaint();
    }

    public void setData(String name, String type, BufferedImage image, int width, int height, WzPngFormat format, int scale, WzObject wzObject, EditPane editPane) {
        super.setData(name, type, wzObject, editPane);

        widthField.setText(String.valueOf(width));
        heightField.setText(String.valueOf(height));
        formatField.setSelectedItem(format);
        scaleField.setText(String.valueOf(scale));

        imagePanel.setImage(image);
        imagePanel.repaint();
    }

    public void transferData(BufferedImage image, String width, String height, WzPngFormat format, String scale) {
        widthField.setText(width);
        heightField.setText(height);
        formatField.setSelectedItem(format);
        scaleField.setText(scale);

        imagePanel.setImage(image);
        imagePanel.repaint();
        MainFrame.getInstance().setStatusText("转移成功，请手动点击保存按钮。");
    }

    @Setter
    private class ImagePanel extends JPanel {
        private BufferedImage image;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int imgWidth = (int) (image.getWidth() * zoomFactor);
                int imgHeight = (int) (image.getHeight() * zoomFactor);

                int x = (getWidth() - imgWidth) / 2;
                int y = (getHeight() - imgHeight) / 2;

                g2.drawImage(image, x, y, imgWidth, imgHeight, this);

                g2.dispose();
            }
        }
    }

    @Override
    public CanvasFormData getData() {
        int scale;
        try {
            scale = Integer.parseInt(scaleField.getText());
        } catch (NumberFormatException ex) {
            scale = 0;
        }

        return new CanvasFormData(
                nameInput.getText(),
                typeInput.getText(),
                imagePanel.image,
                (WzPngFormat) formatField.getSelectedItem(),
                scale
        );
    }
}
