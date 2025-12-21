package orange.wz.gui.component.form.impl;

import orange.wz.gui.component.form.data.IntFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.filter.IntegerFilter;
import orange.wz.provider.WzObject;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public class IntForm extends AbstractValueForm {
    private final JTextField valueInput = new JTextField(defaultColumns);

    public IntForm() {
        super();
        ((AbstractDocument) valueInput.getDocument()).setDocumentFilter(new IntegerFilter());
        addRow("值:", valueInput);
    }

    public void setData(String name, String type, int value, WzObject wzObject, EditPane editPane) {
        super.setData(name, type, wzObject, editPane);
        valueInput.setText(String.valueOf(value));
    }

    @Override
    public IntFormData getData() {
        int value;
        try {
            value = Integer.parseInt(valueInput.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new IntFormData(
                nameInput.getText(),
                typeInput.getText(),
                value
        );
    }
}

