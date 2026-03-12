package orange.wz.gui.component.panel;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.dialog.*;
import orange.wz.gui.component.form.data.*;
import orange.wz.gui.component.form.impl.*;
import orange.wz.gui.component.menu.*;
import orange.wz.gui.utils.*;
import orange.wz.model.Pair;
import orange.wz.provider.*;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.*;
import orange.wz.provider.tools.wzkey.WzKey;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static orange.wz.gui.Icons.*;

@Getter
@Slf4j
public final class EditPane extends JSplitPane {
    private JTree tree;
    private DefaultMutableTreeNode treeRoot;
    private DefaultTreeModel treeModel;

    private JPanel formCards;

    private WzFileMenu wzFilePopupMenu;
    private WzFolderMenu wzFolderPopupMenu;
    private WzImageFileMenu wzImageFilePopupMenu;
    private WzXmlFileMenu wzXmlFilePopupMenu;
    private WzDirectoryMenu wzDirectoryPopupMenu;
    private WzImageMenu wzImagePopupMenu;
    private WzListPropertyMenu wzListPropertyPopupMenu;
    private WzValuePropertyMenu wzValuePropertyPopupMenu;

    @Getter
    private String currentFormName;
    @Getter
    private final Map<String, AbstractValueForm> nodeForms = Map.ofEntries(
            Map.entry("node", new NodeForm()),
            Map.entry("canvas", new CanvasForm(this)),
            Map.entry("double", new DoubleForm()),
            Map.entry("float", new FloatForm()),
            Map.entry("int", new IntForm()),
            Map.entry("long", new LongForm()),
            Map.entry("short", new ShortForm()),
            Map.entry("sound", new SoundForm()),
            Map.entry("string", new StringForm()),
            Map.entry("uolCanvas", new UolCanvasForm(this)),
            Map.entry("uolSound", new UolSoundForm()),
            Map.entry("vector", new VectorForm()),
            Map.entry("lua", new LuaForm())
    );

    private final SearchDialog searchDialog = new SearchDialog("搜索", this);
    private final List<SearchResult> searchResults = new ArrayList<>();

    // 按键搜索
    private final StringBuilder inputBuffer = new StringBuilder();
    private Instant lastInputTime = Instant.EPOCH;

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

    public LuaForm getLuaForm() {
        return (LuaForm) nodeForms.get("lua");
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
        treeModel = (DefaultTreeModel) tree.getModel();

        tree.setDropMode(DropMode.ON);
        tree.setTransferHandler(new FileDropTransferHandler(this));

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
                        case LUA_PROPERTY -> LuaIcon;
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
                handleTreeClick(node);
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
                                collapseAll(path);
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

    private void collapseAll(TreePath parent) {
        if (!tree.isExpanded(parent)) return;

        Object node = parent.getLastPathComponent();
        TreeModel model = treeModel;

        int childCount = model.getChildCount(node);
        for (int i = 0; i < childCount; i++) {
            Object child = model.getChild(node, i);
            TreePath childPath = parent.pathByAddingChild(child);
            collapseAll(childPath);
        }

        tree.collapsePath(parent);
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
     * node 单击事件
     */
    private void handleTreeClick(DefaultMutableTreeNode node) {
        WzObject wzObject = (WzObject) node.getUserObject();
        String npcAction = null;
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
                if (obj.getWzImage().getName().equals("Npc.img") && !obj.getName().equals("name") && !obj.getName().equals("func")) {
                    npcAction = TreeNodeUtil.getNpcActionName(node);
                    if (npcAction == null) npcAction = "动作不存在";
                }
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
            case WzLuaProperty obj -> {
                getLuaForm().setData(obj.getName(), WzType.LUA_PROPERTY.name(), obj.getString(), wzObject, this);
                switchForm("lua");
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

        if (npcAction != null) {
            text = text + " (" + npcAction + ")";
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
                expandTreeNode(node, true, true, false);
                tree.expandPath(new TreePath(node.getPath()));
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
                WzTool.sortWzObjects(children);
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
            case WzImageProperty property -> addChildrenRecursively(node, property, expand); // 粘贴后的 List 节点
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

        WzTool.sortWzObjects(children);

        for (WzObject child : children) {
            DefaultMutableTreeNode childNode = insertNodeToTree(node, child, expand);
            addChildrenRecursively(childNode, child, false);
        }
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
                        int childCount = treeRoot.getChildCount();
                        selectedPaths = new TreePath[childCount];
                        for (int i = 0; i < childCount; i++) {
                            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeRoot.getChildAt(i);
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

        // 按键搜索
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                TreePath path = tree.getSelectionPath();
                if (path == null) return;

                char c = e.getKeyChar();
                if (!Character.isLetterOrDigit(c)) return;

                Instant now = Instant.now();
                if (now.minusMillis(500).isAfter(lastInputTime)) {
                    inputBuffer.setLength(0); // 超时 → 重置
                }
                lastInputTime = now;

                inputBuffer.append(c);
                String prefix = inputBuffer.toString().toLowerCase();


                selectNextMatchingSibling(prefix, path, true);
            }
        });
    }

    // Tree 操作 --------------------------------------------------------------------------------------------------------

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
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(object);
        treeModel.insertNodeInto(newNode, parentNode, index == -1 ? parentNode.getChildCount() : index);

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
        if (node == null) return;
        if (node.getParent() == null) return;

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            removeNodeFromTree(child);
        }
        node.setUserObject(null);
        treeModel.removeNodeFromParent(node);
    }

