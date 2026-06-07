package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
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

public final class CanvasDialog extends NodeDialog {
    private final JTextField pathField = new JTextField(20);
    private final DisabledItemComboBox<WzPngFormat> formatField;
    private final JTextField scaleField = new JTextField(20);
    private BufferedImage image;

    public CanvasDialog(String title, EditPane editPane) {
        super(title, editPane);

        pathField.setEditable(false);
        formatField = new DisabledItemComboBox<>(WzPngFormat.values());
        formatField.setSelectedItem(WzPngFormat.ARGB8888);
        addRow(MainFrame.i18n.get("test.temp0068"), formatField);
        addRow(MainFrame.i18n.get("test.temp0069"), scaleField);
        scaleField.setText("0");

        JButton selectBtn = new JButton(MainFrame.i18n.get("test.temp0070"));
        addRow(MainFrame.i18n.get("test.temp0071"), pathField, selectBtn);

        selectBtn.addActionListener(e -> {
            File pngFile = FileDialog.chooseOpenFile(new String[]{"png"});
            if (pngFile == null) return;

            try (ByteArrayInputStream bis = new ByteArrayInputStream(Files.readAllBytes(pngFile.toPath()))) {
                image = ImageIO.read(bis);
                if (image == null) {
                    JMessageUtil.error(MainFrame.i18n.get("test.temp0072"));
                    return;
                }
            } catch (IOException ex) {
                JMessageUtil.error(MainFrame.i18n.get("test.temp0073"));
                return;
            }

            pathField.setText(pngFile.getAbsolutePath());
        });
    }

    @Override
    public CanvasFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        if (image == null) {
            JMessageUtil.error(MainFrame.i18n.get("test.temp0074"));
            return null;
        }

        int scale;
        try {
            scale = Integer.parseInt(scaleField.getText());
        } catch (NumberFormatException e) {
            scale = 0;
        }
        return new CanvasFormData(nameField.getText().trim(),
                "Canvas",
                image,
                (WzPngFormat) formatField.getSelectedItem(),
                scale
        );
    }
}
