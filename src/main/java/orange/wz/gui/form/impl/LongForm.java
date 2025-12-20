package orange.wz.gui.form.impl;

import orange.wz.gui.form.data.LongFormData;
import orange.wz.gui.filter.IntegerFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public class LongForm extends AbstractValueForm {
    private final JTextField valueInput = new JTextField(defaultColumns);

    public LongForm() {
        super();
        ((AbstractDocument) valueInput.getDocument()).setDocumentFilter(new IntegerFilter());
        addRow("值:", valueInput);
    }

    public void setData(String name, String type, long value) {
        super.setData(name, type);
        valueInput.setText(String.valueOf(value));
    }

    @Override
    public LongFormData getData() {
        long value;
        try {
            value = Long.parseLong(valueInput.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new LongFormData(
                nameInput.getText(),
                typeInput.getText(),
                value
        );
    }
}

