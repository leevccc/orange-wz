package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.canvas.CanvasWall;
import orange.wz.gui.component.dialog.*;
import orange.wz.gui.component.form.data.*;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.CanvasUtil;
import orange.wz.gui.utils.CanvasUtilData;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.WzFolder;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzXmlFile;
import orange.wz.provider.properties.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static orange.wz.gui.Icons.*;

@Slf4j
public final class WzXmlFileMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;
    @Getter
    private final JMenuItem copyBtn;
    @Getter
    private final JMenuItem pasteBtn;

    public WzXmlFileMenu(EditPane editPane, JTree tree) {
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
        JMenuItem saveAsBtn = new JMenuItem("另存为", AiOutlineSaveIcon);
        JMenuItem unloadBtn = new JMenuItem("卸载", AiOutlineCloseIcon);
        JMenuItem reloadBtn = new JMenuItem("重载", AiOutlineReloadIcon);
        JMenuItem moveBtn = new JMenuItem("转移视图", AiOutlineEye);
        copyBtn = new JMenuItem("复制", AiOutlineCopy);
        pasteBtn = new JMenuItem("粘贴", MdOutlineContentPaste);
        JMenu exportBtn = new JMenu("导出");
        JMenuItem exportImgBtn = new JMenuItem("Img");
        exportBtn.add(exportImgBtn);
        JMenuItem compareImgBtn = new JMenuItem("图片对比");
        JMenuItem imageBtn = new JMenuItem("图片嗅探");
        JMenuItem sicBtn = new JMenuItem("排序并改名");
        JMenuItem delChild = new JMenuItem("批量删除");
        JMenuItem changeCavFmt = new JMenuItem("图片格式");
        JMenuItem scaleImage = new JMenuItem("图片缩放");
        JMenuItem changeNodeName = new JMenuItem("修改节点名");
        JMenuItem changeIntNodeValue = new JMenuItem("修改int值");
        JMenuItem rawToIcon = new JMenuItem("RawToIcon");


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
        saveAsBtnAction(saveAsBtn);
        unloadBtn.addActionListener(e -> editPane.unload());
        reloadBtnAction(reloadBtn);
        moveBtnAction(moveBtn);
        copyBtn.addActionListener(e -> editPane.doCopy());
        pasteBtn.addActionListener(e -> editPane.doPaste());
        addExportImgBtnAction(exportImgBtn);
        compareImgBtn.addActionListener(e -> editPane.compareImg());
        addImageBtnAction(imageBtn);
        sicBtn.addActionListener(e -> editPane.sortAndReindexChildren());
        delChild.addActionListener(e -> editPane.removeAllWzChildWithName());
        changeCavFmt.addActionListener(e -> editPane.changeCavFmt());
        scaleImage.addActionListener(e -> editPane.scaleImage());
        changeNodeName.addActionListener(e -> editPane.changeNodeName());
        changeIntNodeValue.addActionListener(e -> editPane.changeIntNodeValue());
        rawToIcon.addActionListener(e -> editPane.rawToIcon());

        add(addBtn);
        add(saveBtn);
        add(saveAsBtn);
        add(unloadBtn);
        add(reloadBtn);
        add(moveBtn);
        add(copyBtn);
        add(pasteBtn);
        add(exportBtn);
        add(compareImgBtn);
        add(imageBtn);
        add(sicBtn);
        add(delChild);
        add(changeCavFmt);
        add(scaleImage);
        add(changeNodeName);
        add(changeIntNodeValue);
        add(rawToIcon);
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
                WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
                targetPane.insertNodeToTree(targetPane.getTreeRoot(), wzXmlFile, true);
                editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
            }
            editPane.resetValueForm();
        });
    }

    private void addExportImgBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            editPane.exportImg(selectedPaths);
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzCanvasProperty prop = new WzCanvasProperty(name, wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }
            prop.initPngProperty(name, prop, wzXmlFile);
            prop.setPng(data.getValue(), data.getFormat(), data.getScale());

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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzConvexProperty prop = new WzConvexProperty(name, wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzDoubleProperty prop = new WzDoubleProperty(name, data.getValue(), wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzFloatProperty prop = new WzFloatProperty(name, data.getValue(), wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzIntProperty prop = new WzIntProperty(name, data.getValue(), wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzListProperty prop = new WzListProperty(name, wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzLongProperty prop = new WzLongProperty(name, data.getValue(), wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzNullProperty prop = new WzNullProperty(name, wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzShortProperty prop = new WzShortProperty(name, data.getValue(), wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzSoundProperty prop = new WzSoundProperty(name, wzXmlFile, wzXmlFile);
            prop.setSound(data.getSoundBytes());
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzStringProperty prop = new WzStringProperty(name, data.getValue(), wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzUOLProperty prop = new WzUOLProperty(name, data.getValue(), wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
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

            WzXmlFile wzXmlFile = (WzXmlFile) node.getUserObject();
            if (!wzXmlFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzXmlFile.getName(), wzXmlFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            WzVectorProperty prop = new WzVectorProperty(name, data.getX(), data.getY(), wzXmlFile, wzXmlFile);
            if (!wzXmlFile.addChild(prop)) {
                JMessageUtil.error("名称已存在");
                return;
            }

            if (node.isLeaf()) return; // isLeaf 说明未加载数据，就不要插入了
            prop.setTempChanged(true);
            editPane.insertNodeToTree(node, prop, true, 0);
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
