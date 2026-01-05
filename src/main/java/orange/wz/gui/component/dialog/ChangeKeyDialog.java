package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.KeyData;
import orange.wz.gui.component.key.KeyBox;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.filter.IntegerFilter;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.tools.wzkey.WzKey;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public final class ChangeKeyDialog extends BaseDialog<KeyData> {
    private final JTextField versionInput = new JTextField(20);
    private final KeyBox keyBox;

    public ChangeKeyDialog(EditPane editPane, boolean wzFile) {
        super("修改密钥", editPane);

        if (wzFile) {
            ((AbstractDocument) versionInput.getDocument()).setDocumentFilter(new IntegerFilter());
            addRow("版本号", versionInput);
        } else {
            versionInput.setText("-1");
        }

        WzKey[] wzKeys = MainFrame.getInstance().getWzKeyStorage().loadAll().toArray(new WzKey[0]);
        keyBox = new KeyBox(wzKeys);
        addRow("密钥", keyBox);
    }

    @Override
    public KeyData getData() {
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
        WzKey wzKey = (WzKey) keyBox.getSelectedItem();

        return new KeyData(wzKey.getName(), version, wzKey.getIv(), wzKey.getUserKey());
    }
}
