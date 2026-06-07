package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.SearchFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class SearchDialog extends BaseDialog<SearchFormData> {
    private final JTextField searchField = new JTextField(20);
    private final JCheckBox checkNameMod = new JCheckBox(MainFrame.i18n.get("test.temp0136"));
    private final JCheckBox checkValueMod = new JCheckBox(MainFrame.i18n.get("test.temp0137"));
    private final JCheckBox checkEqualMod = new JCheckBox(MainFrame.i18n.get("test.temp0138"));
    private final JCheckBox checkLowMod = new JCheckBox(MainFrame.i18n.get("test.temp0139"));
    private final JRadioButton checkGlobalMod = new JRadioButton(MainFrame.i18n.get("test.temp0140"));
    private final JCheckBox checkParseImgMod = new JCheckBox(MainFrame.i18n.get("test.temp0141"));

    public SearchDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow(MainFrame.i18n.get("test.temp0142"), searchField);

        JPanel options1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options1.add(checkNameMod);
        checkNameMod.setSelected(true);
        options1.add(checkValueMod);
        options1.add(checkEqualMod);
        options1.add(checkLowMod);
        addRow(MainFrame.i18n.get("test.temp0143"), options1);

        ButtonGroup mediaGroup = new ButtonGroup();
        JRadioButton checkSelectedMod = new JRadioButton(MainFrame.i18n.get("test.temp0144"));
        mediaGroup.add(checkSelectedMod);
        checkSelectedMod.setSelected(true);
        mediaGroup.add(checkGlobalMod);
        JPanel options2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options2.add(checkSelectedMod);
        options2.add(checkGlobalMod);
        addRow(MainFrame.i18n.get("test.temp0145"), options2);

        JPanel options3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options3.add(checkParseImgMod);
        checkParseImgMod.setSelected(true);
        addRow(MainFrame.i18n.get("test.temp0146"), options3);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (searchField.isVisible() && searchField.isEnabled()) {
                    searchField.requestFocusInWindow();
                    searchField.selectAll();
                }
            }
        });
    }

    @Override
    public SearchFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        if (searchField.getText().isBlank()) return null;

        return new SearchFormData(
                searchField.getText(),
                checkNameMod.isSelected(),
                checkValueMod.isSelected(),
                checkEqualMod.isSelected(),
                checkLowMod.isSelected(),
                checkParseImgMod.isSelected(),
                checkGlobalMod.isSelected()
        );
    }
}
