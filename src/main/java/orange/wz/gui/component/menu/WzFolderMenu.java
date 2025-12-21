package orange.wz.gui.component.menu;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import static orange.wz.gui.Icons.AiOutlineCloseIcon;

@Slf4j
public final class WzFolderMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;

    public WzFolderMenu(EditPane editPane, JTree tree) {
        super();
        this.editPane = editPane;
        this.tree = tree;

        JMenuItem unloadBtn = new JMenuItem("卸载", AiOutlineCloseIcon);

        unloadBtnAction(unloadBtn);

        add(unloadBtn);
    }

    private void unloadBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
            }

            System.gc();
        });
    }
}
