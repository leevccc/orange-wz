package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.dialog.NodeDialog;
import orange.wz.gui.component.dialog.OverwriteChoice;
import orange.wz.gui.component.dialog.OverwriteDialog;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.ChineseUtil;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.gui.utils.Outlink;
import orange.wz.gui.utils.TreePathUtil;
import orange.wz.provider.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        JMenuItem saveAsBtn = new JMenuItem("另存为", AiOutlineSaveIcon);
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
        saveAsBtnAction(saveAsBtn);
        unloadBtnAction(unloadBtn);
        reloadBtnAction(reloadBtn);
        moveBtnAction(moveBtn);
        pasteBtn.addActionListener(e -> editPane.doPaste());
        addKeyBtnAction(keyBtn);
        addExportImgBtnAction(exportImgBtn);
        addExportXmlBtnAction(exportXmlBtn);
        addImportImgBtnAction(importImgBtn);
        addImportXmlBtnAction(importXmlBtn);
        addChineseBtnAction(chineseBtn);
        addOutlinkBtnAction(outlinkBtn);

        add(addBtn);
        add(saveBtn);
        add(saveAsBtn);
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

            editPane.saveFiles(selectedPaths);
        });
    }

    private void saveAsBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            if (selectedPaths.length != 1) {
                JMessageUtil.error("该功能不允许多选。");
                return;
            }

            editPane.saveAs((DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent());
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

            editPane.reloadFile(selectedPaths);
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

            editPane.changeKey(selectedPaths);
        });
    }

    private void addExportImgBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            editPane.exportImg(selectedPaths);
        });
    }

    private void addExportXmlBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            editPane.exportXml(selectedPaths);
        });
    }

    private void addImportImgBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (TreePathUtil.isNullOrMultiple(selectedPaths)) return;

            editPane.importImg((DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent());
        });
    }

    private void addImportXmlBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if(TreePathUtil.isNullOrMultiple(selectedPaths)) return;

            editPane.importXml((DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent());
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
