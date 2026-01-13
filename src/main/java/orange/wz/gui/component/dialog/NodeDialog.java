package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;
import java.awt.event.HierarchyEvent;

public class NodeDialog extends BaseDialog<NodeFormData> {
    protected final JTextField nameField = new JTextField(20);

    public NodeDialog(String title, EditPane editPane) {
        this(title, "名称", editPane);
    }

    public NodeDialog(String title, String fieldName, EditPane editPane) {
        super(title, editPane);

        addRow(fieldName, nameField);

        // 显示的时候光标聚焦在Name输入框
        nameField.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && nameField.isShowing()) {
                SwingUtilities.invokeLater(nameField::requestFocusInWindow);
            }
        });
    }

    @Override
    public NodeFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        return new NodeFormData(nameField.getText().trim(), "List");
    }
}
