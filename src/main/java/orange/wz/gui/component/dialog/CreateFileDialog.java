package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.CreateFileData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.filter.IntegerFilter;
import orange.wz.gui.utils.JMessageUtil;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class CreateFileDialog extends BaseDialog<CreateFileData> {
    private final JTextField versionInput = new JTextField(20);
    private final JTextField nameInput = new JTextField(20);

    public CreateFileDialog(EditPane editPane, boolean wzFile) {
        super("新建文件", editPane);

        if (wzFile) {
            ((AbstractDocument) versionInput.getDocument()).setDocumentFilter(new IntegerFilter());
            addRow("版本号", versionInput);
        } else {
            versionInput.setText("-1");
        }

        addRow("文件名", nameInput);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (nameInput.isVisible() && nameInput.isEnabled()) {
                    nameInput.requestFocusInWindow();
                    nameInput.selectAll();
                }
            }
        });
    }

    @Override
    public CreateFileData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        short version;
        try {
            version = Short.parseShort(versionInput.getText());
        } catch (NumberFormatException e) {
            JMessageUtil.error("错误版本号 " + versionInput.getText());
            return null;
        }

        return new CreateFileData(nameInput.getText(), version);
    }
}
