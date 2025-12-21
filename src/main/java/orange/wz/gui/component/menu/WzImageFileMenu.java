package orange.wz.gui.component.menu;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.WzImageFile;
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
public final class WzImageFileMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;

    public WzImageFileMenu(EditPane editPane,JTree tree) {
        super();
        this.editPane = editPane;
        this.tree = tree;

        JMenuItem saveBtn = new JMenuItem("保存", AiOutlineSaveIcon);
        JMenuItem unloadBtn = new JMenuItem("卸载", AiOutlineCloseIcon);
        JMenuItem reloadItem = new JMenuItem("重载", AiOutlineReloadIcon);

        saveBtnAction(saveBtn);
        unloadBtnAction(unloadBtn);
        reloadItemAction(reloadItem);

        add(saveBtn);
        add(unloadBtn);
        add(reloadItem);
    }

    private void saveBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length == 1) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                int index = pNode.getIndex(node);
                WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
                byte[] iv = wzImageFile.getIv();
                byte[] key = wzImageFile.getKey();
                if (!wzImageFile.isLoad()) {
                    log.warn("未加载的文件 {} 无需保存", wzImageFile.getName());
                    return;
                }

                File oldFile = new File(wzImageFile.getFilePath());
                File newFile = new File(oldFile.getParent(), wzImageFile.getName());

                File saveFile = FileDialog.chooseSaveFile(MainFrame.getInstance(), "保存 " + wzImageFile.getName(), newFile, new String[]{"img"});
                if (saveFile == null) {
                    return;
                }
                Path filePath = Path.of(saveFile.getAbsolutePath());
                wzImageFile.save(filePath);
                editPane.removeNodeFromTree(node);
                String filename = filePath.getFileName().toString();
                wzImageFile = new WzImageFile(filename, filePath.toString(), iv, key);
                editPane.insertNodeToTree(pNode, wzImageFile, true, index);
            } else {
                // 批量保存的时候判断文件名是否发生改变，如果发生改变，跳过并提示。
                Set<String> failed = new HashSet<>();
                for (TreePath treePath : selectedPaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                    int index = pNode.getIndex(node);
                    WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
                    byte[] iv = wzImageFile.getIv();
                    byte[] key = wzImageFile.getKey();
                    if (!wzImageFile.isLoad()) {
                        log.warn("未加载的文件 {} 无需保存", wzImageFile.getName());
                        continue;
                    }

                    Path filePath = Path.of(wzImageFile.getFilePath());
                    if (!filePath.getFileName().toString().equals(wzImageFile.getName())) {
                        failed.add(wzImageFile.getName());
                        log.error("批量保存无法用于文件改名 {} : {}", wzImageFile.getName(), wzImageFile.getFilePath());
                        continue;
                    }

                    wzImageFile.save(filePath);
                    String filename = filePath.getFileName().toString();
                    editPane.removeNodeFromTree(node);
                    wzImageFile = new WzImageFile(filename, filePath.toString(), iv, key);
                    editPane.insertNodeToTree(pNode, wzImageFile, true, index);
                }

                if (!failed.isEmpty()) {
                    JMessageUtil.warn("批量保存无法用于文件改名, 这些文件请手动保存: " + String.join(", ", failed));
                }
            }

            System.gc();
        });
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

    private void reloadItemAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            WzKey key = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                int index = pNode.getIndex(node);
                WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
                Path filePath = Path.of(wzImageFile.getFilePath());
                String filename = filePath.getFileName().toString();

                editPane.removeNodeFromTree(node);
                wzImageFile = new WzImageFile(filename, filePath.toString(), key.getIv(), key.getUserKey());
                editPane.insertNodeToTree(pNode, wzImageFile, true, index);
            }

            System.gc();
        });
    }
}
