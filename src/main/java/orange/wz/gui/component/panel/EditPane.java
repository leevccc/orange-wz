package orange.wz.gui.component.panel;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.dialog.ExportXmlDialog;
import orange.wz.gui.component.dialog.ListEditor;
import orange.wz.gui.component.dialog.SearchDialog;
import orange.wz.gui.component.dialog.SearchResultDialog;
import orange.wz.gui.component.form.data.ExportXmlData;
import orange.wz.gui.component.form.data.SearchFormData;
import orange.wz.gui.component.form.data.SearchResult;
import orange.wz.gui.component.form.impl.*;
import orange.wz.gui.component.menu.*;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.gui.utils.SearchUtil;
import orange.wz.model.Pair;
import orange.wz.provider.*;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.FileTool;
import orange.wz.provider.tools.WzFileStatus;
import orange.wz.provider.tools.WzType;
import orange.wz.provider.tools.wzkey.WzKey;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static orange.wz.gui.Icons.*;

@Getter
@Slf4j
public final class EditPane extends JSplitPane {
    private JTree tree;
    private DefaultMutableTreeNode treeRoot;

    private JPanel formCards;

    private WzFileMenu wzFilePopupMenu;
    private WzFolderMenu wzFolderPopupMenu;
    private WzImageFileMenu wzImageFilePopupMenu;
    private WzXmlFileMenu wzXmlFilePopupMenu;
    private WzDirectoryMenu wzDirectoryPopupMenu;
    private WzImageMenu wzImagePopupMenu;
    private WzListPropertyMenu wzListPropertyPopupMenu;
    private WzValuePropertyMenu wzValuePropertyPopupMenu;

    private String currentFormName;
    private final Map<String, AbstractValueForm> nodeForms = Map.ofEntries(
            Map.entry("node", new NodeForm()),
            Map.entry("canvas", new CanvasForm()),
            Map.entry("double", new DoubleForm()),
            Map.entry("float", new FloatForm()),
            Map.entry("int", new IntForm()),
            Map.entry("long", new LongForm()),
            Map.entry("short", new ShortForm()),
            Map.entry("sound", new SoundForm()),
            Map.entry("string", new StringForm()),
            Map.entry("uolCanvas", new UolCanvasForm()),
            Map.entry("uolSound", new UolSoundForm()),
            Map.entry("vector", new VectorForm())
    );

    private final SearchDialog searchDialog = new SearchDialog("搜索", this);
    private final List<SearchResult> searchResults = new ArrayList<>();

    public NodeForm getNodeForm() {
        return (NodeForm) nodeForms.get("node");
    }

    public CanvasForm getCanvasForm() {
        return (CanvasForm) nodeForms.get("canvas");
    }

    public DoubleForm getDoubleForm() {
        return (DoubleForm) nodeForms.get("double");
    }

    public FloatForm getFloatForm() {
        return (FloatForm) nodeForms.get("float");
    }

    public IntForm getIntForm() {
        return (IntForm) nodeForms.get("int");
    }

    public LongForm getLongForm() {
        return (LongForm) nodeForms.get("long");
    }

    public ShortForm getShortForm() {
        return (ShortForm) nodeForms.get("short");
    }

    public SoundForm getSoundForm() {
        return (SoundForm) nodeForms.get("sound");
    }

    public StringForm getStringForm() {
        return (StringForm) nodeForms.get("string");
    }

    public UolCanvasForm getUolCanvasForm() {
        return (UolCanvasForm) nodeForms.get("uolCanvas");
    }

    public UolSoundForm getUolSoundForm() {
        return (UolSoundForm) nodeForms.get("uolSound");
    }

