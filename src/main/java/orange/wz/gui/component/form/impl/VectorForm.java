package orange.wz.gui.component.form.impl;

import orange.wz.gui.component.form.data.VectorFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.filter.IntegerFilter;
import orange.wz.provider.WzObject;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public class VectorForm extends AbstractValueForm {
    private final JTextField xInput = new JTextField(defaultColumns);
    private final JTextField yInput = new JTextField(defaultColumns);

    public VectorForm() {
        super();
        ((AbstractDocument) xInput.getDocument()).setDocumentFilter(new IntegerFilter());
        addRow("X:", xInput);
        ((AbstractDocument) yInput.getDocument()).setDocumentFilter(new IntegerFilter());
        addRow("Y:", yInput);
    }

    public void setData(String name, String type, int x, int y, WzObject wzObject, EditPane editPane) {
        super.setData(name, type, wzObject, editPane);
        xInput.setText(String.valueOf(x));
        yInput.setText(String.valueOf(y));
    }

    @Override
    public VectorFormData getData() {
        int x, y;
        try {
            x = Integer.parseInt(xInput.getText());
            y = Integer.parseInt(yInput.getText());
        } catch (NumberFormatException e) {
            x = 0;
            y = 0;
        }

        return new VectorFormData(
                nameInput.getText(),
                typeInput.getText(),
                x,
                y
        );
    }
}

