package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;

import javax.swing.*;
import java.awt.*;

public final class OverwriteDialog {

    public static OverwriteChoice show(Component parent, String name) {

        String[] options = {
                MainFrame.i18n.get("test.temp0112"),
                MainFrame.i18n.get("test.temp0113"),
                MainFrame.i18n.get("test.temp0114"),
                MainFrame.i18n.get("test.temp0115")
        };

        int result = JOptionPane.showOptionDialog(
                parent,
                name + MainFrame.i18n.get("test.temp0116"),
                MainFrame.i18n.get("test.temp0117"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );

        return switch (result) {
            case 0 -> OverwriteChoice.OVERWRITE;
            case 1 -> OverwriteChoice.SKIP;
            case 2 -> OverwriteChoice.OVERWRITE_ALL;
            case 3 -> OverwriteChoice.SKIP_ALL;
            default -> OverwriteChoice.CANCEL;
        };
    }
}

