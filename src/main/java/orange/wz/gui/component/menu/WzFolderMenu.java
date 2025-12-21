package orange.wz.gui.component.menu;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.gui.utils.JTreeUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

@Slf4j
public final class WzFolderMenu {
    public static JPopupMenu create() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem unloadBtn = new JMenuItem("卸载", MainFrame.getSVG("AiOutlineClose.svg", 16, 16));

        unloadBtnAction(unloadBtn);

        popupMenu.add(unloadBtn);

        return popupMenu;
    }

    private static void unloadBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = MainFrame.getInstance().getTree().getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                JTreeUtil.remove((DefaultMutableTreeNode) treePath.getLastPathComponent());
            }

            System.gc();
        });
    }
}
