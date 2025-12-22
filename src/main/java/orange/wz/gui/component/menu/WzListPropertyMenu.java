package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.dialog.*;
import orange.wz.gui.component.form.data.*;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import java.util.List;

import static orange.wz.gui.Icons.*;

@Slf4j
public final class WzListPropertyMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;
    @Getter
    private final JMenuItem deleteBtn;
    @Getter
    private final JMenuItem copyBtn;
    @Getter
    private final JMenuItem pasteBtn;

    public WzListPropertyMenu(EditPane editPane, JTree tree) {
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

        copyBtn = new JMenuItem("复制", AiOutlineCopy);
        pasteBtn = new JMenuItem("粘贴", MdOutlineContentPaste);
        deleteBtn = new JMenuItem("删除节点", AiOutlineDelete);

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
        addCopyBtnAction(copyBtn);
        addPasteBtnAction(pasteBtn);
        deleteBtnAction(deleteBtn);

        add(addBtn);
        add(copyBtn);
        add(pasteBtn);
        add(deleteBtn);
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
                WzImageProperty wzObject = (WzImageProperty) node.getUserObject();
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
            WzImageProperty target = (WzImageProperty) node.getUserObject();

            Clipboard clipboard = MainFrame.getInstance().getClipboard();
            clipboard.lock();

            if (clipboard.canPaste(target)) {
                List<WzObject> objects = clipboard.getItems();
                setPasteParent(objects, target);
                setPasteWzImage(objects, target.getWzImage());

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
                JMessageUtil.error("Property 只能粘贴 Property");
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

    private void setPasteWzImage(List<? extends WzObject> objects, WzImage wzImage) {
        if (objects == null) return;
        for (WzObject obj : objects) {
            if (obj instanceof WzImageProperty property) {
                property.setWzImage(wzImage);
                setPasteWzImage(property.getChildren(), wzImage);
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

                if (pWzObject instanceof WzImage image && image.removeChild(wzObject.getName())) {
                    editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
                } else if (pWzObject instanceof WzImageProperty property && property.removeChild(wzObject.getName())) {
                    editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
                } else {
                    log.error("无法删除节点, 父节点类型: {}", pWzObject.getClass().getName());
                }
            }
        });
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzCanvasProperty prop = new WzCanvasProperty(name, imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }
            prop.initPngProperty(name, prop, imageProperty.getWzImage());
            prop.setPng(data.getValue(), data.getFormat());

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzConvexProperty prop = new WzConvexProperty(name, imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzDoubleProperty prop = new WzDoubleProperty(name, data.getValue(), imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzFloatProperty prop = new WzFloatProperty(name, data.getValue(), imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzIntProperty prop = new WzIntProperty(name, data.getValue(), imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzListProperty prop = new WzListProperty(name, imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzLongProperty prop = new WzLongProperty(name, data.getValue(), imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzNullProperty prop = new WzNullProperty(name, imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzShortProperty prop = new WzShortProperty(name, data.getValue(), imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzSoundProperty prop = new WzSoundProperty(name, imageProperty, imageProperty.getWzImage());
            prop.setSound(data.getSoundBytes());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzStringProperty prop = new WzStringProperty(name, data.getValue(), imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzUOLProperty prop = new WzUOLProperty(name, data.getValue(), imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
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

            WzImageProperty imageProperty = (WzImageProperty) node.getUserObject();

            WzVectorProperty prop = new WzVectorProperty(name, data.getX(), data.getY(), imageProperty, imageProperty.getWzImage());
            if (!imageProperty.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            editPane.insertNodeToTree(node, prop, true, 0);
        });
    }
}
