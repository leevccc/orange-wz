package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.DoubleFormData;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class ScaleDialog extends BaseDialog<NodeFormData> {
    private final JTextField nameField = new JTextField(20);
    private final JTextField valueField = new JTextField(20);

    public ScaleDialog(EditPane editPane) {
        super(MainFrame.i18n.get("test.temp0161"), editPane);
        addRow(MainFrame.i18n.get("test.temp0162"), nameField);
        addRow(MainFrame.i18n.get("test.temp0163"), valueField);
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
