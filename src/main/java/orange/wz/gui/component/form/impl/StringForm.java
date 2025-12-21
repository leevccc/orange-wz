package orange.wz.gui.component.form.impl;

import orange.wz.gui.component.form.data.StringFormData;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class StringForm extends AbstractValueForm {
    protected final JTextArea valueInput = new JTextArea(1, defaultColumns);

    public StringForm() {
        super();
        initialValueInput();
        addRow("值:", valueInput);
    }

    private void initialValueInput() {
        valueInput.setLineWrap(true);
        valueInput.setWrapStyleWord(true);

        // 禁止回车（如果你前面那个需求还在）
        valueInput.getInputMap().put(
                KeyStroke.getKeyStroke("ENTER"),
                "none"
        );

        // 自动增高
        valueInput.getDocument().addDocumentListener(new DocumentListener() {
            private void updateHeight() {
                int lineCount = valueInput.getLineCount();

                // 最少 1 行（防止被清空后高度塌陷）
                valueInput.setRows(Math.max(1, lineCount));

                valueInput.revalidate(); // 触发布局更新
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateHeight();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateHeight();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    public void setData(String name, String type, String value) {
        super.setData(name, type);
        valueInput.setText(value);
    }

    @Override
    public StringFormData getData() {
        return new StringFormData(
                nameInput.getText(),
                typeInput.getText(),
                valueInput.getText()
        );
    }
}
