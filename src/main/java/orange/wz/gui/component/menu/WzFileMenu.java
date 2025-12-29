package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.dialog.*;
import orange.wz.gui.component.form.data.ExportXmlData;
import orange.wz.gui.component.form.data.KeyData;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.ChineseUtil;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.gui.utils.Outlink;
import orange.wz.provider.*;
import orange.wz.provider.tools.WzFileStatus;
import orange.wz.provider.tools.wzkey.WzKey;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
        JMenuItem moveBtn = new JMenuItem("转移视图", AiOutlineEye);
        pasteBtn = new JMenuItem("粘贴", MdOutlineContentPaste);
        JMenuItem keyBtn = new JMenuItem("修改密钥", AiOutlineKey);
        JMenu exportBtn = new JMenu("导出");
        JMenuItem exportImgBtn = new JMenuItem("Img");
        JMenuItem exportXmlBtn = new JMenuItem("Xml");
        exportBtn.add(exportImgBtn);
        exportBtn.add(exportXmlBtn);
        JMenuItem importBtn = new JMenu("导入");
        JMenuItem importImgBtn = new JMenuItem("Img");
        JMenuItem importXmlBtn = new JMenuItem("Xml");
        importBtn.add(importImgBtn);
        importBtn.add(importXmlBtn);
        JMenuItem chineseBtn = new JMenuItem("汉化");
        JMenuItem outlinkBtn = new JMenuItem("Outlink");


        addDirBtnAction(addDirBtn);
        addImgBtnAction(addImgBtn);
        saveBtnAction(saveBtn);
        unloadBtnAction(unloadBtn);
        reloadBtnAction(reloadBtn);
        moveBtnAction(moveBtn);
        addPasteBtnAction(pasteBtn);
        addKeyBtnAction(keyBtn);
        addExportImgBtnAction(exportImgBtn);
        addExportXmlBtnAction(exportXmlBtn);
        addImportImgBtnAction(importImgBtn);
        addImportXmlBtnAction(importXmlBtn);
        addChineseBtnAction(chineseBtn);
        addOutlinkBtnAction(outlinkBtn);

        add(addBtn);
        add(saveBtn);
        add(unloadBtn);
        add(reloadBtn);
        add(moveBtn);
        add(pasteBtn);
        add(keyBtn);
        add(exportBtn);
        add(importBtn);
        add(chineseBtn);
        add(outlinkBtn);
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
                String keyBoxName = wzFile.getKeyBoxName();
                byte[] iv = wzFile.getWzIv();
                byte[] key = wzFile.getUserKey();
                if (wzFile.getStatus() != WzFileStatus.PARSE_SUCCESS) {
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
                wzFile = new WzFile(filePath, version, keyBoxName, iv, key);
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
                    String keyBoxName = wzFile.getKeyBoxName();
                    byte[] iv = wzFile.getWzIv();
                    byte[] key = wzFile.getUserKey();
                    if (wzFile.getStatus() != WzFileStatus.PARSE_SUCCESS) {
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
                    wzFile = new WzFile(filePath, version, keyBoxName, iv, key);
                    editPane.insertNodeToTree(pNode, wzFile.getWzDirectory(), true, index);
                }

                if (!failed.isEmpty()) {
                    JMessageUtil.warn("批量保存无法用于文件改名, 这些文件请手动保存: " + String.join(", ", failed));
                }
            }

            editPane.resetValueForm();
            System.gc();
        });
    }

    private void unloadBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzDirectory wzFile = (WzDirectory) node.getUserObject();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                if (pNode == null) continue;
                if (pNode.getUserObject() instanceof WzFolder wzFolder) {
                    wzFolder.remove(wzFile);
                }
            }

            editPane.resetValueForm();
            System.gc();
        });
    }

    private void reloadBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            WzKey key = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();
            if (key == null) {
                MainFrame.getInstance().setStatusText("没有选择密钥?");
                return;
            }
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                int index = pNode.getIndex(node);
                WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
                WzFile wzFileOld = wzDirectory.getWzFile();
                String filePath = wzFileOld.getFilePath();
                short version = wzFileOld.getHeader().getFileVersion();

                editPane.removeNodeFromTree(node);

                WzFile wzFileNew = new WzFile(filePath, version, key.getName(), key.getIv(), key.getUserKey());
                editPane.insertNodeToTree(pNode, wzFileNew.getWzDirectory(), true, index);

                if (pNode.getUserObject() instanceof WzFolder wzFolder) {
                    wzFolder.remove(wzFileOld.getWzDirectory());
                    wzFolder.add(wzFileNew.getWzDirectory());
                }
            }

            editPane.resetValueForm();
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
            editPane.resetValueForm();
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

    private void addKeyBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            ChangeKeyDialog dialog = new ChangeKeyDialog(editPane, true);
            KeyData keyData = dialog.getData();

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
                WzFile wzFile = wzDirectory.getWzFile();
                wzFile.changeKey(keyData.getVersion(), keyData.getIv(), keyData.getKey());
            }
        });
    }

    private void addExportImgBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            File folder = FileDialog.chooseOpenFolder("请选择输出目录");
            if (folder == null) {
                log.info("用户取消了操作");
                return;
            }

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
                WzFile wzFile = wzDirectory.getWzFile();
                if (wzFile.getName().equalsIgnoreCase("List.wz")) return;

                if (!wzFile.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzFile.getName(), wzFile.getStatus().getMessage());
                    throw new RuntimeException();
                }
                wzFile.exportFileToImg(folder.toPath());
            }
        });
    }

    private void addExportXmlBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            ExportXmlDialog dialog = new ExportXmlDialog(editPane, true);
            ExportXmlData data = dialog.getData();
            if (data == null) return;

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
                WzFile wzFile = wzDirectory.getWzFile();
                if (wzFile.getName().equalsIgnoreCase("List.wz")) return;

                if (!wzFile.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzFile.getName(), wzFile.getStatus().getMessage());
                    throw new RuntimeException();
                }
                wzFile.exportFileToXml(Path.of(data.getExportPath()), data.getIndent(), data.getMeType());
            }
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
                    byte[] iv = wzFile.getWzIv();
                    byte[] key = wzFile.getUserKey();
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
                    byte[] iv = wzFile.getWzIv();
                    byte[] key = wzFile.getUserKey();
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

    private void addChineseBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            Instant start = Instant.now();
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            DefaultMutableTreeNode rightTreeRoot = MainFrame.getInstance().getCenterPane().getAnotherPane(editPane).getTreeRoot();
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzDirectory wzDirectory = (WzDirectory) node.getUserObject();
                WzFile to = wzDirectory.getWzFile();
                if (to.getName().equalsIgnoreCase("List.wz")) return;
                if (!to.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", to.getName(), to.getStatus().getMessage());
                    throw new RuntimeException();
                }

                DefaultMutableTreeNode rightNode = MainFrame.getInstance().getCenterPane().getAnotherPane(editPane).findTreeNodeByName(rightTreeRoot, to.getName());
                if (rightNode == null) continue;
                WzFile from = ((WzDirectory) rightNode.getUserObject()).getWzFile();
                if (!from.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", from.getName(), from.getStatus().getMessage());
                    throw new RuntimeException();
                }

                ChineseUtil.chinese(from, to);
            }

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            MainFrame.getInstance().setStatusText("汉化完成! 耗时 %d ms", duration.toMillis());
        });
    }

    private void addOutlinkBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            Instant now = Instant.now();
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            List<WzObject> objects = new ArrayList<>();
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzObject wzObject = (WzObject) node.getUserObject();
                objects.add(wzObject);
            }

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    Outlink.replace(objects);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        Instant end = Instant.now();
                        MainFrame.getInstance().setStatusText("Outlink 结束，耗时 %d 秒", Duration.between(now, end).toSeconds());
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
            worker.execute();
        });
    }
}
