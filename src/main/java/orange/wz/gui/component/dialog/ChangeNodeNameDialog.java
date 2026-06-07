package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.ChangeNodeNameFormData;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class ChangeNodeNameDialog extends BaseDialog<NodeFormData> {
    private final JTextField oldName = new JTextField(20);
    private final JTextField newName = new JTextField(20);
    private final JTextField degree = new JTextField(20);

    public ChangeNodeNameDialog(EditPane editPane) {
        super(MainFrame.i18n.get("test.temp0086"), editPane);

        degree.setText("1");
        addRow(MainFrame.i18n.get("test.temp0087"), oldName);
        addRow(MainFrame.i18n.get("test.temp0088"), newName);
        addRow(MainFrame.i18n.get("test.temp0089"), degree);
    }

    @Override
    public ChangeNodeNameFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        int value;
        try {
            value = Integer.parseInt(degree.getText());
        } catch (NumberFormatException e) {
            value = 1;
        }

        return new ChangeNodeNameFormData(
                "",
                "String",
                oldName.getText(),
                newName.getText(),
                value
        );
    }
}
