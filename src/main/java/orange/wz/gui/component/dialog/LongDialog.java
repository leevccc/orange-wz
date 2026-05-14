package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.LongFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class LongDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public LongDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow(MainFrame.i18n.get("test.temp0098"), valueField);
    }

    @Override
    public LongFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        long value;
        try {
            value = Long.parseLong(valueField.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new LongFormData(
                nameField.getText(),
                "Long",
                value
        );
    }
}
