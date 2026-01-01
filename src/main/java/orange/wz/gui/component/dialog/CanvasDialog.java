package orange.wz.gui.component.dialog;

import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.form.base.DisabledItemComboBox;
import orange.wz.gui.component.form.data.CanvasFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.properties.WzPngFormat;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CanvasDialog extends NodeDialog {
    private final JTextField pathField = new JTextField(20);
    private final DisabledItemComboBox<WzPngFormat> formatField;
    private BufferedImage image;

    public CanvasDialog(String title, EditPane editPane) {
        super(title, editPane);

        pathField.setEditable(false);
        formatField = new DisabledItemComboBox<>(WzPngFormat.values());
        formatField.setSelectedItem(WzPngFormat.ARGB8888);
        addRow("压缩", formatField);

        JButton selectBtn = new JButton("选择图片");
        addRow("路径", pathField, selectBtn);

        selectBtn.addActionListener(e -> {
            File pngFile = FileDialog.chooseOpenFile(new String[]{"png"});
            if (pngFile == null) return;

            try (ByteArrayInputStream bis = new ByteArrayInputStream(Files.readAllBytes(pngFile.toPath()))) {
                image = ImageIO.read(bis);
                if (image == null) {
                    JMessageUtil.error("无法解码图片数据");
                    return;
                }
            } catch (IOException ex) {
                JMessageUtil.error("无法读取文件");
                return;
            }

            pathField.setText(pngFile.getAbsolutePath());
        });
    }

    public CanvasFormData getData() {
        int result = showConfirmDialog(
                editPane,
                panel,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        if (image == null) {
            JMessageUtil.error("没有选择图片");
            return null;
        }
        return new CanvasFormData(nameField.getText().trim(),
                "Canvas",
                image,
                (WzPngFormat) formatField.getSelectedItem());
    }
}
