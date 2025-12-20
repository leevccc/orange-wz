package orange.wz.gui.utils;

import orange.wz.gui.MainFrame;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public final class JTreeUtil {
    public static void remove(DefaultMutableTreeNode node) {
        JTree tree = MainFrame.getInstance().getTree();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

        if (node == null) return;
        if (node.getParent() == null) return;

        model.removeNodeFromParent(node);
    }


}
