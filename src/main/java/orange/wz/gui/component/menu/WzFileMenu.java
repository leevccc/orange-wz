package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.dialog.NodeDialog;
import orange.wz.gui.component.dialog.OverwriteChoice;
import orange.wz.gui.component.dialog.OverwriteDialog;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzFile;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.utils.wzkey.WzKey;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static orange.wz.gui.Icons.*;

@Slf4j
public final class WzFileMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;
    @Getter
    private final JMenuItem pasteBtn;

    public WzFileMenu(EditPane editPane, JTree tree) {
        super();
        this.editPane = editPane;
        this.tree = tree;

        JMenu addBtn = new JMenu("子节点");
        addBtn.setIcon(AiOutlinePlus);
        JMenuItem addDirBtn = new JMenuItem("Directory");
        JMenuItem addImgBtn = new JMenuItem("Image");
        addBtn.add(addDirBtn);
        addBtn.add(addImgBtn);
        JMenuItem saveBtn = new JMenuItem("保存", AiOutlineSaveIcon);
        JMenuItem unloadBtn = new JMenuItem("卸载", AiOutlineCloseIcon);
        JMenuItem reloadBtn = new JMenuItem("重载", AiOutlineReloadIcon);
        JMenuItem moveBtn = new JMenuItem("切换视图", AiOutlineEye);
        pasteBtn = new JMenuItem("粘贴", MdOutlineContentPaste);

        addDirBtnAction(addDirBtn);
        addImgBtnAction(addImgBtn);
        saveBtnAction(saveBtn);
        unloadBtnAction(unloadBtn);
        reloadBtnAction(reloadBtn);
        moveBtnAction(moveBtn);
        addPasteBtnAction(pasteBtn);

        add(addBtn);
        add(saveBtn);
        add(unloadBtn);
        add(reloadBtn);
        add(moveBtn);
        add(pasteBtn);
    }

    private void saveBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
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
                editPane.removeNodeFromTree(node);
                wzFile = new WzFile(filePath, version, iv, key);
                editPane.insertNodeToTree(pNode, wzFile.getWzDirectory(), true, index);
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
                    editPane.removeNodeFromTree(node);
                    wzFile = new WzFile(filePath, version, iv, key);
                    editPane.insertNodeToTree(pNode, wzFile.getWzDirectory(), true, index);
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

    private void reloadBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            WzKey key = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                int index = pNode.getIndex(node);
                WzFile wzFile = ((WzDirectory) node.getUserObject()).getWzFile();
                String filePath = wzFile.getFilePath();
                short version = wzFile.getHeader().getFileVersion();

                editPane.removeNodeFromTree(node);
                wzFile = new WzFile(filePath, version, key.getIv(), key.getUserKey());
                editPane.insertNodeToTree(pNode, wzFile.getWzDirectory(), true, index);
            }

            System.gc();
        });
    }

    private void moveBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            if (!MainFrame.getInstance().getCenterPane().isRightShowing()) {
                MainFrame.getInstance().getCenterPane().showRightEditPane(true);
            }

            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            EditPane targetPane = MainFrame.getInstance().getCenterPane().getAnotherPane(editPane);
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
                targetPane.insertNodeToTree(targetPane.getTreeRoot(), wzDirectory, true);
                editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
            }
        });
    }

    private void addPasteBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
            WzDirectory target = (WzDirectory) node.getUserObject();

            Clipboard clipboard = MainFrame.getInstance().getClipboard();
            clipboard.lock();

            if (clipboard.canPaste(target)) {
                List<WzObject> objects = clipboard.getItems();
                setPasteParent(objects, target);
                setPasteWzFile(objects, target.getWzFile());
                setPasteImgReader(objects, target.getWzFile()); // 重设为新文件的reader主要是key的传递，不得再做读取

                OverwriteChoice choice = null;
                for (WzObject obj : objects) {
                    obj.setTempChanged(true);
                    int index = 0;
                    if (obj instanceof WzDirectory dir) {
                        if (target.existDirectory(dir.getName())) { // 发现重名
                            if (choice == OverwriteChoice.SKIP_ALL) continue;
                            else if (choice == OverwriteChoice.OVERWRITE_ALL) {
                                target.removeDirectoryChild(dir.getName());
                                DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, dir.getName());
                                index = node.getIndex(childNode);
                                editPane.removeNodeFromTree(childNode);
                            } else {
                                choice = OverwriteDialog.show(editPane, dir.getName());
                                switch (choice) {
                                    case OVERWRITE, OVERWRITE_ALL -> {
                                        target.removeDirectoryChild(dir.getName());
                                        DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, dir.getName());
                                        index = node.getIndex(childNode);
                                        editPane.removeNodeFromTree(childNode);
                                    }
                                    case SKIP, SKIP_ALL, CANCEL -> {
                                        continue;
                                    }
                                }
                            }
                        }
                        target.addChild(dir);
                    } else if (obj instanceof WzImage img) {
                        if (target.existImage(img.getName())) { // 发现重名
                            if (choice == OverwriteChoice.SKIP_ALL) continue;
                            else if (choice == OverwriteChoice.OVERWRITE_ALL) {
                                target.removeImageChild(img.getName());
                                DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, img.getName());
                                index = node.getIndex(childNode);
                                editPane.removeNodeFromTree(childNode);
                            } else {
                                choice = OverwriteDialog.show(editPane, img.getName());
                                switch (choice) {
                                    case OVERWRITE, OVERWRITE_ALL -> {
                                        target.removeImageChild(img.getName());
                                        DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, img.getName());
                                        index = node.getIndex(childNode);
                                        editPane.removeNodeFromTree(childNode);
                                    }
                                    case SKIP, SKIP_ALL, CANCEL -> {
                                        continue;
                                    }
                                }
                            }
                        }
                        target.addChild(img);
                    }
                    editPane.insertNodeToTree(node, obj, true, index);
                }
            } else {
                JMessageUtil.error("WzFile 只能粘贴 Directory 或者 Image");
            }

            clipboard.clear();
            clipboard.unlock();
        });
    }

    private void setPasteParent(List<WzObject> objects, WzObject parent) {
        for (WzObject obj : objects) {
            obj.setParent(parent);
        }
    }

    private void setPasteWzFile(List<WzObject> objects, WzFile wzFile) {
        for (WzObject obj : objects) {
            if (obj instanceof WzDirectory dir) {
                dir.setWzFile(wzFile);
                setPasteWzFile(dir.getChildren(), wzFile);
            }
        }
    }

    private void setPasteImgReader(List<WzObject> objects, WzFile wzFile) {
        for (WzObject obj : objects) {
            if (obj instanceof WzDirectory dir) {
                setPasteImgReader(dir.getChildren(), wzFile);
            } else if (obj instanceof WzImage img) {
                img.setReader(wzFile.getReader());
            }
        }
    }

    private void addDirBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            NodeDialog nodeDialog = new NodeDialog("新增 Directory", editPane);
            NodeFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
            WzFile wzFile = wzDirectory.getWzFile();
            wzFile.load();

            WzDirectory newDir = new WzDirectory(name, wzDirectory, wzFile);
            if (!wzDirectory.addChild(newDir)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            editPane.insertNodeToTree(node, newDir, true, 0);
        });
    }

    private void addImgBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            NodeDialog nodeDialog = new NodeDialog("新增 Image", editPane);
            NodeFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            if (!name.endsWith(".img")) {
                JMessageUtil.error("Image 名称需要以.img结尾");
                return;
            }

            WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
            WzFile wzFile = wzDirectory.getWzFile();
            wzFile.load();

            WzImage newImg = new WzImage(name, wzFile.getReader(), wzDirectory);
            newImg.setParsed(true);
            newImg.setChanged(true);
            if (!wzDirectory.addChild(newImg)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            editPane.insertNodeToTree(node, newImg, true, 0);
        });
    }
}