    /**
     * 根据路径在树里查找 WzObject
     *
     * @param path 用 / 隔开，不含 Root
     * @return WzObject
     */
    public WzObject findWzObjectInTreeByPath(String path) {
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
     * 在当前选中节点的同级节点中：
     * 1. 先从“当前节点之后”找第一个以 prefix 开头的
     * 2. 如果没找到，再从“当前节点之前”找第一个
     */
    private void selectNextMatchingSibling(String prefix, TreePath path, boolean toNext) {
        DefaultMutableTreeNode current = (DefaultMutableTreeNode) path.getLastPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) current.getParent();
        if (parent == null) return;

        int count = parent.getChildCount();
        int currentIndex = toNext ? parent.getIndex(current) : parent.getChildCount();

        DefaultMutableTreeNode target = null;
        // 从当前之后查找
        if (toNext) {
            for (int i = currentIndex; i < count; i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);
                WzObject wzObject = (WzObject) node.getUserObject();
                if (wzObject.getName().toLowerCase().startsWith(prefix)) {
                    target = node;
                    break;
                }
            }
        }

        if (target == null) {
            // 从当前之前查找
            for (int i = 0; i < currentIndex; i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);
                WzObject wzObject = (WzObject) node.getUserObject();
                if (wzObject.getName().toLowerCase().startsWith(prefix)) {
                    target = node;
                    break;
                }
            }
        }

        if (target != null) {
            TreePath newPath = new TreePath(target.getPath());
            tree.setSelectionPath(newPath);
            tree.scrollPathToVisible(newPath);
        } else {
            // 找不到的情况下，试着往下一级去找
            if (!current.isLeaf() && tree.isExpanded(path)) {
                selectNextMatchingSibling(prefix, new TreePath(((DefaultMutableTreeNode) current.getFirstChild()).getPath()), false);
            }
        }
    }

    private static class FileDropTransferHandler extends TransferHandler {
        private final EditPane editPane;

        public FileDropTransferHandler(EditPane editPane) {
            this.editPane = editPane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;

            try {
                List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                if (files.isEmpty()) return false;

                for (File file : files) {
                    if (file.isFile()) {

                        String name = file.getName().toLowerCase();
                        if (!(name.endsWith(".xml") || name.endsWith(".wz") || name.endsWith(".img"))) {
                            JMessageUtil.error("包含了未知的文件！");
                            return false;
                        }
                    }
                }

                JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dl.getPath();

                if (path == null) {
                    editPane.loadFiles(files);
                } else {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) path.getLastPathComponent();
                    WzObject wzObject = (WzObject) parent.getUserObject();
                    if (wzObject instanceof WzFolder) {
                        editPane.attachFolder(parent, files);
                    } else if (wzObject instanceof WzDirectory) {
                        List<File> imgFiles = new ArrayList<>();
                        List<File> xmlFiles = new ArrayList<>();
                        List<File> dirs = new ArrayList<>();
                        for (File file : files) {
                            String filename = file.getName();
                            if (file.isDirectory()) {
                                dirs.add(file);
                            } else if (filename.endsWith(".img")) {
                                imgFiles.add(file);
                            } else if (filename.endsWith(".xml")) {
                                xmlFiles.add(file);
                            } else {
                                log.warn("非 img、xml 类型的文件不能拖入到 WzDirectory");
                            }
                        }

                        editPane.attachWzDir(parent, dirs);
                        editPane.attachImg(parent, imgFiles);
                        editPane.attachXml(parent, xmlFiles);
                    } else {
                        log.warn("不能拖入的节点");
                    }
                }


                return true;
            } catch (Exception e) {
                log.error("拖入文件异常 {}", e.getMessage());
            }
            return false;
        }
    }

    // 编辑框 -----------------------------------------------------------------------------------------------------------

    /**
     * 重置编辑框，避免编辑框里的 WzObject 占着已卸载的对象，无法释放内存
     */
    public void resetValueForm() {
        getNodeForm().setData("", "", null, this);
        switchForm("node");
    }

    private void clear() {
        resetValueForm();
        System.gc();
    }

    // 同步 -------------------------------------------------------------------------------------------------------------

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
                    collapseAll(new TreePath(node.getPath()));
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

    // 加载 -------------------------------------------------------------------------------------------------------------
    private void loadFiles(DefaultMutableTreeNode pNode, List<File> files, WzKey key) {
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

    // 重载 -------------------------------------------------------------------------------------------------------------
    private void reloadFile(DefaultMutableTreeNode node, WzKey key) {
        DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
        int index = pNode.getIndex(node);
        WzObject oldObject = (WzObject) node.getUserObject();
        WzObject newObject = null;

        if (oldObject instanceof WzFolder oldFolder) {
            newObject = new WzFolder(oldFolder.getFilePath(), key.getName(), key.getIv(), key.getUserKey());
        } else if (oldObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            if (wzDir.getWzFile().isNewFile()) {
                JMessageUtil.error("新建的文件无法使用重载功能，请先另存为。");
                return;
            }
            WzFile oldWzFile = wzDir.getWzFile();
            String filePath = oldWzFile.getFilePath();
            WzFile newWzFile = new WzFile(filePath, (short) -1, key.getName(), key.getIv(), key.getUserKey());
            newObject = newWzFile.getWzDirectory();
        } else if (oldObject instanceof WzImageFile oldImg) {
            if (oldImg.isNewFile()) {
                JMessageUtil.error("新建的文件无法使用重载功能，请先另存为。");
                return;
            }
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

    // 卸载 -------------------------------------------------------------------------------------------------------------
    public void unload() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) return;

        for (TreePath treePath : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
            if (pNode == null) continue;

            WzObject wzObject = (WzObject) node.getUserObject();
            if (pNode.getUserObject() instanceof WzFolder pFolder) {
                pFolder.remove(wzObject);
            }

            removeNodeFromTree(node);
        }

        clear();
    }

    /**
     * 移除 Root 下全部节点
     */
    public void unloadAll() {
        treeRoot.removeAllChildren();
        treeModel.reload(treeRoot);
        resetValueForm();
    }

    // 排序并改名：对子列表进行排序，并将数值类型的名称按从0开始的自然序数进行改名，使其连续 --------------------------------------------
    public void sortAndReindexChildren() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (TreePathUtil.isNullOrMultiple(selectedPaths)) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
        WzObject wzObject = (WzObject) node.getUserObject();

        boolean success;
        if (wzObject instanceof WzListProperty listProperty) {
            success = listProperty.sortAndReindexChildren();
        } else if (wzObject instanceof WzImage image) {
            success = image.sortAndReindexChildren();
        } else {
            MainFrame.getInstance().setStatusText("该功能仅支持 image 或者 list 节点");
            return;
        }

        if (!success) {
            MainFrame.getInstance().setStatusText("节点已经是从0开始的序数了，已经为你按顺序排列，但是名称没有发生变化。");
            return;
        }

        DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
        int index = pNode.getIndex(node);
        removeNodeFromTree(node);
        MainFrame.getInstance().setStatusText("有名称发生了变化");
        insertNodeToTree(pNode, wzObject, true, index);
    }

    // 保存 -------------------------------------------------------------------------------------------------------------

    /**
     * 保存节点文件
     *
     * @param treePaths 选中的节点
     */
    public void saveFiles(TreePath[] treePaths) {
        MainFrame.getInstance().setStatusText("文件保存中");
        MainFrame.getInstance().updateProgress(0, 0);
        new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (TreePath treePath : treePaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    if (node.getUserObject() instanceof WzFolder) {
                        saveWzFolder(node);
                    } else {
                        saveFile(node);
                    }
                }

                clear();
                return null;
            }

            @Override
            protected void done() {
                MainFrame.getInstance().setStatusText("文件保存完毕");
            }
        }.execute();
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

        if (wzObject instanceof WzImageFile wzImageFile) {
            if (wzImageFile.isNewFile()) {
                JMessageUtil.error("新建的文件请使用另存为指定要保存的路径");
                return;
            }
        }

        if (wzObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            if (wzDir.getWzFile().isNewFile()) {
                JMessageUtil.error("新建的文件请使用另存为指定要保存的路径");
                return;
            }
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
                if (renamed) {
                    FileTool.deleteFile(oldPath);
                }
            } else {
                JMessageUtil.error("保存失败，请查看日志文件");
            }
            reloadFile(node, new WzKey(-1, keyBoxName, iv, key));
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
            if (!wz.save()) {
                JMessageUtil.error("保存失败，请查看日志文件");
            }
            reloadFile(node, new WzKey(-1, keyBoxName, iv, key));
        }
    }

    // 导出 -------------------------------------------------------------------------------------------------------------

    /**
     * 收集要导出的 Img节点
     *
     * @param node      要处理的节点
     * @param folder    用于存放导出文件的文件夹
     * @param collector 收集器
     */
    private void collectExportImg(DefaultMutableTreeNode node, Path folder, List<Pair<WzImage, Path>> collector) {
        WzObject wzObject = (WzObject) node.getUserObject();
        if (wzObject instanceof WzFolder) {
            expandTreeNode(node, false, false, false);
            String name = wzObject.getName().replaceAll("(?i)\\.wz$", "");
            folder = folder.resolve(name);
            int total = node.getChildCount();
            int current = 0;
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                collectExportImg(child, folder, collector);
                MainFrame.getInstance().updateProgress(++current, total);
            }
        } else if (wzObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            WzFile wzFile = wzDir.getWzFile();

            if (!wzFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzFile.getName(), wzFile.getStatus().getMessage());
                throw new RuntimeException();
            }
            wzFile.exportFileToImg(folder, collector);
        } else if (wzObject instanceof WzImage wzImage) {
            Path p;
            if (wzImage instanceof WzXmlFile wzXmlFile) {
                p = folder.resolve(wzXmlFile.getImgName());
            } else {
                p = folder.resolve(wzImage.getName());
            }

            collector.add(new Pair<>(wzImage, p));
        }
    }

    /**
     * 导出 img
     *
     * @param selectedPaths 要处理的节点
     */
    public void exportImg(TreePath[] selectedPaths) {
        File folder = FileDialog.chooseOpenFolder("请选择输出目录");
        if (folder == null) {
            log.info("用户取消了操作");
            return;
        }

        Instant now = Instant.now();
        new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                MainFrame.getInstance().setStatusText("开始收集并解析要导出的文件");
                int total = selectedPaths.length;
                int finish = 0;
                List<Pair<WzImage, Path>> collector = new ArrayList<>();
                for (TreePath treePath : selectedPaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    collectExportImg(node, folder.toPath(), collector);
                    MainFrame.getInstance().updateProgress(++finish, total);
                }

                total = collector.size();
                finish = 0;
                MainFrame.getInstance().setStatusText("开始导出文件");
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

    /**
     * 收集要导出的 XML 节点
     *
     * @param node      要处理的节点
     * @param folder    用于存放导出文件的文件夹
     * @param collector 收集器
     */
    private void collectExportXml(DefaultMutableTreeNode node, Path folder, List<Pair<WzImage, Path>> collector) {
        WzObject wzObject = (WzObject) node.getUserObject();

        if (wzObject instanceof WzFolder) {
            expandTreeNode(node, false, false, false);
            folder = folder.resolve(wzObject.getName());

            int total = node.getChildCount();
            int current = 0;
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                collectExportXml(child, folder, collector);
                MainFrame.getInstance().updateProgress(++current, total);
            }
        } else if (wzObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            WzFile wzFile = wzDir.getWzFile();

            if (!wzFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzFile.getName(), wzFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            wzFile.exportFileToXml(folder, collector);
        } else if (wzObject instanceof WzImage wzImage) {
            String filename = wzImage.getName();
            if (!filename.endsWith(".xml")) {
                filename = filename + ".xml";
            }

            collector.add(new Pair<>(wzImage, folder.resolve(filename)));
        }
    }

    /**
     * 导出 XML
     *
     * @param selectedPaths 要处理的节点
     */
    public void exportXml(TreePath[] selectedPaths) {
        ExportXmlDialog dialog = new ExportXmlDialog(this);
        ExportXmlData data = dialog.getData();
        if (data == null) return;

        Instant now = Instant.now();

        new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int total = selectedPaths.length;
                int finish = 0;
                List<Pair<WzImage, Path>> collector = new ArrayList<>();
                for (TreePath treePath : selectedPaths) {
                    MainFrame.getInstance().setStatusText("开始收集并解析要导出的文件");
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    collectExportXml(node, Path.of(data.getExportPath()), collector);
                    MainFrame.getInstance().updateProgress(++finish, total);
                }

                total = collector.size();
                finish = 0;
                MainFrame.getInstance().setStatusText("开始导出文件");
                for (Pair<WzImage, Path> pair : collector) {
                    WzImage wzImage = pair.getLeft();
                    Path path = pair.getRight();
                    if (wzImage.exportToXml(path, data.getIndent(), data.getMeType())) {
                        MainFrame.getInstance().updateProgress(++finish, total);
                    } else {
                        MainFrame.getInstance().setStatusText("%s 导出失败，请查看日志文件", wzImage.getName());
                    }
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

    // 修改密钥 ----------------------------------------------------------------------------------------------------------

    /**
     * 收集可以修改密钥的文件
     *
     * @param node      目标节点
     * @param collector 收集器
     * @return 是否存在 wz 文件
     */
    private boolean collectChangeKey(DefaultMutableTreeNode node, List<WzObject> collector) {
        WzObject wzObject = (WzObject) node.getUserObject();
        if (wzObject instanceof WzFolder) {
            expandTreeNode(node, false, false, false);
            boolean hasWzFiles = false;
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                if (collectChangeKey(child, collector)) hasWzFiles = true;
            }
            return hasWzFiles;
        } else if (wzObject instanceof WzDirectory wzDir && wzDir.isWzFile()) {
            collector.add(wzDir.getWzFile());
            return true;
        } else if (wzObject instanceof WzImageFile wzImg) {
            collector.add(wzImg);
        }
        return false;
    }

    /**
     * 修改密钥
     *
     * @param selectedPaths 要处理的节点
     */
    public void changeKey(TreePath[] selectedPaths) {
        List<WzObject> collector = new ArrayList<>();
        boolean hasWzFile = false;
        for (TreePath treePath : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            if (collectChangeKey(node, collector)) hasWzFile = true;
        }

        ChangeKeyDialog dialog = new ChangeKeyDialog(this, hasWzFile);
        KeyData keyData = dialog.getData();
        if (keyData == null) return;

        Instant now = Instant.now();
        MainFrame.getInstance().setStatusText("密钥修改中...");
        int total = collector.size();
        new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int finish = 0;
                for (WzObject wzObject : collector) {
                    if (wzObject instanceof WzFile wzFile) {
                        wzFile.changeKey(keyData.getVersion(), keyData.getName(), keyData.getIv(), keyData.getKey());
                        wzFile.getWzDirectory().setTempChanged(true);
                    } else if (wzObject instanceof WzImageFile wzImg) {
                        wzImg.changeKey(keyData.getName(), keyData.getIv(), keyData.getKey());
                        wzImg.setTempChanged(true);
                    }
                    MainFrame.getInstance().updateProgress(++finish, total);
                }
                tree.updateUI();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Instant end = Instant.now();
                    MainFrame.getInstance().setStatusText("密钥修改完成，耗时 %d 秒，请自行保存文件以应用新的密钥。", Duration.between(now, end).toSeconds());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }.execute();
    }

    // 导入--------------------------------------------------------------------------------------------------------------

    /**
     * 导入 Img
     *
     * @param node 导入到该节点
     */
    public void importImg(DefaultMutableTreeNode node) {
        List<File> imgFiles = FileDialog.chooseOpenFiles(new String[]{"img"});
        attachImg(node, imgFiles);
    }

    private void attachImg(DefaultMutableTreeNode node, List<File> imgFiles) {
        final int[] count = {0};
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                WzDirectory targetDirectory = (WzDirectory) node.getUserObject();
                WzFile wzFile = targetDirectory.getWzFile();
                String keyBoxName = wzFile.getKeyBoxName();
                byte[] iv = wzFile.getIv();
                byte[] key = wzFile.getKey();
                int total = imgFiles.size();
                OverwriteChoice choice = null;
                int index = 0;
                for (File imgFile : imgFiles) {
                    String imgName = imgFile.getName();
                    if (targetDirectory.existImage(imgName)) {
                        if (choice == OverwriteChoice.SKIP_ALL) continue;
                        else if (choice == OverwriteChoice.OVERWRITE_ALL) {
                            targetDirectory.removeImageChild(imgName);
                            DefaultMutableTreeNode childNode = findTreeNodeByName(node, imgName);
                            index = node.getIndex(childNode);
                            removeNodeFromTree(childNode);
                        } else {
                            choice = OverwriteDialog.show(EditPane.this, imgName);
                            switch (choice) {
                                case OVERWRITE, OVERWRITE_ALL -> {
                                    targetDirectory.removeImageChild(imgName);
                                    DefaultMutableTreeNode childNode = findTreeNodeByName(node, imgName);
                                    index = node.getIndex(childNode);
                                    removeNodeFromTree(childNode);
                                }
                                case SKIP, SKIP_ALL, CANCEL -> {
                                    continue;
                                }
                            }
                        }
                    }
                    String filePathStr = imgFile.getAbsolutePath();
                    WzImageFile wzImageFile = new WzImageFile(imgName, filePathStr, keyBoxName, iv, key);
                    if (!wzImageFile.parse()) {
                        MainFrame.getInstance().setStatusText("无法解析 Img, 停止导入。请确认 Img 的密钥和导入对象的密钥是否一致: %s", filePathStr);
                        return null;
                    }
                    WzImage wzImage = wzImageFile.deepClone(targetDirectory);
                    wzImage.setReader(new BinaryReader(wzFile.getWzMutableKey()));
                    wzImage.setChildrenWzImage();
                    wzImage.setTempChanged(true);
                    targetDirectory.addChild(wzImage);
                    insertNodeToTree(node, wzImage, true, index);
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
    }

    /**
     * 导入 XML
     *
     * @param node 导入到该节点
     */
    public void importXml(DefaultMutableTreeNode node) {
        List<File> xmlFiles = FileDialog.chooseOpenFiles(new String[]{"xml"});
        attachXml(node, xmlFiles);
    }

    private void attachXml(DefaultMutableTreeNode node, List<File> xmlFiles) {
        final int[] count = {0};
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                WzDirectory targetDirectory = (WzDirectory) node.getUserObject();
                WzFile wzFile = targetDirectory.getWzFile();
                String keyBoxName = wzFile.getKeyBoxName();
                byte[] iv = wzFile.getIv();
                byte[] key = wzFile.getKey();
                int total = xmlFiles.size();
                OverwriteChoice choice = null;
                int index = 0;
                for (File xmlFile : xmlFiles) {
                    String imgName = xmlFile.getName();
                    imgName = imgName.endsWith(".xml")
                            ? imgName.substring(0, imgName.length() - 4)
                            : imgName;
                    if (targetDirectory.existImage(imgName)) {
                        if (choice == OverwriteChoice.SKIP_ALL) continue;
                        else if (choice == OverwriteChoice.OVERWRITE_ALL) {
                            targetDirectory.removeImageChild(imgName);
                            DefaultMutableTreeNode childNode = findTreeNodeByName(node, imgName);
                            index = node.getIndex(childNode);
                            removeNodeFromTree(childNode);
                        } else {
                            choice = OverwriteDialog.show(EditPane.this, imgName);
                            switch (choice) {
                                case OVERWRITE, OVERWRITE_ALL -> {
                                    targetDirectory.removeImageChild(imgName);
                                    DefaultMutableTreeNode childNode = findTreeNodeByName(node, imgName);
                                    index = node.getIndex(childNode);
                                    removeNodeFromTree(childNode);
                                }
                                case SKIP, SKIP_ALL, CANCEL -> {
                                    continue;
                                }
                            }
                        }
                    }
                    String filePathStr = xmlFile.getAbsolutePath();
                    WzXmlFile wzXmlFile = new WzXmlFile(imgName, filePathStr, keyBoxName, iv, key);
                    if (!wzXmlFile.parse()) {
                        MainFrame.getInstance().setStatusText("无法解析 Xml, 停止导入。请查看相关日志: %s", filePathStr);
                        return null;
                    }
                    WzImage wzImage = wzXmlFile.deepClone(targetDirectory);
                    wzImage.setReader(new BinaryReader(wzFile.getWzMutableKey()));
                    wzImage.setChildrenWzImage();
                    wzImage.setTempChanged(true);
                    targetDirectory.addChild(wzImage);
                    insertNodeToTree(node, wzImage, true, index);
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
    }

    private void attachFolder(DefaultMutableTreeNode node, List<File> files) {
        final int[] count = {0};
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                WzFolder targetWzFolder = (WzFolder) node.getUserObject();
                targetWzFolder.getChildren(); // 只是调用一下确保加载了子项
                int total = files.size();
                TreePath treePath = new TreePath(node.getPath());
                boolean expanded = tree.isExpanded(treePath);
                for (File file : files) {
                    WzObject wzObject = targetWzFolder.addFile(file.toPath());
                    if (expanded) {
                        insertNodeToTree(node, wzObject, true);
                    }
                    MainFrame.getInstance().updateProgress(++count[0], total);
                }

                if (!expanded) {
                    tree.expandPath(new TreePath(node.getPath()));
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
    }

    private void attachWzDir(DefaultMutableTreeNode node, List<File> folders) {
        final int[] count = {0};
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                WzDirectory wzDir = (WzDirectory) node.getUserObject();
                WzFile wzFile = wzDir.getWzFile();
                String keyBoxName = wzFile.getKeyBoxName();
                byte[] iv = wzFile.getIv();
                byte[] key = wzFile.getKey();
                int total = folders.size();
                OverwriteChoice choice = null;
                MainFrame.getInstance().updateProgress(0, total);

                for (File folder : folders) {
                    for (Map.Entry<String, List<Path>> entry : FileTool.getAllFiles(folder.toPath()).entrySet()) {
                        String[] relativize = entry.getKey().split(Pattern.quote(File.separator));
                        WzDirectory curDir = wzDir;
                        for (String r : relativize) {
                            if (curDir.existDirectory(r)) {
                                curDir = curDir.getDirectory(r);
                            } else {
                                WzDirectory newDir = new WzDirectory(r, curDir, wzFile);
                                curDir.addChild(newDir);
                                curDir = newDir;
                            }
                        }

                        List<Path> files = entry.getValue();
                        total += files.size();
                        for (Path file : files) {
                            String filename = file.getFileName().toString();
                            boolean isXml = false;
                            if (filename.endsWith(".xml")) {
                                filename = filename.substring(0, filename.length() - 4);
                                isXml = true;
                            }
                            if (curDir.existImage(filename)) {
                                if (choice == null) choice = OverwriteDialog.show(EditPane.this, filename);

                                if (choice == OverwriteChoice.SKIP_ALL || choice == OverwriteChoice.SKIP) {
                                    if (choice == OverwriteChoice.SKIP) choice = null;

                                    MainFrame.getInstance().updateProgress(++count[0], total);
                                    continue;
                                } else if (choice == OverwriteChoice.OVERWRITE_ALL || choice == OverwriteChoice.OVERWRITE) {
                                    curDir.removeImageChild(filename);

                                    if (choice == OverwriteChoice.OVERWRITE) choice = null;
                                } else if (choice == OverwriteChoice.CANCEL) break;
                            }

                            String filePathStr = file.toString();
                            WzImage wzImage;
                            if (isXml) {
                                WzXmlFile wzXmlFile = new WzXmlFile(filename, filePathStr, keyBoxName, iv, key);
                                if (!wzXmlFile.parse()) {
                                    MainFrame.getInstance().setStatusText("无法解析 Xml, 停止导入。请查看相关日志: %s", filePathStr);
                                    return null;
                                }
                                wzImage = wzXmlFile.deepClone(curDir);
                            } else {
                                WzImageFile wzImageFile = new WzImageFile(filename, filePathStr, keyBoxName, iv, key);
                                if (!wzImageFile.parse()) {
                                    MainFrame.getInstance().setStatusText("无法解析 Img, 停止导入。请确认 Img 的密钥和导入对象的密钥是否一致: %s", filePathStr);
                                    return null;
                                }
                                wzImage = wzImageFile.deepClone(curDir);
                            }

                            wzImage.setReader(new BinaryReader(wzFile.getWzMutableKey()));
                            wzImage.setChildrenWzImage();
                            wzImage.setTempChanged(true);
                            curDir.addChild(wzImage);
                            MainFrame.getInstance().updateProgress(++count[0], total);
                        }
                        if (choice == OverwriteChoice.CANCEL) break;
                    }

                    MainFrame.getInstance().updateProgress(++count[0], total);
                    if (choice == OverwriteChoice.CANCEL) break;
                }

                node.removeAllChildren();
                treeModel.reload();
                handleTreeDoubleClick(node);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    MainFrame.getInstance().setStatusText("共导入 %d 个文件夹", count[0]);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }.execute();
    }

    // 剪贴板操作 --------------------------------------------------------------------------------------------------------
    public void doCopy() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) return;

        Clipboard clipboard = MainFrame.getInstance().getClipboard();
        clipboard.lock();
        clipboard.clear();
        List<String> names = new ArrayList<>();
        for (TreePath treePath : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            WzObject wzObject = (WzObject) node.getUserObject();
            clipboard.add(wzObject.deepClone(null));
            names.add(wzObject.getName());
        }
        clipboard.unlock();

        // 将复制的节点名称写入到系统剪贴板，以触发一些工具复制音效
        StringSelection selection = new StringSelection(String.join(",", names));
        java.awt.datatransfer.Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        systemClipboard.setContents(selection, null);
    }

    private void setPasteWzFileAndReader(List<WzObject> items, WzFile wzFile) {
        for (WzObject item : items) {
            if (item instanceof WzDirectory dir) {
                dir.setWzFile(wzFile);
                setPasteWzFileAndReader(dir.getChildren(), wzFile);
            } else if (item instanceof WzImage img) {
                img.setReader(wzFile.getReader());
                setPasteWzImage(img.getChildren(), img);
            } else {
                MainFrame.getInstance().getClipboard().unlock();
                throw new RuntimeException("无法给类型 " + item.getClass().getSimpleName() + " 设置 WzFile");
            }
        }
    }

    private void setPasteWzImage(List<? extends WzObject> items, WzImage image) {
        for (WzObject item : items) {
            if (item instanceof WzImageProperty prop) {
                prop.setWzImage(image);
                prop.setChildrenWzImage(image);
            } else {
                MainFrame.getInstance().getClipboard().unlock();
                throw new RuntimeException("无法给类型 " + item.getClass().getSimpleName() + " 设置 WzImage");
            }
        }
    }

    private boolean isWzObjExistChild(WzObject parent, WzObject child) {
        switch (parent) {
            case WzDirectory pDir when child instanceof WzDirectory -> {
                return pDir.existDirectory(child.getName());
            }
            case WzDirectory pDir when child instanceof WzImage -> {
                return pDir.existImage(child.getName());
            }
            case WzImage image -> {
                return image.existChild(child.getName());
            }
            case WzImageProperty property when property.isListProperty() -> {
                return property.existChild(child.getName());
            }
            default -> {
                MainFrame.getInstance().getClipboard().unlock();
                throw new RuntimeException("isExistChild 未支持的类型" + parent.getClass().getSimpleName());
            }
        }
    }

    private void removeWzObjChild(WzObject parent, WzObject child) {
        switch (parent) {
            case WzDirectory pDir when child instanceof WzDirectory -> pDir.removeDirectoryChild(child.getName());
            case WzDirectory pDir when child instanceof WzImage -> pDir.removeImageChild(child.getName());
            case WzImage image -> image.removeChild(child.getName());
            case WzImageProperty property when property.isListProperty() -> property.removeChild(child.getName());
            default -> {
                MainFrame.getInstance().getClipboard().unlock();
                throw new RuntimeException("removeWzObjChild 未支持的类型" + parent.getClass().getSimpleName());
            }
        }
    }

    private void addWzObjChild(WzObject parent, WzObject child) {
        switch (parent) {
            case WzDirectory pDir when child instanceof WzDirectory cDir -> pDir.addChild(cDir);
            case WzDirectory pDir when child instanceof WzImage cImg -> pDir.addChild(cImg);
            case WzImage image when child instanceof WzImageProperty property -> image.addChild(property);
            case WzImageProperty pProp when pProp.isListProperty() && child instanceof WzImageProperty cProp ->
                    pProp.addChild(cProp);
            default -> {
                MainFrame.getInstance().getClipboard().unlock();
                throw new RuntimeException("addWzObjChild 未支持的组合 " + parent.getClass().getSimpleName() + " 和 " + child.getClass().getSimpleName());
            }
        }
    }

    public void doPaste() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (TreePathUtil.isNullOrMultiple(selectedPaths)) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
        WzObject to = (WzObject) node.getUserObject();

        Clipboard clipboard = MainFrame.getInstance().getClipboard();
        clipboard.lock();

        if (clipboard.isEmpty()) {
            JMessageUtil.error("剪贴板是空的");
            clipboard.unlock();
            return;
        }

        if (clipboard.canPaste(to)) {
            List<WzObject> cpItems = clipboard.getItems();
            cpItems.forEach(cpItem -> cpItem.setParent(to)); // 复制的时候顶级对象没有 parent

            // 这里的对象类型和canPaste保持一致
            if (to instanceof WzDirectory wzDirectory) {
                setPasteWzFileAndReader(cpItems, wzDirectory.getWzFile());
            } else if (to instanceof WzImage wzImg) {
                setPasteWzImage(cpItems, wzImg);
            } else if (to instanceof WzImageProperty wzProp && wzProp.isListProperty()) {
                setPasteWzImage(cpItems, wzProp.getWzImage());
            }

            OverwriteChoice choice = null;
            for (WzObject item : cpItems) {
                item.setTempChanged(true);
                int index = 0;
                if (isWzObjExistChild(to, item)) { // 发现重名
                    if (choice == OverwriteChoice.SKIP_ALL) continue;
                    else if (choice == OverwriteChoice.OVERWRITE_ALL) {
                        removeWzObjChild(to, item);
                        DefaultMutableTreeNode childNode = findTreeNodeByName(node, item.getName());
                        index = node.getIndex(childNode);
                        removeNodeFromTree(childNode);
                    } else {
                        choice = OverwriteDialog.show(this, item.getName());
                        switch (choice) {
                            case OVERWRITE, OVERWRITE_ALL -> {
                                removeWzObjChild(to, item);
                                DefaultMutableTreeNode childNode = findTreeNodeByName(node, item.getName());
                                index = node.getIndex(childNode);
                                removeNodeFromTree(childNode);
                            }
                            case SKIP, SKIP_ALL, CANCEL -> {
                                continue;
                            }
                        }
                    }
                }
                addWzObjChild(to, item); // 已经设置 changed 了

                insertNodeToTree(node, item, true, index);
            }
        } else {
            JMessageUtil.error("复制的东西不能粘贴到 " + to.getClass().getSimpleName());
            clipboard.unlock();
            return;
        }

        resetValueForm();
        clipboard.unlock();
    }

    // 新建文件 ----------------------------------------------------------------------------------------------------------
    public void createWz() {
        CreateFileDialog dialog = new CreateFileDialog(this, true);
        CreateFileData data = dialog.getData();
        if (data == null) return;

        String name = data.getName();
        if (!name.endsWith(".wz")) name = name + ".wz";
        short fileVer = data.getVersion();
        WzKey wzKey = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();
        if (wzKey == null) return;

        WzFile wzFile = WzFile.createNewFile(name, fileVer, wzKey.getName(), wzKey.getIv(), wzKey.getUserKey());
        wzFile.setNewFile(true);
        wzFile.getWzDirectory().setTempChanged(true);
        insertNodeToTree(treeRoot, wzFile.getWzDirectory(), true);
    }

    public void createImg() {
        CreateFileDialog dialog = new CreateFileDialog(this, false);
        CreateFileData data = dialog.getData();
        if (data == null) return;

        String name = data.getName();
        if (!name.endsWith(".img")) name = name + ".img";
        WzKey wzKey = (WzKey) MainFrame.getInstance().getKeyBox().getSelectedItem();
        if (wzKey == null) return;

        WzImageFile wzImageFile = new WzImageFile(name, name, wzKey.getName(), wzKey.getIv(), wzKey.getUserKey());
        wzImageFile.setReader(new BinaryReader(wzImageFile.getIv(), wzImageFile.getKey()));
        wzImageFile.setNewFile(true);
        wzImageFile.setStatus(WzFileStatus.PARSE_SUCCESS);
        wzImageFile.setChanged(true);
        wzImageFile.setTempChanged(true);
        insertNodeToTree(treeRoot, wzImageFile, true);
    }

    // 汉化 -------------------------------------------------------------------------------------------------------------
    public void compareImg() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    ChineseUtil.initChineseImg();
                    for (TreePath treePath : selectedPaths) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                        WzObject to = (WzObject) node.getUserObject();

                        WzObject from = MainFrame.getInstance().getCenterPane().getAnotherPane(EditPane.this).findWzObjectInTreeByPath(to.getPath());
                        if (from == null) {
                            log.error("找不到中文版本的 {}", to.getName());
                            continue;
                        }

                        ChineseUtil.chineseImg(from, to);
                    }

                    ChineseUtil.completeChineseImg();
                    return null;
                } catch (Exception e) {
                    log.error(e.getMessage());
                    return null;
                }
            }
        }.execute();
    }

    // 批量修改图片格式 ---------------------------------------------------------------------------------------------------
    public void changeCavFmt() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) return;

        ChangeCavFmtDialog dialog = new ChangeCavFmtDialog(this);
        CanvasFormData data = dialog.getData();
        if (data == null) return;
        WzPngFormat format = data.getFormat();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                List<WzImageProperty> properties = new ArrayList<>();
                for (TreePath treePath : selectedPaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    WzImageProperty prop = (WzImageProperty) node.getUserObject();
                    properties.add(prop);
                }
                CanvasUtil.changeFormat(properties, format);
                MainFrame.getInstance().setStatusText("修改完成");
                return null;
            }
        }.execute();
    }

    // 批量缩放图片 ------------------------------------------------------------------------------------------------------
    public void scaleImage() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) return;

        ScaleDialog dialog = new ScaleDialog(this);
        DoubleFormData data = dialog.getData();
        if (data == null) return;
        double scale = data.getValue();
        if (scale == 1.0) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                List<WzImageProperty> properties = new ArrayList<>();
                for (TreePath treePath : selectedPaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    WzImageProperty prop = (WzImageProperty) node.getUserObject();
                    properties.add(prop);
                }
                CanvasUtil.scaleImage(properties, scale);
                MainFrame.getInstance().setStatusText("修改完成");
                return null;
            }
        }.execute();
    }

    public void removeAllWzChildWithName() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) return;

        NodeDialog dialog = new NodeDialog("批量删除节点", "节点名称", this);
        NodeFormData data = dialog.getData();
        if (data == null) return;

        String name = data.getName();
        int count = 0;
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            WzObject wzObj = (WzObject) node.getUserObject();
            int current = 0;
            if (wzObj instanceof WzImage image) {
                if (!image.parse()) {
                    MainFrame.getInstance().setStatusText("由于 %s 解析失败，操作中断，已经删除了 %d 个节点", image.getName(), count);
                    return;
                }
                current = image.removeAllChildWithName(name);
            } else if (wzObj instanceof WzImageProperty prop) {
                current = prop.removeAllChildWithName(name);
            }

            if (current > 0) {
                DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) node.getParent();
                int index = pNode.getIndex(node);
                removeNodeFromTree(node);
                insertNodeToTree(pNode, wzObj, true, index);
                count += current;
            }
        }

        MainFrame.getInstance().setStatusText("总共删除了 %d 个节点", count);
    }

    public void removeNonCashEqp() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null) return;

        int cashCount = 0;
        int delCount = 0;
        int notfoundCount = 0;
        int size = 0;

        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            WzDirectory wzDir = (WzDirectory) node.getUserObject();
            List<WzImage> images = wzDir.getImages();
            size += images.size();
            for (WzImage image : images) {
                image.parse();
                int cashValue = 0;
                WzImageProperty info = image.getChild("info");

                if (info != null) {
                    WzImageProperty cash = info.getChild("cash");

                    switch (cash) {
                        case WzIntProperty c -> cashValue = c.getValue();
                        case WzStringProperty c -> cashValue = Integer.parseInt(c.getValue());
                        case null -> log.warn("{} 找不到 cash 节点视作非现金装备已被移除", info.getPath());
                        default ->
                                log.warn("{} cash 节点不是 Int 也不是 String 视作非现金装备已被移除", info.getPath());
                    }
                } else {
                    log.warn("{} 找不到 info 节点视作非现金装备已被移除", image.getPath());
                    notfoundCount++;
                }

                if (cashValue == 0) {
                    wzDir.removeImageChild(image.getName());
                    delCount++;
                    continue;
                }

                cashCount++;
            }

            node.removeAllChildren();
            tree.repaint();
            handleTreeDoubleClick(node);
        }

        log.info("处理完毕: 共 {} 个装备, 现金装备 {} , 非现金装备 {}, 找不到 info 节点 {} 个", size, cashCount, delCount, notfoundCount);
    }
}
