package orange.wz.gui.component.form.impl;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.form.data.StringFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.WzSoundProperty;

import javax.swing.*;

@Slf4j
public class UolSoundForm extends SoundForm {
    protected final JTextArea valueInput = new JTextArea(1, defaultColumns);

    public UolSoundForm() {
        super();
        addRow("UOL:", valueInput);
    }

    public void setData(String name, String type, String value, WzSoundProperty sound, WzObject wzObject, EditPane editPane) {
        valueInput.setText(value);
        byte[] bytes = null;

        if (sound == null) {
            log.warn("sound is null");
        } else {
            bytes = sound.getSoundBytes(false);
        }

        setData(name, type, bytes, wzObject, editPane);
    }

    public StringFormData getUolData() {
        return new StringFormData(
                nameInput.getText(),
                typeInput.getText(),
                valueInput.getText()
        );
    }
}
