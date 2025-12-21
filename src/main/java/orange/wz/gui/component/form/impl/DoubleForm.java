package orange.wz.gui.component.form.impl;

import orange.wz.gui.component.form.data.DoubleFormData;
import orange.wz.gui.filter.DecimalFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public class DoubleForm extends AbstractValueForm {
    private final JTextField valueInput = new JTextField(defaultColumns);

    public DoubleForm() {
        super();
        ((AbstractDocument) valueInput.getDocument()).setDocumentFilter(new DecimalFilter());
        addRow("值:", valueInput);
    }

    public void setData(String name, String type, double value) {
        super.setData(name, type);
        valueInput.setText(String.valueOf(value));
    }

    @Override
    public DoubleFormData getData() {
        double value;
        try {
            value = Double.parseDouble(valueInput.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new DoubleFormData(
                nameInput.getText(),
                typeInput.getText(),
                value
        );
    }
}

