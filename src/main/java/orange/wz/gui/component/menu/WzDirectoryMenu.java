package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.canvas.CanvasWall;
import orange.wz.gui.component.dialog.NodeDialog;
import orange.wz.gui.component.dialog.OverwriteChoice;
import orange.wz.gui.component.dialog.OverwriteDialog;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.CanvasUtil;
import orange.wz.gui.utils.CanvasUtilData;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static orange.wz.gui.Icons.*;

@Slf4j
public final class WzDirectoryMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;
    @Getter
    private final JMenuItem deleteBtn;
    @Getter
    private final JMenuItem copyBtn;
    @Getter
    private final JMenuItem pasteBtn;

    public WzDirectoryMenu(EditPane editPane, JTree tree) {
        super();
        this.editPane = editPane;
        this.tree = tree;

        JMenu addBtn = new JMenu("子节点");
        addBtn.setIcon(AiOutlinePlus);
        JMenuItem addDirBtn = new JMenuItem("Directory");
        JMenuItem addImgBtn = new JMenuItem("Image");
        addBtn.add(addDirBtn);
        addBtn.add(addImgBtn);

        copyBtn = new JMenuItem("复制", AiOutlineCopy);
        pasteBtn = new JMenuItem("粘贴", MdOutlineContentPaste);
        deleteBtn = new JMenuItem("删除节点", AiOutlineDelete);
        JMenuItem imageBtn = new JMenuItem("图片嗅探");
        JMenuItem importBtn = new JMenu("导入");
        JMenuItem importImgBtn = new JMenuItem("Img");
        JMenuItem importXmlBtn = new JMenuItem("Xml");
        importBtn.add(importImgBtn);
        importBtn.add(importXmlBtn);

        addDirBtnAction(addDirBtn);
        addImgBtnAction(addImgBtn);
        addCopyBtnAction(copyBtn);
        addPasteBtnAction(pasteBtn);
        deleteBtnAction(deleteBtn);
        addImageBtnAction(imageBtn);
        addImportImgBtnAction(importImgBtn);
        addImportXmlBtnAction(importXmlBtn);

        add(addBtn);
        add(copyBtn);
        add(pasteBtn);
        add(deleteBtn);
        add(imageBtn);
        add(importBtn);
    }

    private void addCopyBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            Clipboard clipboard = MainFrame.getInstance().getClipboard();
            clipboard.lock();
            clipboard.clear();
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzDirectory wzObject = (WzDirectory) node.getUserObject();
                clipboard.add(wzObject.deepClone(null));
            }
            clipboard.unlock();
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
                JMessageUtil.error("Directory 只能粘贴 Directory 或者 Image");
            }

            editPane.resetValueForm();
            clipboard.clear();
            clipboard.unlock();
        });
    }

    private void setPasteParent(List<? extends WzObject> objects, WzObject parent) {
        for (WzObject obj : objects) {
            obj.setParent(parent);
            if (obj instanceof WzDirectory directory) {
                setPasteParent(directory.getChildren(), directory);
            } else if (obj instanceof WzImage image) {
                setPasteParent(image.getChildren(), image);
            } else if (obj instanceof WzImageProperty prop && prop.isListProperty()) {
                setPasteParent(prop.getChildren(), prop);
            }
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

    private void deleteBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzObject wzObject = (WzObject) node.getUserObject();
                WzObject pWzObject = wzObject.getParent();

                if (pWzObject instanceof WzDirectory directory && directory.removeImageChild(wzObject.getName())) {
                    editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
                } else {
                    log.error("无法删除节点, 父节点类型: {}", pWzObject.getClass().getName());
                }
            }

            editPane.resetValueForm();
        });
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
            if (!wzFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzFile.getName(), wzFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzDirectory newDir = new WzDirectory(name, wzDirectory, wzFile);
            if (!wzDirectory.addChild(newDir)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            newDir.setTempChanged(true);
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
            if (!wzFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzFile.getName(), wzFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzImage newImg = new WzImage(name, wzDirectory, wzFile.getReader());
            if (!wzDirectory.addChild(newImg)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            newImg.setTempChanged(true);
            editPane.insertNodeToTree(node, newImg, true, 0);
        });
    }

    private void addImageBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("该功能不支持多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
            WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
            List<CanvasUtilData> data = new ArrayList<>();
            CanvasUtil.search(data, wzDirectory.getChildren());

            CanvasWall canvasWall = new CanvasWall(data, wzDirectory.getPath(), node, editPane);
            canvasWall.setVisible(true);
        });
    }

    private void addImportImgBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;
            if (selectedPaths.length != 1) {
                JMessageUtil.error("该功能不支持多选");
                return;
            }

            List<File> imgFiles = FileDialog.chooseOpenFiles(new String[]{"img"});
            final int[] count = {0};
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                    WzDirectory target = (WzDirectory) node.getUserObject();
                    WzFile wzFile = target.getWzFile();
                    String keyBoxName = wzFile.getKeyBoxName();
                    byte[] iv = wzFile.getIv();
                    byte[] key = wzFile.getKey();
                    int total = imgFiles.size();
                    OverwriteChoice choice = null;
                    int index = 0;
                    for (File imgFile : imgFiles) {
                        String name = imgFile.getName();
                        if (target.existImage(name)) {
                            if (choice == OverwriteChoice.SKIP_ALL) continue;
                            else if (choice == OverwriteChoice.OVERWRITE_ALL) {
                                target.removeImageChild(name);
                                DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, name);
                                index = node.getIndex(childNode);
                                editPane.removeNodeFromTree(childNode);
                            } else {
                                choice = OverwriteDialog.show(editPane, name);
                                switch (choice) {
                                    case OVERWRITE, OVERWRITE_ALL -> {
                                        target.removeImageChild(name);
                                        DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, name);
                                        index = node.getIndex(childNode);
                                        editPane.removeNodeFromTree(childNode);
                                    }
                                    case SKIP, SKIP_ALL, CANCEL -> {
                                        continue;
                                    }
                                }
                            }
                        }
                        WzImageFile wzImageFile = new WzImageFile(name, imgFile.getAbsolutePath(), keyBoxName, iv, key);
                        wzImageFile.parse();
                        WzImage wzImage = wzImageFile.deepClone(target);
                        wzImage.setTempChanged(true);
                        target.addChild(wzImage);
                        editPane.insertNodeToTree(node, wzImage, true, index);
                        MainFrame.getInstance().updateProgress(++count[0], total);
                    }

                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        MainFrame.getInstance().setStatusText("共导入 %d 个文件", count[0]);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }.execute();

        });
    }

    private void addImportXmlBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;
            if (selectedPaths.length != 1) {
                JMessageUtil.error("该功能不支持多选");
                return;
            }

            List<File> xmlFiles = FileDialog.chooseOpenFiles(new String[]{"xml"});
            final int[] count = {0};
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                    WzDirectory target = (WzDirectory) node.getUserObject();
                    WzFile wzFile = target.getWzFile();
                    String keyBoxName = wzFile.getKeyBoxName();
                    byte[] iv = wzFile.getIv();
                    byte[] key = wzFile.getKey();
                    int total = xmlFiles.size();
                    OverwriteChoice choice = null;
                    int index = 0;
                    for (File xmlFile : xmlFiles) {
                        String name = xmlFile.getName();
                        name = name.endsWith(".xml")
                                ? name.substring(0, name.length() - 4)
                                : name;
                        if (target.existImage(name)) {
                            if (choice == OverwriteChoice.SKIP_ALL) continue;
                            else if (choice == OverwriteChoice.OVERWRITE_ALL) {
                                target.removeImageChild(name);
                                DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, name);
                                index = node.getIndex(childNode);
                                editPane.removeNodeFromTree(childNode);
                            } else {
                                choice = OverwriteDialog.show(editPane, name);
                                switch (choice) {
                                    case OVERWRITE, OVERWRITE_ALL -> {
                                        target.removeImageChild(name);
                                        DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, name);
                                        index = node.getIndex(childNode);
                                        editPane.removeNodeFromTree(childNode);
                                    }
                                    case SKIP, SKIP_ALL, CANCEL -> {
                                        continue;
                                    }
                                }
                            }
                        }
                        WzXmlFile wzXmlFile = new WzXmlFile(name, xmlFile.getAbsolutePath(), keyBoxName, iv, key);
                        wzXmlFile.parse();
                        WzImage wzImage = wzXmlFile.deepClone(target);
                        wzImage.setTempChanged(true);
                        target.addChild(wzImage);
                        editPane.insertNodeToTree(node, wzImage, true, index);
                        MainFrame.getInstance().updateProgress(++count[0], total);
                    }

                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        MainFrame.getInstance().setStatusText("共导入 %d 个文件", count[0]);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }.execute();
        });
    }
}
