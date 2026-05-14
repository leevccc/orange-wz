package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.StringFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class StringDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public StringDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow(MainFrame.i18n.get("test.temp0098"), valueField);
    }

    @Override
    public StringFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        return new StringFormData(
                nameField.getText(),
                "String",
                valueField.getText()
        );
    }
}
