package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.dialog.NodeDialog;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzFile;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import static orange.wz.gui.Icons.AiOutlineDelete;
import static orange.wz.gui.Icons.AiOutlinePlus;

@Slf4j
public final class WzDirectoryMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;
    @Getter
    private final JMenuItem deleteBtn;

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

        deleteBtn = new JMenuItem("删除节点", AiOutlineDelete);

        addDirBtnAction(addDirBtn);
        addImgBtnAction(addImgBtn);
        deleteBtnAction(deleteBtn);

        add(addBtn);
        add(deleteBtn);
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
