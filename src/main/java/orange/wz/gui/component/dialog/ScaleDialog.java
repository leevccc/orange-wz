package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.DoubleFormData;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class ScaleDialog extends BaseDialog<NodeFormData> {
    private final JTextField nameField = new JTextField(20);
    private final JTextField valueField = new JTextField(20);

    public ScaleDialog(EditPane editPane) {
        super("图片缩放", editPane);
        addRow("匹配名称(留空=全部)", nameField);
        addRow("缩放比例(原始=1.0)", valueField);
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
