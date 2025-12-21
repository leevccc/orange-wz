package orange.wz.gui.component.form.impl;

import orange.wz.gui.component.form.FormPanel;
import orange.wz.gui.component.form.FormSaveHandler;
import orange.wz.gui.component.form.data.NodeFormData;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractValueForm implements FormPanel {

    protected final static int defaultColumns = 30;
    protected final JPanel panel = new JPanel(new BorderLayout());
    protected final JPanel topLeftPanel = new JPanel(new GridBagLayout());
    protected final JPanel bottomRightPanel = new JPanel(new GridBagLayout());
    protected final JTextField nameInput = new JTextField(defaultColumns);
    protected final JTextField typeInput = new JTextField(defaultColumns);

    private int topPanelRow = 0;
    private int bottomPanelCol = 0;

    protected AbstractValueForm() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(topLeftPanel, BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomRightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        bottomPanel.add(bottomRightPanel, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        topLeftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addRow("名称:", nameInput);

        typeInput.setEditable(false);
        addRow("类型:", typeInput);

        JButton saveBtn = new JButton("保存");
        saveBtn.addActionListener(e -> FormSaveHandler.saveClick());
        addButton(saveBtn);
    }

    protected void addRow(String label, JComponent field) {
        // 标签 gbc
        GridBagConstraints labelGbc = baseGbc();
        labelGbc.gridx = 0;
        labelGbc.gridy = topPanelRow;
        labelGbc.weightx = 0; // 标签不拉伸
        topLeftPanel.add(new JLabel(label), labelGbc);

        // 输入框 gbc
        GridBagConstraints fieldGbc = baseGbc();
        fieldGbc.gridx = 1;
        fieldGbc.gridy = topPanelRow;
        fieldGbc.weightx = 1.0; // 输入框水平拉伸
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        topLeftPanel.add(field, fieldGbc);

        topPanelRow++;
    }

    protected void addButton(JButton button) {
        // 标签 gbc
        GridBagConstraints gbc = baseGbc();
        gbc.gridx = bottomPanelCol;
        gbc.gridy = 0;
        bottomRightPanel.add(button, gbc);

        bottomPanelCol++;
    }

    protected GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    protected void setData(String name, String type) {
        nameInput.setText(name);
        typeInput.setText(type);
    }

    @Override
    public abstract NodeFormData getData();
}

