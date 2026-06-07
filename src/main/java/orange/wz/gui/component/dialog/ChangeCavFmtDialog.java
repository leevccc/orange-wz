package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.base.DisabledItemComboBox;
import orange.wz.gui.component.form.data.CanvasFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.provider.properties.WzPngFormat;

import javax.swing.*;

public final class ChangeCavFmtDialog extends BaseDialog<CanvasFormData> {
    private final DisabledItemComboBox<WzPngFormat> formatField;

    public ChangeCavFmtDialog(EditPane editPane) {
        super(MainFrame.i18n.get("test.temp0075"), editPane);

        formatField = new DisabledItemComboBox<>(WzPngFormat.values());
        formatField.setSelectedItem(WzPngFormat.ARGB8888);
        addRow(MainFrame.i18n.get("test.temp0076"), formatField);
    }

    @Override
    public CanvasFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        return new CanvasFormData(null,
                null,
                null,
                (WzPngFormat) formatField.getSelectedItem(),
                0
        );
    }
}
