package orange.wz.gui.form.impl;

import orange.wz.gui.form.data.FloatFormData;
import orange.wz.gui.filter.DecimalFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public class FloatForm extends AbstractValueForm {
    private final JTextField valueInput = new JTextField(defaultColumns);

    public FloatForm() {
        super();
        ((AbstractDocument) valueInput.getDocument()).setDocumentFilter(new DecimalFilter());
        addRow("值:", valueInput);
    }

    public void setData(String name, String type, float value) {
        super.setData(name, type);
        valueInput.setText(String.valueOf(value));
    }

    @Override
    public FloatFormData getData() {
        float value;
        try {
            value = Float.parseFloat(valueInput.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new FloatFormData(
                nameInput.getText(),
                typeInput.getText(),
                value
        );
    }
}

