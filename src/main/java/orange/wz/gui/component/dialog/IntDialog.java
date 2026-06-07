package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.IntFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class IntDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public IntDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow(MainFrame.i18n.get("test.temp0098"), valueField);
    }

    @Override
    public IntFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        int value;
        try {
            value = Integer.parseInt(valueField.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new IntFormData(
                nameField.getText(),
                "Int",
                value
        );
    }
}
