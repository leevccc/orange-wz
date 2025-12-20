package orange.wz.gui.form.impl;

import orange.wz.gui.form.data.ShortFormData;
import orange.wz.gui.filter.IntegerFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public class ShortForm extends AbstractValueForm {
    private final JTextField valueInput = new JTextField(defaultColumns);

    public ShortForm() {
        super();
        ((AbstractDocument) valueInput.getDocument()).setDocumentFilter(new IntegerFilter());
        addRow("值:", valueInput);
    }

    public void setData(String name, String type, short value) {
        super.setData(name, type);
        valueInput.setText(String.valueOf(value));
    }

    @Override
    public ShortFormData getData() {
        short value;
        try {
            value = Short.parseShort(valueInput.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new ShortFormData(
                nameInput.getText(),
                typeInput.getText(),
                value
        );
    }
}

