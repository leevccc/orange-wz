package orange.wz.gui.component.panel;

import lombok.Getter;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.impl.*;
import orange.wz.gui.component.menu.WzFileMenu;
import orange.wz.gui.component.menu.WzFolderMenu;
import orange.wz.gui.component.menu.WzImageFileMenu;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.*;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.WzType;
import orange.wz.utils.wzkey.WzKey;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Comparator;
import java.util.List;

import static orange.wz.gui.Icons.*;

@Getter
public final class EditPane extends JSplitPane {
    private JTree tree;
    private DefaultMutableTreeNode treeRoot;

    private JPanel formCards;
    private NodeForm nodeForm;
    private CanvasForm canvasForm;
    private DoubleForm doubleForm;
    private FloatForm floatForm;
    private IntForm intForm;
    private LongForm longForm;
    private ShortForm shortForm;
    private SoundForm soundForm;
    private StringForm stringForm;
    private UolCanvasForm uolCanvasForm;
    private UolSoundForm uolSoundForm;
    private VectorForm vectorForm;

    public EditPane(boolean oneTouchExpandable) {
        super();

        JScrollPane treePanel = createTreePane();
        treePanel.setMinimumSize(new Dimension(260, 0)); // 宽度最小 260，高度不限

        setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        setLeftComponent(treePanel);
        setRightComponent(createValuePane());
        setDividerLocation(260);
        setOneTouchExpandable(oneTouchExpandable); // 增加小箭头快速展开/收起
    }

    private JScrollPane createTreePane() {
        treeRoot = new DefaultMutableTreeNode("root");
        tree = new JTree(treeRoot);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true); // 隐藏自带的展开/收缩图标
        tree.setToggleClickCount(0);

