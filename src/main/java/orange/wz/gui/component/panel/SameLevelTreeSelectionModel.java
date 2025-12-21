package orange.wz.gui.component.panel;

import javax.swing.*;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class SameLevelTreeSelectionModel extends DefaultTreeSelectionModel {
    private Runnable rejectCallback;

    public void onReject(Runnable callback) {
        this.rejectCallback = callback;
    }

    private boolean isSameParent(TreePath[] paths) {
        if (paths == null || paths.length <= 1) return true;

        TreePath parent = paths[0].getParentPath();
        for (TreePath p : paths) {
            if (!Objects.equals(parent, p.getParentPath())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setSelectionPaths(TreePath[] paths) {
        if (isSameParent(paths)) {
            super.setSelectionPaths(paths);
        } else {
            notifyReject();
        }
    }

    @Override
    public void addSelectionPaths(TreePath[] paths) {
        TreePath[] current = getSelectionPaths();
        if (current == null || current.length == 0) {
            super.addSelectionPaths(paths);
            return;
        }

        TreePath[] merged = Stream
                .concat(Arrays.stream(current), Arrays.stream(paths))
                .distinct()
                .toArray(TreePath[]::new);

        if (isSameParent(merged)) {
            super.addSelectionPaths(paths);
        } else {
            notifyReject();
        }
    }

    private void notifyReject() {
        if (rejectCallback != null) {
            SwingUtilities.invokeLater(rejectCallback);
        }
    }
}
