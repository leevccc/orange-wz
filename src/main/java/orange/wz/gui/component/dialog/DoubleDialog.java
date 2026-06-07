package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.DoubleFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class DoubleDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public DoubleDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow(MainFrame.i18n.get("test.temp0098"), valueField);
    }

    @Override
    public DoubleFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        double value;
        try {
            value = Double.parseDouble(valueField.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new DoubleFormData(
                nameField.getText(),
                "Double",
                value
        );
    }
}
