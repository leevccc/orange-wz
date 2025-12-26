package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.canvas.CanvasWall;
import orange.wz.gui.component.dialog.*;
import orange.wz.gui.component.form.data.*;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.CanvasUtil;
import orange.wz.gui.utils.CanvasUtilData;
import orange.wz.gui.utils.ChineseUtil;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.*;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.WzFileStatus;
import orange.wz.utils.wzkey.WzKey;

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
public final class WzImageFileMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;
    @Getter
    private final JMenuItem copyBtn;
    @Getter
    private final JMenuItem pasteBtn;

    public WzImageFileMenu(EditPane editPane, JTree tree) {
        super();
        this.editPane = editPane;
        this.tree = tree;

        JMenu addBtn = new JMenu("子节点");
        addBtn.setIcon(AiOutlinePlus);
        JMenuItem addCanvasBtn = new JMenuItem("图片");
        JMenuItem addConvexBtn = new JMenuItem("Convex");
        JMenuItem addDoubleBtn = new JMenuItem("Double");
        JMenuItem addFloatBtn = new JMenuItem("Float");
        JMenuItem addIntBtn = new JMenuItem("Int");
        JMenuItem addListBtn = new JMenuItem("列表");
        JMenuItem addLongBtn = new JMenuItem("Long");
        JMenuItem addNullBtn = new JMenuItem("Null");
        JMenuItem addShortBtn = new JMenuItem("Short");
        JMenuItem addSoundBtn = new JMenuItem("音频");
        JMenuItem addStringBtn = new JMenuItem("字符串");
        JMenuItem addUOLBtn = new JMenuItem("链接");
        JMenuItem addVectorBtn = new JMenuItem("向量");
        addBtn.add(addCanvasBtn);
        addBtn.add(addConvexBtn);
        addBtn.add(addDoubleBtn);
        addBtn.add(addFloatBtn);
        addBtn.add(addIntBtn);
        addBtn.add(addListBtn);
        addBtn.add(addLongBtn);
        addBtn.add(addNullBtn);
        addBtn.add(addShortBtn);
        addBtn.add(addSoundBtn);
        addBtn.add(addStringBtn);
        addBtn.add(addUOLBtn);
        addBtn.add(addVectorBtn);
        JMenuItem saveBtn = new JMenuItem("保存", AiOutlineSaveIcon);
        JMenuItem unloadBtn = new JMenuItem("卸载", AiOutlineCloseIcon);
        JMenuItem reloadBtn = new JMenuItem("重载", AiOutlineReloadIcon);
        JMenuItem moveBtn = new JMenuItem("切换视图", AiOutlineEye);
        copyBtn = new JMenuItem("复制", AiOutlineCopy);
        pasteBtn = new JMenuItem("粘贴", MdOutlineContentPaste);
        JMenuItem keyBtn = new JMenuItem("修改密钥", AiOutlineKey);
        JMenu exportBtn = new JMenu("导出");
        JMenuItem exportXmlBtn = new JMenuItem("Xml");
        exportBtn.add(exportXmlBtn);
        JMenuItem chineseBtn = new JMenuItem("汉化");
        JMenuItem imageBtn = new JMenuItem("图片嗅探");


        addCanvasBtnItem(addCanvasBtn);
        addConvexBtnItem(addConvexBtn);
        addDoubleBtnItem(addDoubleBtn);
        addFloatBtnItem(addFloatBtn);
        addIntBtnItem(addIntBtn);
        addListBtnItem(addListBtn);
        addLongBtnItem(addLongBtn);
        addNullBtnItem(addNullBtn);
        addShortBtnItem(addShortBtn);
        addSoundBtnItem(addSoundBtn);
        addStringBtnItem(addStringBtn);
        addUOLBtnItem(addUOLBtn);
        addVectorBtnItem(addVectorBtn);
        saveBtnAction(saveBtn);
        unloadBtnAction(unloadBtn);
        reloadBtnAction(reloadBtn);
        moveBtnAction(moveBtn);
        addCopyBtnAction(copyBtn);
        addPasteBtnAction(pasteBtn);
        addKeyBtnAction(keyBtn);
        addExportXmlBtnAction(exportXmlBtn);
        addChineseBtnAction(chineseBtn);
        addImageBtnAction(imageBtn);

        add(addBtn);
        add(saveBtn);
        add(unloadBtn);
        add(reloadBtn);
        add(moveBtn);
        add(copyBtn);
        add(pasteBtn);
        add(keyBtn);
        add(exportBtn);
        add(chineseBtn);
        add(imageBtn);
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
                if (wzImageFile.getStatus() != WzFileStatus.PARSE_SUCCESS) {
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
                    if (wzImageFile.getStatus() != WzFileStatus.PARSE_SUCCESS) {
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
                WzImageFile imageFile = (WzImageFile) node.getUserObject();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                if (pNode == null) continue;
                if (pNode.getUserObject() instanceof WzFolder wzFolder) {
                    wzFolder.remove(imageFile);
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
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                int index = pNode.getIndex(node);
                WzImageFile wzImageFileOld = (WzImageFile) node.getUserObject();
                Path filePath = Path.of(wzImageFileOld.getFilePath());
                String filename = filePath.getFileName().toString();

                editPane.removeNodeFromTree(node);

                WzImageFile wzImageFileNew = new WzImageFile(filename, filePath.toString(), key.getIv(), key.getUserKey());
                editPane.insertNodeToTree(pNode, wzImageFileNew, true, index);

                if (pNode.getUserObject() instanceof WzFolder wzFolder) {
                    wzFolder.remove(wzImageFileOld);
                    wzFolder.add(wzImageFileNew);
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
                WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
                targetPane.insertNodeToTree(targetPane.getTreeRoot(), wzImageFile, true);
                editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
            }
            editPane.resetValueForm();
        });
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
                WzImageFile wzObject = (WzImageFile) node.getUserObject();
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
            WzImage target = (WzImage) node.getUserObject();

            Clipboard clipboard = MainFrame.getInstance().getClipboard();
            clipboard.lock();

            if (clipboard.canPaste(target)) {
                List<WzObject> objects = clipboard.getItems();
                setPasteParent(objects, target);
                setPasteWzImage(objects, target);

                OverwriteChoice choice = null;
                for (WzObject obj : objects) {
                    obj.setTempChanged(true);
                    int index = 0;
                    if (obj instanceof WzImageProperty prop) {
                        if (target.existChild(prop.getName())) { // 发现重名
                            if (choice == OverwriteChoice.SKIP_ALL) continue;
                            else if (choice == OverwriteChoice.OVERWRITE_ALL) {
                                target.removeChild(prop.getName());
                                DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, prop.getName());
                                index = node.getIndex(childNode);
                                editPane.removeNodeFromTree(childNode);
                            } else {
                                choice = OverwriteDialog.show(editPane, prop.getName());
                                switch (choice) {
                                    case OVERWRITE, OVERWRITE_ALL -> {
                                        target.removeChild(prop.getName());
                                        DefaultMutableTreeNode childNode = editPane.findTreeNodeByName(node, prop.getName());
                                        index = node.getIndex(childNode);
                                        editPane.removeNodeFromTree(childNode);
                                    }
                                    case SKIP, SKIP_ALL, CANCEL -> {
                                        continue;
                                    }
                                }
                            }
                        }
                        target.addChild(prop);
                    }
                    editPane.insertNodeToTree(node, obj, true, index);
                }
            } else {
                JMessageUtil.error("Image 只能粘贴 Property");
            }

            editPane.resetValueForm();
            clipboard.clear();
            clipboard.unlock();
        });
    }

    private void addKeyBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            ChangeKeyDialog dialog = new ChangeKeyDialog(editPane, false);
            KeyData keyData = dialog.getData();

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
                wzImageFile.changeKey(keyData.getIv(), keyData.getKey());
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
                WzImage wzImage = (WzImage) node.getUserObject();

                if (!wzImage.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImage.getName(), wzImage.getStatus().getMessage());
                    throw new RuntimeException();
                }
                wzImage.exportToXml(Path.of(data.getExportPath()), data.getIndent(), data.isExportMedia());
            }
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

    private void setPasteWzImage(List<? extends WzObject> objects, WzImage wzImage) {
        if (objects == null) return;
        for (WzObject obj : objects) {
            if (obj instanceof WzImageProperty property) {
                property.setWzImage(wzImage);
                setPasteWzImage(property.getChildren(), wzImage);
            }
        }
    }

    private void addCanvasBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            CanvasDialog nodeDialog = new CanvasDialog("新增 Canvas", editPane);
            CanvasFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzCanvasProperty prop = new WzCanvasProperty(name, wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }
            prop.initPngProperty(name, prop, wzImageFile);
            prop.setPng(data.getValue(), data.getFormat());

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addConvexBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            NodeDialog nodeDialog = new NodeDialog("新增 Convex", editPane);
            NodeFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzConvexProperty prop = new WzConvexProperty(name, wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addDoubleBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            DoubleDialog nodeDialog = new DoubleDialog("新增 Double", editPane);
            DoubleFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzDoubleProperty prop = new WzDoubleProperty(name, data.getValue(), wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addFloatBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            FloatDialog nodeDialog = new FloatDialog("新增 Float", editPane);
            FloatFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzFloatProperty prop = new WzFloatProperty(name, data.getValue(), wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addIntBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            IntDialog nodeDialog = new IntDialog("新增 Int", editPane);
            IntFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzIntProperty prop = new WzIntProperty(name, data.getValue(), wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addListBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            NodeDialog nodeDialog = new NodeDialog("新增 List", editPane);
            NodeFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzListProperty prop = new WzListProperty(name, wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addLongBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            LongDialog nodeDialog = new LongDialog("新增 Long", editPane);
            LongFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzLongProperty prop = new WzLongProperty(name, data.getValue(), wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addNullBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            NodeDialog nodeDialog = new NodeDialog("新增 Null", editPane);
            NodeFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzNullProperty prop = new WzNullProperty(name, wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addShortBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            ShortDialog nodeDialog = new ShortDialog("新增 Short", editPane);
            ShortFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzShortProperty prop = new WzShortProperty(name, data.getValue(), wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addSoundBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            SoundDialog nodeDialog = new SoundDialog("新增 Sound", editPane);
            SoundFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzSoundProperty prop = new WzSoundProperty(name, wzImageFile, wzImageFile);
            prop.setSound(data.getSoundBytes());
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addStringBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            StringDialog nodeDialog = new StringDialog("新增 String", editPane);
            StringFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzStringProperty prop = new WzStringProperty(name, data.getValue(), wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addUOLBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            StringDialog nodeDialog = new StringDialog("新增 UOL", editPane);
            StringFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzUOLProperty prop = new WzUOLProperty(name, data.getValue(), wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addVectorBtnItem(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("不要多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();

            VectorDialog nodeDialog = new VectorDialog("新增 Vector", editPane);
            VectorFormData data = nodeDialog.getData();

            if (data == null) return;

            String name = data.getName();

            if (name.isEmpty()) {
                JMessageUtil.error("名称不能为空");
                return;
            }

            WzImageFile wzImageFile = (WzImageFile) node.getUserObject();
            if (!wzImageFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImageFile.getName(), wzImageFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzVectorProperty prop = new WzVectorProperty(name, data.getX(), data.getY(), wzImageFile, wzImageFile);
            if (!wzImageFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }

    private void addChineseBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            Instant start = Instant.now();
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzImage to = (WzImage) node.getUserObject();
                if (!to.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", to.getName(), to.getStatus().getMessage());
                    throw new RuntimeException();
                }

                WzImage from = (WzImage) MainFrame.getInstance().getCenterPane().getAnotherPane(editPane).findTreeWzObjectByPath(to.getPath());
                if (from == null) {
                    log.error("找不到中文版本的 {}", to.getName());
                    continue;
                }
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

    private void addImageBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("该功能不支持多选");
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
            WzImage wzImage = (WzImage) node.getUserObject();
            if (!wzImage.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImage.getName(), wzImage.getStatus().getMessage());
                throw new RuntimeException();
            }
            List<CanvasUtilData> data = new ArrayList<>();
            CanvasUtil.search(data, wzImage.getChildren());

            CanvasWall canvasWall = new CanvasWall(data, wzImage.getPath(), node, editPane);
            canvasWall.setVisible(true);
        });
    }
}