    public VectorForm getVectorForm() {
        return (VectorForm) nodeForms.get("vector");
    }

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
                            } else if (obj instanceof WzXmlFile) {
                                yield AiOutlineFileExcelIcon;
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
                        setForeground(Color.MAGENTA);
                    }
                    if (obj instanceof WzImageFile file && file.isErrorStatus()) {
                        setForeground(Color.RED);
                    } else if (obj instanceof WzDirectory dir && dir.isWzFile() && dir.getWzFile().isErrorStatus()) {
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
                WzObject wzObject = (WzObject) node.getUserObject();
                handleTreeClick(wzObject);
                MainFrame.getInstance().getCenterPane().getAnotherPane(EditPane.this).syncTreeClick(wzObject);
            }
        });

        // 右键菜单
        wzFilePopupMenu = new WzFileMenu(this, tree);
        wzFolderPopupMenu = new WzFolderMenu(this, tree);
        wzImageFilePopupMenu = new WzImageFileMenu(this, tree);
        wzXmlFilePopupMenu = new WzXmlFileMenu(this, tree);
        wzDirectoryPopupMenu = new WzDirectoryMenu(this, tree);
        wzImagePopupMenu = new WzImageMenu(this, tree);
        wzListPropertyPopupMenu = new WzListPropertyMenu(this, tree);
        wzValuePropertyPopupMenu = new WzValuePropertyMenu(this, tree);
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
                if (wzObject instanceof WzFolder) {
                    wzFolderPopupMenu.show(tree, e.getX(), e.getY());
                } else if (wzObject instanceof WzDirectory directory) {
                    if (directory.isWzFile()) {
                        wzFilePopupMenu.show(tree, e.getX(), e.getY());
                    } else {
                        wzDirectoryPopupMenu.show(tree, e.getX(), e.getY());
                    }
                } else if (wzObject instanceof WzImageFile) {
                    wzImageFilePopupMenu.show(tree, e.getX(), e.getY());
                } else if (wzObject instanceof WzXmlFile) {
                    wzXmlFilePopupMenu.show(tree, e.getX(), e.getY());
                } else if (wzObject instanceof WzImage) {
                    wzImagePopupMenu.show(tree, e.getX(), e.getY());
                } else if (wzObject instanceof WzImageProperty prop) {
                    if (prop.isListProperty()) {
                        wzListPropertyPopupMenu.show(tree, e.getX(), e.getY());
                    } else {
                        wzValuePropertyPopupMenu.show(tree, e.getX(), e.getY());
                    }
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
                        WzObject wzObject = (WzObject) node.getUserObject();
                        EditPaneAction finalAction;
                        if (node.isLeaf()) {
                            // 叶子节点：执行业务逻辑
                            handleTreeDoubleClick(node);
                            finalAction = EditPaneAction.ACTION;
                        } else {
                            // 非叶子节点：手动切换展开状态
                            if (tree.isExpanded(path)) {
                                tree.collapsePath(path);
                                finalAction = EditPaneAction.COLLAPSE;
                            } else {
                                tree.expandPath(path);
                                finalAction = EditPaneAction.EXPAND;
                            }
                        }
                        MainFrame.getInstance().getCenterPane().getAnotherPane(EditPane.this).syncTreeDoubleClick(wzObject, finalAction);
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

        addKeyEvent();

        return new JScrollPane(tree);
    }

    private JPanel createValuePane() {
        // CardLayout 容器
        formCards = new JPanel(new CardLayout());
        // 这两行是为了防止拉伸右侧边框后导致SplitPanel左侧的视图无法再放大
        formCards.setMinimumSize(new Dimension(0, 0));
        formCards.setPreferredSize(new Dimension(0, 0));

        for (var obj : nodeForms.entrySet()) {
            formCards.add(obj.getValue().getValuePane(), obj.getKey());
        }

        // 默认显示 node 表单
        ((CardLayout) formCards.getLayout()).show(formCards, "node");

        return formCards;
    }

    private void switchForm(String formName) {
        CardLayout cl = (CardLayout) (formCards.getLayout());
        cl.show(formCards, formName);

        if (currentFormName != null && !currentFormName.equals(formName)) {
            nodeForms.get(currentFormName).onHide();
        }
        currentFormName = formName;
    }

    /**
     * 单击事件
     *
     * @param wzObject 目标 WzObject
     */
    private void handleTreeClick(WzObject wzObject) {
        switch (wzObject) {
            case WzFolder obj -> {
                getNodeForm().setData(obj.getName(), WzType.FOLDER.name(), wzObject, this);
                switchForm("node");
            }
            case WzDirectory obj -> {
                if (obj.isWzFile()) {
                    getNodeForm().setData(obj.getName(), WzType.WZ_FILE.name(), wzObject, this);
                } else {
                    getNodeForm().setData(obj.getName(), WzType.DIRECTORY.name(), wzObject, this);
                }
                switchForm("node");
            }
            case WzImage obj -> {
                getNodeForm().setData(obj.getName(), WzType.IMAGE.name(), wzObject, this);
                switchForm("node");
            }
            case WzCanvasProperty obj -> {
                getCanvasForm().setData(obj.getName(), WzType.CANVAS_PROPERTY.name(), obj.getPngImage(true), obj.getWidth(), obj.getHeight(), obj.getFormat(), obj.getScale(), wzObject, this);
                switchForm("canvas");
            }
            case WzConvexProperty obj -> {
                getNodeForm().setData(obj.getName(), WzType.CONVEX_PROPERTY.name(), wzObject, this);
                switchForm("node");
            }
            case WzDoubleProperty obj -> {
                getDoubleForm().setData(obj.getName(), WzType.DOUBLE_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("double");
            }
            case WzFloatProperty obj -> {
                getFloatForm().setData(obj.getName(), WzType.FLOAT_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("float");
            }
            case WzIntProperty obj -> {
                getIntForm().setData(obj.getName(), WzType.INT_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("int");
            }
            case WzListProperty obj -> {
                getNodeForm().setData(obj.getName(), WzType.LIST_PROPERTY.name(), wzObject, this);
                switchForm("node");
            }
            case WzLongProperty obj -> {
                getLongForm().setData(obj.getName(), WzType.LONG_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("long");
            }
            case WzNullProperty obj -> {
                getNodeForm().setData(obj.getName(), WzType.NULL_PROPERTY.name(), wzObject, this);
                switchForm("node");
            }
            case WzShortProperty obj -> {
                getShortForm().setData(obj.getName(), WzType.SHORT_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("short");
            }
            case WzSoundProperty obj -> {
                getSoundForm().setData(obj.getName(), WzType.SOUND_PROPERTY.name(), obj.getSoundBytes(), obj.getLenMs(), wzObject, this);
                switchForm("sound");
            }
            case WzStringProperty obj -> {
                getStringForm().setData(obj.getName(), WzType.STRING_PROPERTY.name(), obj.getValue(), wzObject, this);
                switchForm("string");
            }
            case WzUOLProperty obj -> {
                WzObject target = obj.getUolTarget();
                if (target instanceof WzCanvasProperty cav) {
                    getUolCanvasForm().setData(obj.getName(), WzType.UOL_PROPERTY.name(), obj.getValue(), cav, wzObject, this);
                    switchForm("uolCanvas");
                } else if (target instanceof WzSoundProperty sound) {
                    getUolSoundForm().setData(obj.getName(), WzType.UOL_PROPERTY.name(), obj.getValue(), sound, wzObject, this);
                    switchForm("uolSound");
                }
            }
            case WzVectorProperty obj -> {
                getVectorForm().setData(obj.getName(), WzType.VECTOR_PROPERTY.name(), obj.getX(), obj.getY(), wzObject, this);
                switchForm("vector");
            }
            default -> {
                MainFrame.getInstance().setStatusText("%s 未知的节点类型 %s", wzObject.getName(), wzObject.getClass().getSimpleName());
                return;
            }
        }

        // 更新状态栏
        String text = wzObject.getPath();
        if (wzObject instanceof WzFolder obj) {
            text = text + "  /  " + obj.getKeyBoxName();
        } else if (wzObject instanceof WzDirectory obj && obj.isWzFile()) {
            text = text + "  /  " + obj.getWzFile().getKeyBoxName() + "  /  版本 " + obj.getWzFile().getHeader().getFileVersion();
        } else if (wzObject instanceof WzImageFile obj) {
            text = text + "  /  " + obj.getKeyBoxName();
        }
        MainFrame.getInstance().setStatusText(text);
    }

    /**
     * 双击事件
     *
     * @param node 节点
     * @return SwingWorker 用于捕获异常
     */
    private SwingWorker<Void, Void> handleTreeDoubleClick(DefaultMutableTreeNode node) {
        WzObject wzObject = (WzObject) node.getUserObject();
        MainFrame.getInstance().setStatusText("加载 %s...", wzObject.getName());

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                expandTreeNode(node, true, true, true);
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
        };

        worker.execute();
        return worker;
    }

    /**
     * 展开叶子节点，加载子节点进来
     *
     * @param node     叶子节点
     * @param parseWz  如果是 wz 是否解析
     * @param parseImg 如果是 img 是否解析
     * @param expand   是否展开节点
     */
    public void expandTreeNode(DefaultMutableTreeNode node, boolean parseWz, boolean parseImg, boolean expand) {
        if (!node.isLeaf()) return;

        WzObject wzObject = (WzObject) node.getUserObject();
        switch (wzObject) {
            case WzFolder folder -> {
                List<WzObject> children = folder.getChildren();
                sortWzObjects(children);
                children.forEach(child -> insertNodeToTree(node, child, expand));
            }
            case WzDirectory wzDir -> {
                if (wzDir.isWzFile() && parseWz && !wzDir.getWzFile().parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzDir.getName(), wzDir.getWzFile().getStatus().getMessage());
                    throw new RuntimeException();
                }
                addChildrenRecursively(node, wzDir, expand);
            }
            case WzImage wzImg -> {
                if (parseImg && !wzImg.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImg.getName(), wzImg.getStatus().getMessage());
                    throw new RuntimeException();
                }
                addChildrenRecursively(node, wzImg, expand);
            }
            default -> {
            }
        }
    }

    /**
     * 递归操作，将 WzObject 的 children 递归加入到节点来
     * <p>
     * 提示：不涉及解析，为解析的节点，children 为 0
     *
     * @param node     要展开的节点
     * @param wzObject 目标 WzObject
     * @param expand   是否展开
     */
    private void addChildrenRecursively(DefaultMutableTreeNode node, WzObject wzObject, boolean expand) {
        if (node.getChildCount() > 0) return;

        List<? extends WzObject> children = null;
        if (wzObject instanceof WzDirectory wzDir) {
            children = wzDir.getChildren();
        } else if (wzObject instanceof WzImage wzImg) {
            children = wzImg.getChildren();
        } else if (wzObject instanceof WzImageProperty prop) {
            children = prop.getChildren();
        }

        if (children == null || children.isEmpty()) return;

        sortWzObjects(children);

        for (WzObject child : children) {
            DefaultMutableTreeNode childNode = insertNodeToTree(node, child, expand);
            addChildrenRecursively(childNode, child, false);
        }
    }

    /**
     * WzObject 按名称排序：类型 Folder > WzFile > WzDir > 名称 自然数 > 字母
     *
     * @param objects 要排序的 List
     */
    private static void sortWzObjects(List<? extends WzObject> objects) {
        List<WzType> typePriority = List.of(
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

    /**
     * 快捷键
     */
    private void addKeyEvent() {
        // 获取 JTree 的输入映射和动作映射
        InputMap im = tree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = tree.getActionMap();
        // Delete 删除
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath[] selectedPaths = tree.getSelectionPaths();
                if (selectedPaths == null || selectedPaths.length == 0) return;

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                WzObject wzObject = (WzObject) node.getUserObject();
                if (wzObject instanceof WzFolder) {
                    return;
                } else if (wzObject instanceof WzDirectory directory) {
                    if (directory.isWzFile()) {
                        return;
                    } else {
                        wzDirectoryPopupMenu.getDeleteBtn().doClick();
                    }
                } else if (wzObject instanceof WzImageFile || wzObject instanceof WzXmlFile) {
                    return;
                } else if (wzObject instanceof WzImage) {
                    wzImagePopupMenu.getDeleteBtn().doClick();
                } else if (wzObject instanceof WzImageProperty prop) {
                    if (prop.isListProperty()) {
                        wzListPropertyPopupMenu.getDeleteBtn().doClick();
                    } else {
                        wzValuePropertyPopupMenu.getDeleteBtn().doClick();
                    }
                }
            }
        });

        // Ctrl+C 复制
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
        am.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath[] selectedPaths = tree.getSelectionPaths();
                if (selectedPaths == null || selectedPaths.length == 0) return;

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                WzObject wzObject = (WzObject) node.getUserObject();
                if (wzObject instanceof WzFolder) {
                    return;
                } else if (wzObject instanceof WzDirectory directory) {
                    if (directory.isWzFile()) {
                        return;
                    } else {
                        wzDirectoryPopupMenu.getCopyBtn().doClick();
                    }
                } else if (wzObject instanceof WzImageFile) {
                    wzImageFilePopupMenu.getCopyBtn().doClick();
                } else if (wzObject instanceof WzXmlFile) {
                    wzXmlFilePopupMenu.getCopyBtn().doClick();
                } else if (wzObject instanceof WzImage) {
                    wzImagePopupMenu.getCopyBtn().doClick();
                } else if (wzObject instanceof WzImageProperty prop) {
                    if (prop.isListProperty()) {
                        wzListPropertyPopupMenu.getCopyBtn().doClick();
                    } else {
                        wzValuePropertyPopupMenu.getCopyBtn().doClick();
                    }
                }
            }
        });

        // Ctrl+V 粘贴
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "paste");
        am.put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath[] selectedPaths = tree.getSelectionPaths();
                if (selectedPaths == null) return;

                if (selectedPaths.length != 1) {
                    JMessageUtil.error("不要多选");
                    return;
                }

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                WzObject wzObject = (WzObject) node.getUserObject();
                if (wzObject instanceof WzFolder) {
                    return;
                } else if (wzObject instanceof WzDirectory directory) {
                    if (directory.isWzFile()) {
                        wzFilePopupMenu.getPasteBtn().doClick();
                    } else {
                        wzDirectoryPopupMenu.getPasteBtn().doClick();
                    }
                } else if (wzObject instanceof WzImageFile) {
                    wzImageFilePopupMenu.getPasteBtn().doClick();
                } else if (wzObject instanceof WzXmlFile) {
                    wzXmlFilePopupMenu.getPasteBtn().doClick();
                } else if (wzObject instanceof WzImage) {
                    wzImagePopupMenu.getPasteBtn().doClick();
                } else if (wzObject instanceof WzImageProperty prop) {
                    if (prop.isListProperty()) {
                        wzListPropertyPopupMenu.getPasteBtn().doClick();
                    } else {
                        return;
                    }
                }
            }
        });

        // Ctrl+F 搜索
        EditPane editPane = this;
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "search");
        am.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchFormData option;
                while (true) {
                    option = searchDialog.getData();
                    if (option == null) break;

                    if (!option.nameMod() && !option.valueMod()) {
                        JMessageUtil.warn("你要搜索名称还是值？");
                        continue;
                    }

                    TreePath[] selectedPaths;
                    if (option.globalMod()) {
                        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                        int childCount = root.getChildCount();
                        selectedPaths = new TreePath[childCount];
                        for (int i = 0; i < childCount; i++) {
                            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
                            selectedPaths[i] = new TreePath(child.getPath());
                        }
                    } else {
                        selectedPaths = tree.getSelectionPaths();
                    }

                    if (selectedPaths == null || selectedPaths.length == 0) {
                        JMessageUtil.error("请选择要搜索的目标，或者勾选‘搜全局’");
                        continue;
                    }

                    searchResults.clear();
                    for (TreePath treePath : selectedPaths) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                        SearchUtil.search(
                                option.search(),
                                option.nameMod(),
                                option.valueMod(),
                                option.equalMod(),
                                option.lowMod(),
                                option.parseImgMod(),
                                searchResults,
                                node,
                                editPane
                        );
                    }

                    String title = "搜索结果 '" + option.search() + "'";
                    SearchResultDialog dialog = new SearchResultDialog(null, title, searchResults, editPane);
                    dialog.setVisible(true);
                    break;
                }
            }
        });
    }

    /**
     * 将文件加载到树里
     *
     * @param files 要打开的文件列表
     */
    public void loadFiles(List<File> files) {
        WzKey key = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();
        if (key == null) {
            MainFrame.getInstance().setStatusText("没有选择密钥?");
            return;
        }

        loadFiles(treeRoot, files, key);
    }

    public void loadFiles(DefaultMutableTreeNode pNode, List<File> files, WzKey key) {
        files.forEach(f -> {
            if (f.isFile()) {
                if (f.getName().endsWith("List.wz")) {
                    new ListEditor(f.getAbsolutePath(), key);
                } else if (f.getName().endsWith(".wz")) {
                    WzFile wzFile = new WzFile(f.getAbsolutePath(), (short) -1, key.getName(), key.getIv(), key.getUserKey());
                    insertNodeToTree(pNode, wzFile.getWzDirectory(), true);
                } else if (f.getName().endsWith(".img")) {
                    WzImageFile wzImageFile = new WzImageFile(f.getName(), f.getAbsolutePath(), key.getName(), key.getIv(), key.getUserKey());
                    insertNodeToTree(pNode, wzImageFile, true);
                } else if (f.getName().endsWith(".xml")) {
                    WzXmlFile wzXmlFile = new WzXmlFile(f.getName(), f.getAbsolutePath(), key.getName(), key.getIv(), key.getUserKey());
                    insertNodeToTree(pNode, wzXmlFile, true);
                }
            } else if (f.isDirectory()) {
                WzFolder folder = new WzFolder(f.getAbsolutePath(), key.getName(), key.getIv(), key.getUserKey());
                insertNodeToTree(pNode, folder, true);
            }
        });
    }

    /**
     * 使用菜单栏的密钥重新载入文件节点
     *
     * @param treePaths tree 选中的节点
     */
    public void reloadFile(TreePath[] treePaths) {
        WzKey key = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();
        if (key == null) {
            MainFrame.getInstance().setStatusText("没有选择密钥?");
            return;
        }

        for (TreePath treePath : treePaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            reloadFile(node, key);
        }

        clear();
    }

    private void reloadFile(DefaultMutableTreeNode node, WzKey key) {
        DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
        int index = pNode.getIndex(node);
        WzObject oldObject = (WzObject) node.getUserObject();
        WzObject newObject = null;

        if (oldObject instanceof WzFolder oldFolder) {
            newObject = new WzFolder(oldFolder.getFilePath(), key.getName(), key.getIv(), key.getUserKey());
        } else if (oldObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            WzFile oldWzFile = wzDir.getWzFile();
            String filePath = oldWzFile.getFilePath();
            short version = oldWzFile.getHeader().getFileVersion();
            WzFile newWzFile = new WzFile(filePath, version, key.getName(), key.getIv(), key.getUserKey());
            newObject = newWzFile.getWzDirectory();
        } else if (oldObject instanceof WzImageFile oldImg) {
            Path filePath = Path.of(oldImg.getFilePath());
            String filename = filePath.getFileName().toString();
            newObject = new WzImageFile(filename, filePath.toString(), key.getName(), key.getIv(), key.getUserKey());
        } else if (oldObject instanceof WzXmlFile oldXml) {
            Path filePath = Path.of(oldXml.getFilePath());
            String filename = filePath.getFileName().toString();
            newObject = new WzXmlFile(filename, filePath.toString(), key.getName(), key.getIv(), key.getUserKey());
        }

        removeNodeFromTree(node);
        insertNodeToTree(pNode, newObject, true, index);

        if (pNode.getUserObject() instanceof WzFolder wzFolder) {
            wzFolder.remove(oldObject);
            wzFolder.add(newObject);
        }
    }

    /**
     * 将 WzObject 插入到指定节点的末尾
     *
     * @param parentNode 目标节点
     * @param object     要插入的 WzObject
     * @param expand     插入后展开/刷新上级节点
     * @return 插入后生成的新 Node
     */
    public DefaultMutableTreeNode insertNodeToTree(DefaultMutableTreeNode parentNode, WzObject object, boolean expand) {
        return insertNodeToTree(parentNode, object, expand, -1);
    }

    /**
     * 将 WzObject 插入到指定节点的指定位置
     *
     * @param parentNode 目标节点
     * @param object     要插入的 WzObject
     * @param expand     插入后刷新/展开目标节点
     * @param index      插入位置
     * @return 插入后生成的新 Node
     */
    public DefaultMutableTreeNode insertNodeToTree(DefaultMutableTreeNode parentNode, WzObject object, boolean expand, int index) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(object);
        model.insertNodeInto(newNode, parentNode, index == -1 ? parentNode.getChildCount() : index);

        if (expand) {
            tree.expandPath(new TreePath(parentNode.getPath()));
        }

        return newNode;
    }

    /**
     * 从树里移除节点
     *
     * @param node 任意节点
     */
    public void removeNodeFromTree(DefaultMutableTreeNode node) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

        if (node == null) return;
        if (node.getParent() == null) return;

        model.removeNodeFromParent(node);
    }

    /**
     * 重置编辑框，避免编辑框里的 WzObject 占着已卸载的对象，无法释放内存
     */
    public void resetValueForm() {
        getNodeForm().setData("", "", null, this);
        switchForm("node");
    }

    /**
     * 在树里查找到 WzObject 对象（根据 WzObject.getPath 相对路径），并同步单击操作
     *
     * @param object 目标 WzObject
     */
    public void syncTreeClick(WzObject object) {
        if (!MainFrame.getInstance().getCenterPane().isRightShowing()) return;
        if (!MainFrame.getInstance().getCenterPane().isSync()) return;

        DefaultMutableTreeNode node = treeRoot;
        String[] paths = object.getPath().split("/");
        for (int i = 0; i < paths.length; i++) {
            node = findTreeNodeByName(node, paths[i]);
            if (node == null) break;

            if (i == paths.length - 1) {
                tree.setSelectionPath(new TreePath(node.getPath()));
            } else {
                if (node.isLeaf()) {
                    handleTreeDoubleClick(node);
                } else {
                    tree.expandPath(new TreePath(node.getPath()));
                }
            }
        }
    }

    /**
     * 在树里查找到 WzObject 对象（根据 WzObject.getPath 相对路径），并同步展开/收缩/双击操作
     *
     * @param object      目标 WzObject
     * @param finalAction 要同步的操作
     */
    public void syncTreeDoubleClick(WzObject object, EditPaneAction finalAction) {
        if (!MainFrame.getInstance().getCenterPane().isRightShowing()) return;
        if (!MainFrame.getInstance().getCenterPane().isSync()) return;

        DefaultMutableTreeNode node = treeRoot;
        String[] paths = object.getPath().split("/");
        for (int i = 0; i < paths.length; i++) {
            node = findTreeNodeByName(node, paths[i]);
            if (node == null) break;

            if (i == paths.length - 1) {
                if (finalAction == EditPaneAction.ACTION) {
                    handleTreeDoubleClick(node);
                } else if (finalAction == EditPaneAction.COLLAPSE) {
                    tree.collapsePath(new TreePath(node.getPath()));
                } else if (finalAction == EditPaneAction.EXPAND) {
                    tree.expandPath(new TreePath(node.getPath()));
                }
            } else {
                if (node.isLeaf()) {
                    handleTreeDoubleClick(node);
                } else {
                    tree.expandPath(new TreePath(node.getPath()));
                }
            }
        }
    }

    /**
     * 搜索 node 的下一级，查找符合名称的节点
     *
     * @param parent 要查找的 node
     * @param name   要查找的名称 全匹配
     * @return child node
     */
    public DefaultMutableTreeNode findTreeNodeByName(DefaultMutableTreeNode parent, String name) {
        for (int j = 0; j < parent.getChildCount(); j++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(j);
            if (name.equals(((WzObject) child.getUserObject()).getName())) {
                return child;
            }
        }
        return null;
    }

    /**
     * 根据路径在树里查找 WzObject
     *
     * @param path 用 / 隔开，不含 Root
     * @return WzObject
     */
    public WzObject findTreeWzObjectByPath(String path) {
        DefaultMutableTreeNode node = treeRoot;
        String[] paths = path.split("/");
        for (int i = 0; i < paths.length; i++) {
            node = findTreeNodeByName(node, paths[i]);
            if (node == null) break;

            if (i == paths.length - 1) {
                return (WzObject) node.getUserObject();
            } else {
                if (node.isLeaf()) {
                    handleTreeDoubleClick(node);
                } else {
                    tree.expandPath(new TreePath(node.getPath()));
                }
            }
        }

        return null;
    }

    /**
     * 跳转到 path 对应的节点
     *
     * @param paths 节点路径，不含 Root
     */
    public void focusNodeByPath(List<String> paths) {
        DefaultMutableTreeNode node = treeRoot;
        for (int i = 0; i < paths.size(); i++) {
            node = findTreeNodeByName(node, paths.get(i));
            if (node == null) break;

            if (i == paths.size() - 1) {
                TreePath path = new TreePath(node.getPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path); // 关键，滚动到可见
            } else {
                if (node.isLeaf()) {
                    SwingWorker<Void, Void> worker = handleTreeDoubleClick(node);
                    try {
                        worker.get(); // 等待完成
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    tree.expandPath(new TreePath(node.getPath()));
                }
            }
        }
    }

    /**
     * 移除 Root 下全部节点
     */
    public void unloadAll() {
        treeRoot.removeAllChildren();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.reload(treeRoot);
        resetValueForm();
    }

    private void clear() {
        resetValueForm();
        System.gc();
    }

    /**
     * 保存节点文件
     *
     * @param treePaths 选中的节点
     */
    public void saveFiles(TreePath[] treePaths) {
        for (TreePath treePath : treePaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            if (node.getUserObject() instanceof WzFolder) {
                saveWzFolder(node);
            } else {
                saveFile(node);
            }
        }

        clear();
    }

    /**
     * 保存文件夹
     *
     * @param node WzFolder 对应的节点
     */
    private void saveWzFolder(DefaultMutableTreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (child.getUserObject() instanceof WzFolder) {
                saveWzFolder(child);
            } else {
                saveFile(child);
            }
        }
    }

    /**
     * 保存文件
     *
     * @param node WzFile / WzImageFile / WzXmlFile 对应的节点
     */
    private void saveFile(DefaultMutableTreeNode node) {
        WzObject wzObject = (WzObject) node.getUserObject();
        if (wzObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            wzObject = wzDir.getWzFile();
        }

        if (wzObject instanceof WzSavableFile wz) {
            String keyBoxName = wz.getKeyBoxName();
            byte[] iv = wz.getIv();
            byte[] key = wz.getKey();

            if (wz.getStatus() != WzFileStatus.PARSE_SUCCESS) {
                log.info("未加载文件 {} 不需要保存, 跳过...", wz.getName());
                return;
            }

            Path oldPath = Path.of(wz.getFilePath());
            String fileName = oldPath.getFileName().toString();
            boolean renamed = !fileName.equals(wz.getName());

            if (renamed) {
                Path newPath = oldPath.resolveSibling(wz.getName());
                wz.setFilePath(newPath.toString());
            }

            if (wz.save()) {
                reloadFile(node, new WzKey(-1, keyBoxName, iv, key));
                if (renamed) {
                    FileTool.deleteFile(oldPath);
                }
            }
        }
    }

    /**
     * 文件另存为
     *
     * @param node 要保存的文件节点
     */
    public void saveAs(DefaultMutableTreeNode node) {
        WzObject wzObject = (WzObject) node.getUserObject();
        if (wzObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            wzObject = wzDir.getWzFile();
        }

        if (wzObject instanceof WzSavableFile wz) {
            String keyBoxName = wz.getKeyBoxName();
            byte[] iv = wz.getIv();
            byte[] key = wz.getKey();

            if (wz.getStatus() != WzFileStatus.PARSE_SUCCESS) {
                log.info("未加载文件 {} 不需要另存为, 跳过...", wz.getName());
                return;
            }

            File newFile = new File(wz.getFilePath());
            newFile = new File(newFile.getParent(), wz.getName());

            String[] filter = switch (wzObject) {
                case WzFile ignored -> new String[]{"wz"};
                case WzImageFile ignored -> new String[]{"img"};
                case WzXmlFile ignored -> new String[]{"xml"};
                default -> null;
            };

            File saveFile = FileDialog.chooseSaveFile(MainFrame.getInstance(), "保存 " + wz.getName(), newFile, filter);
            if (saveFile == null) {
                return;
            }
            wz.setFilePath(saveFile.getAbsolutePath());
            if (wz.save()) {
                reloadFile(node, new WzKey(-1, keyBoxName, iv, key));
            }
        }
    }

    private void collectExportImg(DefaultMutableTreeNode node, Path folder, List<Pair<WzImage, Path>> collector) {
        WzObject wzObject = (WzObject) node.getUserObject();
        if (wzObject instanceof WzFolder) {
            folder = folder.resolve(wzObject.getName());
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                collectExportImg(child, folder, collector);
            }
        } else if (wzObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            WzFile wzFile = wzDir.getWzFile();

            if (!wzFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzFile.getName(), wzFile.getStatus().getMessage());
                throw new RuntimeException();
            }
            wzFile.exportFileToImg(folder, collector);
        } else if (wzObject instanceof WzImage wzImage) {
            if (!wzImage.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImage.getName(), wzImage.getStatus().getMessage());
                throw new RuntimeException();
            }

            Path p;
            if (wzImage instanceof WzXmlFile wzXmlFile) {
                p = folder.resolve(wzXmlFile.getImgName());
            } else {
                p = folder.resolve(wzImage.getName());
            }

            collector.add(new Pair<>(wzImage, p));
        }
    }

    public void exportImg(TreePath[] selectedPaths) {
        File folder = FileDialog.chooseOpenFolder("请选择输出目录");
        if (folder == null) {
            log.info("用户取消了操作");
            return;
        }

        Instant now = Instant.now();

        List<Pair<WzImage, Path>> collector = new ArrayList<>();
        for (TreePath treePath : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            collectExportImg(node, folder.toPath(), collector);
        }

        int total = collector.size();
        new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int finish = 0;
                for (Pair<WzImage, Path> pair : collector) {
                    WzImage wzImage = pair.getLeft();
                    Path path = pair.getRight();
                    wzImage.save(path);
                    MainFrame.getInstance().updateProgress(++finish, total);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Instant end = Instant.now();
                    MainFrame.getInstance().setStatusText("导出完成，耗时 %d 秒", Duration.between(now, end).toSeconds());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }.execute();
    }

    private void collectExportXml(DefaultMutableTreeNode node, Path folder, List<Pair<WzImage, Path>> collector) {
        WzObject wzObject = (WzObject) node.getUserObject();

        if (wzObject instanceof WzFolder) {
            folder = folder.resolve(wzObject.getName());

            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                collectExportXml(child, folder, collector);
            }
        } else if (wzObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            WzFile wzFile = wzDir.getWzFile();

            if (!wzFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzFile.getName(), wzFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            wzFile.exportFileToXml(folder, collector);
        } else if (wzObject instanceof WzImage wzImage) {
            if (!wzImage.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImage.getName(), wzImage.getStatus().getMessage());
                throw new RuntimeException();
            }

            String filename = wzImage.getName();
            if (!filename.endsWith(".xml")) {
                filename = filename + ".xml";
            }

            collector.add(new Pair<>(wzImage, folder.resolve(filename)));
        }
    }

    public void exportXml(TreePath[] selectedPaths) {
        ExportXmlDialog dialog = new ExportXmlDialog(this);
        ExportXmlData data = dialog.getData();
        if (data == null) return;

        Instant now = Instant.now();

        List<Pair<WzImage, Path>> collector = new ArrayList<>();
        for (TreePath treePath : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            collectExportXml(node, Path.of(data.getExportPath()), collector);
        }

        int total = collector.size();
        new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int finish = 0;
                for (Pair<WzImage, Path> pair : collector) {
                    WzImage wzImage = pair.getLeft();
                    Path path = pair.getRight();
                    wzImage.exportToXml(path, data.getIndent(), data.getMeType());
                    MainFrame.getInstance().updateProgress(++finish, total);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Instant end = Instant.now();
                    MainFrame.getInstance().setStatusText("导出完成，耗时 %d 秒", Duration.between(now, end).toSeconds());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }.execute();
    }
}
