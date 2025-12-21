package orange.wz.gui.component.form.impl;

import orange.wz.gui.component.form.data.FloatFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.filter.DecimalFilter;
import orange.wz.provider.WzObject;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public class FloatForm extends AbstractValueForm {
    private final JTextField valueInput = new JTextField(defaultColumns);

    public FloatForm() {
        super();
        ((AbstractDocument) valueInput.getDocument()).setDocumentFilter(new DecimalFilter());
        addRow("值:", valueInput);
    }

    public void setData(String name, String type, float value, WzObject wzObject, EditPane editPane) {
        super.setData(name, type, wzObject, editPane);
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

