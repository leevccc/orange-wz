package orange.wz.gui.utils;

import orange.wz.gui.MainFrame;

import javax.swing.tree.TreePath;

public final class TreePathUtil {
    public static boolean isNullOrMultiple(TreePath[] treePaths) {
        if (treePaths == null) return true;
        if (treePaths.length != 1) {
            JMessageUtil.error(MainFrame.i18n.get("tree.notMulti"));
            return true;
        }

        return false;
    }
}
