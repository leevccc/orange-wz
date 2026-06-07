package orange.wz.gui.component.form.impl;

import com.formdev.flatlaf.util.SystemFileChooser;
import lombok.Getter;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.form.base.DisabledItemComboBox;
import orange.wz.gui.component.form.data.CanvasFormData;
import orange.wz.gui.component.panel.CenterPane;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.component.panel.ImagePanel;
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
    @Getter
    private ImagePanel imagePanel;
    private JSlider zoomSlider; // 缩放条

    public CanvasForm(EditPane editPane) {
        super();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createValuePanel(), createImagePanelPanel());
        splitPane.setDividerLocation(150);
        splitPane.setDividerSize(0);
        splitPane.setEnabled(false);

        valuePane.add(splitPane, BorderLayout.CENTER);


        JButton downloadBtn = new JButton(MainFrame.i18n.get("download"));
        JButton uploadBtn = new JButton(MainFrame.i18n.get("upload"));
        JButton copyBtn = new JButton(MainFrame.i18n.get("copy"));
        JButton transferBtn = new JButton(MainFrame.i18n.get("transfer"));

        downloadBtn.addActionListener(e -> {
            byte[] imageBytes;

            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ImageIO.write(imagePanel.getImage(), "PNG", stream);
                imageBytes = stream.toByteArray();
            } catch (Exception ex) {
                throw new RuntimeException();
            }

            if (imageBytes.length == 0) {
                JMessageUtil.warn(MainFrame.i18n.get("test.temp0099"));
                return;
            }

            SystemFileChooser chooser = new SystemFileChooser();
            chooser.setDialogTitle(MainFrame.i18n.get("test.temp0100"));
            chooser.setSelectedFile(new File(nameInput.getText() + ".png"));
            chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter(MainFrame.i18n.get("test.temp0101"), "png"));

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

                JMessageUtil.info(MainFrame.i18n.get("test.temp0102", file.getAbsolutePath()));
            } catch (IOException ex) {
                ex.printStackTrace();
                JMessageUtil.error(MainFrame.i18n.get("test.temp0103", ex.getMessage()));
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
                JMessageUtil.error(MainFrame.i18n.get("test.temp0104", ex.getMessage()));
            }
        });

        copyBtn.addActionListener(e -> {
            TransferableImage trans = new TransferableImage(imagePanel.getImage());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(trans, null);
            MainFrame.getInstance().setStatusText(MainFrame.i18n.get("status.image_copied"));
        });

        transferBtn.addActionListener(e -> {
            CenterPane centerPane = MainFrame.getInstance().getCenterPane();
            EditPane anotherPane = centerPane.getAnotherPane(editPane);
            if (centerPane.getRightEditPane() == anotherPane && !centerPane.isRightShowing()) {
                JMessageUtil.error(MainFrame.i18n.get("test.temp0105"));
                return;
            }
            if (!anotherPane.getCurrentFormName().equals("canvas")) {
                JMessageUtil.error(MainFrame.i18n.get("test.temp0106"));
                return;
            }

            anotherPane.getCanvasForm().transferData(imagePanel.getImage(), widthField.getText(), heightField.getText(), (WzPngFormat) formatField.getSelectedItem(), scaleField.getText());
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
        topPanel.add(new JLabel(MainFrame.i18n.get("test.temp0107")), gbc);

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
        topPanel.add(new JLabel(MainFrame.i18n.get("test.temp0108")), gbc);

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
        topPanel.add(new JLabel(MainFrame.i18n.get("test.temp0109")), gbc);

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
        topPanel.add(new JLabel(MainFrame.i18n.get("test.temp0110")), gbc);

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
            imagePanel.setZoomFactor(zoomSlider.getValue() / 100.0);
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
                throw new IOException(MainFrame.i18n.get("test.temp0111"));
            }
        } catch (IOException ex) {
            throw new RuntimeException();
        }

        widthField.setText(image.getWidth() + "");
        heightField.setText(image.getHeight() + "");
        imagePanel.setImage(image);
    }

    public void setData(String name, String type, BufferedImage image, int width, int height, WzPngFormat format, int scale, WzObject wzObject, EditPane editPane) {
        super.setData(name, type, wzObject, editPane);

        widthField.setText(String.valueOf(width));
        heightField.setText(String.valueOf(height));
        formatField.setSelectedItem(format);
        scaleField.setText(String.valueOf(scale));

        imagePanel.setImage(image);
    }

    public void transferData(BufferedImage image, String width, String height, WzPngFormat format, String scale) {
        widthField.setText(width);
        heightField.setText(height);
        formatField.setSelectedItem(format);
        scaleField.setText(scale);

        imagePanel.setImage(image);
        MainFrame.getInstance().setStatusText(MainFrame.i18n.get("status.transfer_success"));
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
                imagePanel.getImage(),
                (WzPngFormat) formatField.getSelectedItem(),
                scale
        );
    }
}
