package orange.wz.gui.component.dialog;

import javax.swing.*;
import java.awt.*;

public final class OverwriteDialog {

    private OverwriteDialog() {
    }

    public static OverwriteChoice show(Component parent, String name) {

        String[] options = {
                "覆盖",
                "跳过",
                "全部覆盖",
                "全部跳过"
        };

        int result = JOptionPane.showOptionDialog(
                parent,
                name + " 已存在，是否覆盖？",
                "确认操作",
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

