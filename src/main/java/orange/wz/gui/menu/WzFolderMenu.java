package orange.wz.gui.menu;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.FileDialog;
import orange.wz.gui.MainFrame;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.gui.utils.JTreeUtil;
import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzFile;
import orange.wz.utils.wzkey.WzKey;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
