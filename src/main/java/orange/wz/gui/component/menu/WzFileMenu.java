package orange.wz.gui.component.menu;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.FileDialog;
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
import java.util.HashSet;
import java.util.Set;

import static orange.wz.gui.Icons.*;

@Slf4j
public final class WzFileMenu {
    public static JPopupMenu create() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem saveBtn = new JMenuItem("保存", AiOutlineSaveIcon);
        JMenuItem unloadBtn = new JMenuItem("卸载", AiOutlineCloseIcon);
        JMenuItem reloadItem = new JMenuItem("重载", AiOutlineReloadIcon);

        saveBtnAction(saveBtn);
        unloadBtnAction(unloadBtn);
        reloadItemAction(reloadItem);

        popupMenu.add(saveBtn);
        popupMenu.add(unloadBtn);
        popupMenu.add(reloadItem);

        return popupMenu;
    }

    private static void saveBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = MainFrame.getInstance().getTree().getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length == 1) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                int index = pNode.getIndex(node);
                WzFile wzFile = ((WzDirectory) node.getUserObject()).getWzFile();
                short version = wzFile.getHeader().getFileVersion();
                byte[] iv = wzFile.getWzIv();
                byte[] key = wzFile.getUserKey();
                if (!wzFile.isLoad()) {
                    log.warn("未加载的文件 {} 无需保存", wzFile.getName());
                    return;
                }

                File oldFile = new File(wzFile.getFilePath());
                File newFile = new File(oldFile.getParent(), wzFile.getName());

                File saveFile = FileDialog.chooseSaveFile(MainFrame.getInstance(), "保存 " + wzFile.getName(), newFile, new String[]{"wz"});
                if (saveFile == null) {
                    return;
                }
                String filePath = saveFile.getAbsolutePath();
                wzFile.save(filePath);
                JTreeUtil.remove(node);
                wzFile = new WzFile(filePath, version, iv, key);
                MainFrame.getInstance().insertNodeToTree(pNode, wzFile.getWzDirectory(), true, index);
            } else {
                // 批量保存的时候判断文件名是否发生改变，如果发生改变，跳过并提示。
                Set<String> failed = new HashSet<>();
                for (TreePath treePath : selectedPaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                    int index = pNode.getIndex(node);
                    WzFile wzFile = ((WzDirectory) node.getUserObject()).getWzFile();
                    String filePath = wzFile.getFilePath();
                    short version = wzFile.getHeader().getFileVersion();
                    byte[] iv = wzFile.getWzIv();
                    byte[] key = wzFile.getUserKey();
                    if (!wzFile.isLoad()) {
                        log.warn("未加载的文件 {} 无需保存", wzFile.getName());
                        continue;
                    }

                    if (!Path.of(wzFile.getFilePath()).getFileName().toString().equals(wzFile.getName())) {
                        failed.add(wzFile.getName());
                        log.error("批量保存无法用于文件改名 {} : {}", wzFile.getName(), wzFile.getFilePath());
                        continue;
                    }

                    wzFile.save();
                    JTreeUtil.remove(node);
                    wzFile = new WzFile(filePath, version, iv, key);
                    MainFrame.getInstance().insertNodeToTree(pNode, wzFile.getWzDirectory(), true, index);
                }

                if (!failed.isEmpty()) {
                    JMessageUtil.warn("批量保存无法用于文件改名, 这些文件请手动保存: " + String.join(", ", failed));
                }
            }

            System.gc();
        });
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

    private static void reloadItemAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = MainFrame.getInstance().getTree().getSelectionPaths();
            if (selectedPaths == null) return;

            WzKey key = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                int index = pNode.getIndex(node);
                WzFile wzFile = ((WzDirectory) node.getUserObject()).getWzFile();
                String filePath = wzFile.getFilePath();
                short version = wzFile.getHeader().getFileVersion();

                JTreeUtil.remove(node);
                wzFile = new WzFile(filePath, version, key.getIv(), key.getUserKey());
                MainFrame.getInstance().insertNodeToTree(pNode, wzFile.getWzDirectory(), true, index);
            }

            System.gc();
        });
    }
}
