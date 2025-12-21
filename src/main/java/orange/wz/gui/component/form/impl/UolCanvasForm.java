package orange.wz.gui.component.form.impl;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.form.data.StringFormData;
import orange.wz.provider.properties.WzCanvasProperty;
import orange.wz.provider.properties.WzPngFormat;

import javax.swing.*;
import java.awt.image.BufferedImage;

@Slf4j
public class UolCanvasForm extends CanvasForm {
    protected final JTextArea valueInput = new JTextArea(1, defaultColumns);

    public UolCanvasForm() {
        super();
        addRow("UOL:", valueInput);
        formatField.setEnabled(false);
    }

    public void setData(String name, String type, String value, WzCanvasProperty canvas) {

        valueInput.setText(value);
        BufferedImage image = null;
        int width = 0;
        int height = 0;
        WzPngFormat format = null;

        if (canvas == null) {
            log.warn("canvas is null");
        } else {
            image = canvas.getPngImage();
            width = canvas.getWidth();
            height = canvas.getHeight();
            format = canvas.getPngFormat();
        }

        setData(name, type, image, width, height, format);
    }

    public StringFormData getUolData() {
        return new StringFormData(
                nameInput.getText(),
                typeInput.getText(),
                valueInput.getText()
        );
    }
}
