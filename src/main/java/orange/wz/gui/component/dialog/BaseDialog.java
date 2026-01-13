package orange.wz.gui.component.dialog;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

@Slf4j
public abstract class BaseDialog<T> {
    protected final EditPane editPane;
    protected final JPanel panel = new JPanel(new GridBagLayout());
    protected final String title;
    private int topPanelRow = 0;

    private JDialog dialog;
    private JOptionPane optionPane;

    public BaseDialog(String title, EditPane editPane) {
        this.title = title;
        this.editPane = editPane;
    }

    protected void addRow(String label, JComponent... fields) {
        // 标签 gbc
        GridBagConstraints labelGbc = baseGbc();
        labelGbc.gridx = 0;
        labelGbc.gridy = topPanelRow;
        labelGbc.weightx = 0; // 标签不拉伸
        panel.add(new JLabel(label), labelGbc);

        // 输入组件
        for (int i = 0; i < fields.length; i++) {
            GridBagConstraints fieldGbc = baseGbc();
            fieldGbc.gridx = i + 1;
            fieldGbc.gridy = topPanelRow;

            // 只有最后一个组件拉伸
            if (i == fields.length - 1) {
                fieldGbc.weightx = 1.0;
                fieldGbc.fill = GridBagConstraints.HORIZONTAL;
            } else {
                fieldGbc.weightx = 0;
            }

            panel.add(fields[i], fieldGbc);
        }

        topPanelRow++;
    }

    /**
     * 布局完成后才能执行，否则窗口无法自适应扩大
     */
    private void initDialog() {
        optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        dialog = optionPane.createDialog(editPane, title);
    }

    protected void addWindowListener(WindowListener l) {
        if (dialog == null) initDialog();
        dialog.addWindowListener(l);
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }

    protected int showDialog() {
        if (dialog == null) initDialog();
        dialog.setVisible(true);

        // 获取用户选择结果
        Object value = optionPane.getValue();
        return (value instanceof Integer) ? (Integer) value : JOptionPane.CLOSED_OPTION;
    }

    public abstract T getData();
}
