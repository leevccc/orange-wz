package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.canvas.CanvasWall;
import orange.wz.gui.component.dialog.NodeDialog;
import orange.wz.gui.component.dialog.OverwriteChoice;
import orange.wz.gui.component.dialog.OverwriteDialog;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.CanvasUtil;
import orange.wz.gui.utils.CanvasUtilData;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.gui.utils.TreePathUtil;
import orange.wz.provider.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
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
        copyBtn.addActionListener(e -> editPane.doCopy());
        pasteBtn.addActionListener(e -> editPane.doPaste());
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

    private void deleteBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzObject wzObject = (WzObject) node.getUserObject();
                WzObject pWzObject = wzObject.getParent();

                if (pWzObject instanceof WzDirectory directory) {
                    boolean success = false;
                    if (wzObject instanceof WzImage wzImg) {
                        success = directory.removeImageChild(wzImg.getName());
                    } else if (wzObject instanceof WzDirectory wzDir) {
                        success = directory.removeDirectoryChild(wzDir.getName());
                    }

                    if (!success) {
                        log.warn("节点删除失败, 父节点: {} 子节点: {}", pWzObject.getName(), directory.getName());
                        continue;
                    }
                    editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
                } else {
                    log.error("无法删除节点, 父节点类型: {}", pWzObject.getClass().getSimpleName());
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
            if (TreePathUtil.isNullOrMultiple(selectedPaths)) return;

            editPane.importImg((DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent());
        });
    }

    private void addImportXmlBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (TreePathUtil.isNullOrMultiple(selectedPaths)) return;

            editPane.importXml((DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent());
        });
    }
}