        // 节点自定义渲染器
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof WzObject obj) {
                    Icon icon = switch (obj.getType()) {
                        case FOLDER -> FcFolderIcon;
                        case DIRECTORY -> {
                            if (((WzDirectory) obj).isWzFile()) {
                                yield AiOutlineFileWordIcon;
                            } else {
                                yield FcFolderBlueIcon;
                            }
                        }
                        case IMAGE -> {
                            if (obj instanceof WzImageFile) {
                                yield AiOutlineFileMarkdownIcon;
                            } else {
                                yield ImgIcon;
                            }
                        }
                        case CANVAS_PROPERTY -> PngIcon;
                        case CONVEX_PROPERTY -> ConvexIcon;
                        case DOUBLE_PROPERTY -> DoubleIcon;
                        case FLOAT_PROPERTY -> FloatIcon;
                        case INT_PROPERTY -> IntIcon;
                        case LIST_PROPERTY -> ListIcon;
                        case LONG_PROPERTY -> LongIcon;
                        case NULL_PROPERTY -> NullIcon;
                        case RAW_DATA_PROPERTY -> RawIcon;
                        case SHORT_PROPERTY -> ShortIcon;
                        case SOUND_PROPERTY -> WavIcon;
                        case STRING_PROPERTY -> StrIcon;
                        case UOL_PROPERTY -> UolIcon;
                        case VECTOR_PROPERTY -> VectorIcon;
                        case WZ_FILE, PNG_PROPERTY -> null;
                    };
                    setIcon(icon);
                    setText(obj.getName());
                    if (obj.isTempChanged()) {
                        setForeground(Color.RED);
                    }
                }

                return this;
            }
        };
        tree.setCellRenderer(renderer);

        // 禁止跨区域多选
        SameLevelTreeSelectionModel selectionModel = new SameLevelTreeSelectionModel();
        selectionModel.onReject(() -> JMessageUtil.warn("操作提示", "不允许跨区域多选"));
        tree.setSelectionModel(selectionModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // 选中节点触发事件
        tree.addTreeSelectionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                handleTreeClick((WzObject) node.getUserObject());
            }
        });

        // 右键菜单
        JPopupMenu wzFilePopupMenu = new WzFileMenu(this, tree);
        JPopupMenu wzFolderPopupMenu = new WzFolderMenu(this, tree);
        JPopupMenu wzImageFilePopupMenu = new WzImageFileMenu(this, tree);
        tree.addMouseListener(new MouseAdapter() {
            private void showPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                TreePath path = getTreePath(e);
                if (path == null) return;

                // 右键目标不是选中对象，则单独选中目标并右键
                if (!tree.isPathSelected(path)) {
                    tree.setSelectionPath(path);
                }

                // 显示菜单
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                WzObject wzObject = (WzObject) node.getUserObject();

                if (wzObject instanceof WzDirectory directory && directory.isWzFile()) {
                    wzFilePopupMenu.show(tree, e.getX(), e.getY());
                } else if (wzObject instanceof WzFolder) {
                    wzFolderPopupMenu.show(tree, e.getX(), e.getY());
                } else if (wzObject instanceof WzImageFile) {
                    wzImageFilePopupMenu.show(tree, e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopup(e);
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    TreePath path = getTreePath(e);
                    if (path == null) return;

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                    // 左键单击
                    // 移动到选中事件中去了
                    // if (e.getClickCount() == 1) {
                    //     handleTreeClick((WzObject) node.getUserObject());
                    // }

                    // 左键双击
                    if (e.getClickCount() == 2) {
                        if (node.isLeaf()) {
                            // 叶子节点：执行业务逻辑
                            handleTreeDoubleClick(node, (WzObject) node.getUserObject());
                        } else {
                            // 非叶子节点：手动切换展开状态
                            if (tree.isExpanded(path)) {
                                tree.collapsePath(path);
                            } else {
                                tree.expandPath(path);
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopup(e);
                }
            }
        });

        return new JScrollPane(tree);
    }

    private JPanel createValuePane() {
        // CardLayout 容器
        formCards = new JPanel(new CardLayout());
        // 这两行是为了防止拉伸右侧边框后导致SplitPanel左侧的视图无法再放大
        formCards.setMinimumSize(new Dimension(0, 0));
        formCards.setPreferredSize(new Dimension(0, 0));

        // 初始化表单
        nodeForm = new NodeForm();
        canvasForm = new CanvasForm();
        doubleForm = new DoubleForm();
        floatForm = new FloatForm();
        intForm = new IntForm();
        longForm = new LongForm();
        shortForm = new ShortForm();
        soundForm = new SoundForm();
        stringForm = new StringForm();
        uolCanvasForm = new UolCanvasForm();
        uolSoundForm = new UolSoundForm();
        vectorForm = new VectorForm();

        // 包一层 BoxLayout，让控件贴顶部
        formCards.add(nodeForm.getValuePane(), "node");
        formCards.add(canvasForm.getValuePane(), "canvas");
        formCards.add(doubleForm.getValuePane(), "double");
        formCards.add(floatForm.getValuePane(), "float");
        formCards.add(intForm.getValuePane(), "int");
        formCards.add(longForm.getValuePane(), "long");
        formCards.add(shortForm.getValuePane(), "short");
        formCards.add(soundForm.getValuePane(), "sound");
        formCards.add(stringForm.getValuePane(), "string");
        formCards.add(uolCanvasForm.getValuePane(), "uolCanvas");
        formCards.add(uolSoundForm.getValuePane(), "uolSound");
        formCards.add(vectorForm.getValuePane(), "vector");

        // 默认显示 node 表单
        ((CardLayout) formCards.getLayout()).show(formCards, "node");

        return formCards;
    }

    private void switchForm(String formName) {
        CardLayout cl = (CardLayout) (formCards.getLayout());
        cl.show(formCards, formName);
    }

    private void handleTreeClick(WzObject wzObject) {
        switch (wzObject) {
            case WzFolder obj -> {
                nodeForm.setData(obj.getName(), WzType.FOLDER.name(), wzObject, this);
                switchForm("node");
            }
            case WzDirectory obj -> {
                if (obj.isWzFile()) {
                    nodeForm.setData(obj.getName(), WzType.WZ_FILE.name(), wzObject, this);
                } else {
                    nodeForm.setData(obj.getName(), WzType.DIRECTORY.name(), wzObject, this);
                }
                switchForm("node");
            }
            case WzImage obj -> {
                nodeForm.setData(obj.getName(), WzType.IMAGE.name(), wzObject, this);
                switchForm("node");
            }
            case WzCanvasProperty obj -> {
                canvasForm.setData(obj.getName(), WzType.CANVAS_PROPERTY.name(), obj.getPngImage(), obj.getWidth(), obj.getHeight(), obj.getPngFormat(), wzObject, this);
                switchForm("canvas");
            }
            case WzConvexProperty obj -> {
                nodeForm.setData(obj.getName(), WzType.CONVEX_PROPERTY.name(), wzObject, this);
                switchForm("node");
            }
            case WzDoubleProperty obj -> {
                doubleForm.setData(obj.getName(), WzType.DOUBLE_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("double");
            }
            case WzFloatProperty obj -> {
                floatForm.setData(obj.getName(), WzType.FLOAT_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("float");
            }
            case WzIntProperty obj -> {
                intForm.setData(obj.getName(), WzType.INT_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("int");
            }
            case WzListProperty obj -> {
                nodeForm.setData(obj.getName(), WzType.LIST_PROPERTY.name(), wzObject, this);
                switchForm("node");
            }
            case WzLongProperty obj -> {
                longForm.setData(obj.getName(), WzType.LONG_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("long");
            }
            case WzNullProperty obj -> {
                nodeForm.setData(obj.getName(), WzType.NULL_PROPERTY.name(), wzObject, this);
                switchForm("node");
            }
            case WzShortProperty obj -> {
                shortForm.setData(obj.getName(), WzType.SHORT_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("short");
            }
            case WzSoundProperty obj -> {
                soundForm.setData(obj.getName(), WzType.SOUND_PROPERTY.name(), obj.getFileBytes(), wzObject, this);
                switchForm("sound");
            }
            case WzStringProperty obj -> {
                stringForm.setData(obj.getName(), WzType.STRING_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("string");
            }
            case WzUOLProperty obj -> {
                WzObject target = obj.getUolTarget();
                if (target instanceof WzCanvasProperty cav) {
                    uolCanvasForm.setData(obj.getName(), WzType.UOL_PROPERTY.name(), obj.getValue(), cav, wzObject, this);
                    switchForm("uolCanvas");
                } else if (target instanceof WzSoundProperty sound) {
                    uolSoundForm.setData(obj.getName(), WzType.UOL_PROPERTY.name(), obj.getValue(), sound, wzObject, this);
                    switchForm("uolSound");
                }
            }
            case WzVectorProperty obj -> {
                vectorForm.setData(obj.getName(), WzType.VECTOR_PROPERTY.name(), obj.getX(), obj.getY(), wzObject, this);
                switchForm("vector");
            }
            default -> {
                MainFrame.getInstance().setStatusText("%s 未知的节点类型 %s", wzObject.getName(), wzObject.getClass().getSimpleName());
                return;
            }
        }

        MainFrame.getInstance().setStatusText(wzObject.getPath());
    }

    private void handleTreeDoubleClick(DefaultMutableTreeNode node, WzObject wzObject) {
        MainFrame.getInstance().setStatusText("加载 %s...", wzObject.getName());

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                switch (wzObject) {
                    case WzFolder folder -> {
                        if (node.getChildCount() == 0) {
                            java.util.List<WzObject> children = folder.getChildren();
                            sortWzObjects(children);
                            children.forEach(child -> insertNodeToTree(node, child, true));
                        }
                    }
                    case WzDirectory wzDir -> {
                        if (wzDir.isWzFile()) {
                            wzDir.getWzFile().load();
                        }
                        addChildrenRecursively(node, wzDir, true);
                    }
                    case WzImage wzImg -> {
                        if (node.getChildCount() == 0) {
                            wzImg.parse();
                            java.util.List<WzImageProperty> children = wzImg.getChildren();
                            sortWzObjects(children);
                            children.forEach(child -> {
                                DefaultMutableTreeNode childNode = insertNodeToTree(node, child, true);
                                addChildrenRecursively(childNode, child, false);
                            });
                        }
                    }
                    case WzImageProperty prop -> addChildrenRecursively(node, prop, true);
                    default -> {
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    MainFrame.getInstance().setStatusText("%s 加载完毕", wzObject.getName());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }.execute();
    }

    // 递归方法：只插入子节点，不展开
    private void addChildrenRecursively(DefaultMutableTreeNode parentNode, WzObject wzObject, boolean expand) {
        if (parentNode.getChildCount() > 0) return;

        java.util.List<? extends WzObject> children = null;
        if (wzObject instanceof WzDirectory wzDir) {
            children = wzDir.getChildren();
        } else if (wzObject instanceof WzImageProperty prop) {
            children = prop.getChildren();
        }

        if (children == null || children.isEmpty()) return;

        sortWzObjects(children);

        for (WzObject child : children) {
            DefaultMutableTreeNode childNode = insertNodeToTree(parentNode, child, expand);
            addChildrenRecursively(childNode, child, false);
        }
    }

    private static void sortWzObjects(java.util.List<? extends WzObject> objects) {
        java.util.List<WzType> typePriority = java.util.List.of(
                WzType.FOLDER,
                WzType.WZ_FILE,
                WzType.DIRECTORY
        );

        objects.sort(Comparator
                .comparing((WzObject node) -> {
                    int index = typePriority.indexOf(node.getType());
                    return index == -1 ? Integer.MAX_VALUE : index; // 未定义的type排最后
                })
                .thenComparing(WzObject::getName, (a, b) -> {
                    // 内联的自然排序比较器
                    if (a == null && b == null) return 0;
                    if (a == null) return -1;
                    if (b == null) return 1;

                    int aIndex = 0, bIndex = 0;
                    int aLength = a.length();
                    int bLength = b.length();

                    while (aIndex < aLength && bIndex < bLength) {
                        char aChar = a.charAt(aIndex);
                        char bChar = b.charAt(bIndex);

                        if (Character.isDigit(aChar) && Character.isDigit(bChar)) {
                            int aNumber = 0;
                            while (aIndex < aLength && Character.isDigit(a.charAt(aIndex))) {
                                aNumber = aNumber * 10 + (a.charAt(aIndex) - '0');
                                aIndex++;
                            }

                            int bNumber = 0;
                            while (bIndex < bLength && Character.isDigit(b.charAt(bIndex))) {
                                bNumber = bNumber * 10 + (b.charAt(bIndex) - '0');
                                bIndex++;
                            }

                            if (aNumber != bNumber) {
                                return Integer.compare(aNumber, bNumber);
                            }
                        } else {
                            int compare = Character.compare(Character.toLowerCase(aChar), Character.toLowerCase(bChar));
                            if (compare != 0) {
                                return compare;
                            }
                            aIndex++;
                            bIndex++;
                        }
                    }

                    return aLength - bLength;
                }));
    }

    /**
     * 支持空白处选中节点
     *
     * @param e MouseEvent
     * @return TreePath
     */
    private TreePath getTreePath(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        TreePath path = tree.getClosestPathForLocation(x, y);
        if (path == null) return null;

        // 判断 y 是否真的落在该行高度范围内
        Rectangle bounds = tree.getPathBounds(path);
        if (bounds == null || y < bounds.y || y > bounds.y + bounds.height) {
            return null;
        }

        return path;
    }

    public void open(List<File> files) {
        WzKey key = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();

        files.forEach(f -> {
            if (f.isFile()) {
                if (f.getName().endsWith(".wz")) {
                    WzFile wzFile = new WzFile(f.getAbsolutePath(), (short) -1, key.getIv(), key.getUserKey());
                    insertNodeToTree(treeRoot, wzFile.getWzDirectory(), true);
                } else if (f.getName().endsWith(".img")) {
                    WzImageFile wzImageFile = new WzImageFile(f.getName(), f.getAbsolutePath(), key.getIv(), key.getUserKey());
                    insertNodeToTree(treeRoot, wzImageFile, true);
                }
            } else if (f.isDirectory()) {
                WzFolder folder = new WzFolder(f.getAbsolutePath(), key.getIv(), key.getUserKey());
                insertNodeToTree(treeRoot, folder, true);
            }
        });
    }

    public DefaultMutableTreeNode insertNodeToTree(DefaultMutableTreeNode parentNode, WzObject object, boolean expand) {
        return insertNodeToTree(parentNode, object, expand, -1);
    }

    public DefaultMutableTreeNode insertNodeToTree(DefaultMutableTreeNode parentNode, WzObject object, boolean expand, int index) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(object);
        model.insertNodeInto(newNode, parentNode, index == -1 ? parentNode.getChildCount() : index);

        if (expand) {
            tree.expandPath(new TreePath(parentNode.getPath()));
        }

        return newNode;
    }

    public void removeNodeFromTree(DefaultMutableTreeNode node) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

        if (node == null) return;
        if (node.getParent() == null) return;

        model.removeNodeFromParent(node);
    }
}
