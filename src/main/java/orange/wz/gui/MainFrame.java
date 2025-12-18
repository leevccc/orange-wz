package orange.wz.gui;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.form.impl.*;
import orange.wz.gui.menu.WzFileMenu;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.model.WzKey;
import orange.wz.model.WzKeyStorage;
import orange.wz.provider.*;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.WzType;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Getter
public class MainFrame extends JFrame {
    private final WzKeyStorage wzKeyStorage = new WzKeyStorage();
    private static MainFrame instance;

    private JTree tree;
    private DefaultMutableTreeNode treeRoot;

    private JPanel formCards;
    private WzObject curWzObject;
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

    private JProgressBar progressBar;
    private JLabel statusLabel;

    private static final FlatSVGIcon fcFolderIcon = getSVG("FcFolder.svg", 16, 16);
    private static final FlatSVGIcon fcFolderBlueIcon = getSVG("FcFolder.svg", 16, 16, Color.BLUE);
    private static final FlatSVGIcon fcFileIcon = getSVG("FcFile.svg", 16, 16);
    private static final FlatSVGIcon aiOutlineFileWordIcon = getSVG("AiOutlineFileWord.svg", 16, 16);
    private static final FlatSVGIcon aiOutlineFileMarkdownIcon = getSVG("AiOutlineFileMarkdown.svg", 16, 16);
    private static final FlatSVGIcon imgIcon = getSVG("IMG.svg", 16, 16);
    private static final FlatSVGIcon listIcon = getSVG("LIST.svg", 16, 16);
    private static final FlatSVGIcon strIcon = getSVG("STR.svg", 16, 16);
    private static final FlatSVGIcon pngIcon = getSVG("PNG.svg", 16, 16);
    private static final FlatSVGIcon intIcon = getSVG("INT.svg", 16, 16);
    private static final FlatSVGIcon doubleIcon = getSVG("DOUBLE.svg", 16, 16);
    private static final FlatSVGIcon floatIcon = getSVG("FLOAT.svg", 16, 16);
    private static final FlatSVGIcon longIcon = getSVG("LONG.svg", 16, 16);
    private static final FlatSVGIcon nullIcon = getSVG("NULL.svg", 16, 16);
    private static final FlatSVGIcon rawIcon = getSVG("RAW.svg", 16, 16);
    private static final FlatSVGIcon shortIcon = getSVG("SHORT.svg", 16, 16);
    private static final FlatSVGIcon wavIcon = getSVG("WAV.svg", 16, 16);
    private static final FlatSVGIcon uolIcon = getSVG("UOL.svg", 16, 16);
    private static final FlatSVGIcon convexIcon = getSVG("CONVEX.svg", 16, 16);
    private static final FlatSVGIcon vectorIcon = getSVG("VECTOR.svg", 16, 16);

    public static MainFrame getInstance() {
        if (instance == null) {
            instance = new MainFrame();
        }
        return instance;
    }

    public MainFrame() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {
        }
        setTitle("OrzRepacker");
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawPanel();
    }

    private void drawPanel() {
        setJMenuBar(createMenuBar());

        JScrollPane treePanel = createTreePanel();
        treePanel.setMinimumSize(new Dimension(260, 0)); // 宽度最小 260，高度不限

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, createMainPanel());
        splitPane.setDividerLocation(260);
        // splitPane.setOneTouchExpandable(true); // 增加小箭头快速展开/收起
        add(splitPane, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");

        JMenu openItem = new JMenu("打开");
        openItem.setIcon(fcFolderIcon);

        JMenuItem openFiles = new JMenuItem("文件 wz/img", fcFileIcon);
        openFiles.addActionListener(e -> {
            List<File> files = FileDialog.chooseOpenFiles(new String[]{"wz", "img"});
            open(files);
        });
        openItem.add(openFiles);

        JMenuItem openFolders = new JMenuItem("文件夹...", fcFolderIcon);
        openFolders.addActionListener(e -> {
            List<File> files = FileDialog.chooseOpenFolders();
            open(files);
        });
        openItem.add(openFolders);

        fileMenu.add(openItem);
        menuBar.add(fileMenu);

        return menuBar;
    }

    private JScrollPane createTreePanel() {
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
                        case FOLDER -> fcFolderIcon;
                        case DIRECTORY -> {
                            if (((WzDirectory) obj).isWzFile()) {
                                yield aiOutlineFileWordIcon;
                            } else {
                                yield fcFolderBlueIcon;
                            }
                        }
                        case IMAGE -> {
                            if (obj instanceof WzImageFile) {
                                yield aiOutlineFileMarkdownIcon;
                            } else {
                                yield imgIcon;
                            }
                        }
                        case CANVAS_PROPERTY -> pngIcon;
                        case CONVEX_PROPERTY -> convexIcon;
                        case DOUBLE_PROPERTY -> doubleIcon;
                        case FLOAT_PROPERTY -> floatIcon;
                        case INT_PROPERTY -> intIcon;
                        case LIST_PROPERTY -> listIcon;
                        case LONG_PROPERTY -> longIcon;
                        case NULL_PROPERTY -> nullIcon;
                        case RAW_DATA_PROPERTY -> rawIcon;
                        case SHORT_PROPERTY -> shortIcon;
                        case SOUND_PROPERTY -> wavIcon;
                        case STRING_PROPERTY -> strIcon;
                        case UOL_PROPERTY -> uolIcon;
                        case VECTOR_PROPERTY -> vectorIcon;
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
        JPopupMenu wzFilePopupMenu = WzFileMenu.create();
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

    private JPanel createMainPanel() {
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
        formCards.add(nodeForm.getPanel(), "node");
        formCards.add(canvasForm.getPanel(), "canvas");
        formCards.add(doubleForm.getPanel(), "double");
        formCards.add(floatForm.getPanel(), "float");
        formCards.add(intForm.getPanel(), "int");
        formCards.add(longForm.getPanel(), "long");
        formCards.add(shortForm.getPanel(), "short");
        formCards.add(soundForm.getPanel(), "sound");
        formCards.add(stringForm.getPanel(), "string");
        formCards.add(uolCanvasForm.getPanel(), "uolCanvas");
        formCards.add(uolSoundForm.getPanel(), "uolSound");
        formCards.add(vectorForm.getPanel(), "vector");

        // 默认显示 node 表单
        ((CardLayout) formCards.getLayout()).show(formCards, "node");

        return formCards;
    }

    private void switchForm(String formName) {
        CardLayout cl = (CardLayout) (formCards.getLayout());
        cl.show(formCards, formName);
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        // 进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusBar.add(progressBar, BorderLayout.WEST);

        // 状态文字
        statusLabel = new JLabel("准备就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        statusBar.add(statusLabel, BorderLayout.CENTER);

        // 状态文字
        JLabel versionLabel = new JLabel("OrzRepacker v1.0.0");
        versionLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        statusBar.add(versionLabel, BorderLayout.EAST);

        return statusBar;
    }

    public void open(List<File> files) {
        WzKey key = wzKeyStorage.findByName("GMS");

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

    private void handleTreeClick(WzObject wzObject) {
        WzObject temp = curWzObject;
        curWzObject = wzObject;
        switch (wzObject) {
            case WzFolder obj -> {
                nodeForm.setData(obj.getName(), WzType.FOLDER.name());
                switchForm("node");
            }
            case WzDirectory obj -> {
                if (obj.isWzFile()) {
                    nodeForm.setData(obj.getName(), WzType.WZ_FILE.name());
                } else {
                    nodeForm.setData(obj.getName(), WzType.DIRECTORY.name());
                }
                switchForm("node");
            }
            case WzImage obj -> {
                nodeForm.setData(obj.getName(), WzType.IMAGE.name());
                switchForm("node");
            }
            case WzCanvasProperty obj -> {
                canvasForm.setData(obj.getName(), WzType.CANVAS_PROPERTY.name(), obj.getPngImage(), obj.getWidth(), obj.getHeight(), obj.getPngFormat());
                switchForm("canvas");
            }
            case WzConvexProperty obj -> {
                nodeForm.setData(obj.getName(), WzType.CONVEX_PROPERTY.name());
                switchForm("node");
            }
            case WzDoubleProperty obj -> {
                doubleForm.setData(obj.getName(), WzType.DOUBLE_PROPERTY.name(), obj.getValue());
                switchForm("double");
            }
            case WzFloatProperty obj -> {
                floatForm.setData(obj.getName(), WzType.FLOAT_PROPERTY.name(), obj.getValue());
                switchForm("float");
            }
            case WzIntProperty obj -> {
                intForm.setData(obj.getName(), WzType.INT_PROPERTY.name(), obj.getValue());
                switchForm("int");
            }
            case WzListProperty obj -> {
                nodeForm.setData(obj.getName(), WzType.LIST_PROPERTY.name());
                switchForm("node");
            }
            case WzLongProperty obj -> {
                longForm.setData(obj.getName(), WzType.LONG_PROPERTY.name(), obj.getValue());
                switchForm("long");
            }
            case WzNullProperty obj -> {
                nodeForm.setData(obj.getName(), WzType.NULL_PROPERTY.name());
                switchForm("node");
            }
            case WzShortProperty obj -> {
                shortForm.setData(obj.getName(), WzType.SHORT_PROPERTY.name(), obj.getValue());
                switchForm("short");
            }
            case WzSoundProperty obj -> {
                soundForm.setData(obj.getName(), WzType.SOUND_PROPERTY.name(), obj.getFileBytes());
                switchForm("sound");
            }
            case WzStringProperty obj -> {
                stringForm.setData(obj.getName(), WzType.STRING_PROPERTY.name(), obj.getValue());
                switchForm("string");
            }
            case WzUOLProperty obj -> {
                WzObject target = obj.getUolTarget();
                if (target instanceof WzCanvasProperty cav) {
                    uolCanvasForm.setData(obj.getName(), WzType.UOL_PROPERTY.name(), obj.getValue(), cav);
                    switchForm("uolCanvas");
                } else if (target instanceof WzSoundProperty sound) {
                    uolSoundForm.setData(obj.getName(), WzType.UOL_PROPERTY.name(), obj.getValue(), sound);
                    switchForm("uolSound");
                }
            }
            case WzVectorProperty obj -> {
                vectorForm.setData(obj.getName(), WzType.VECTOR_PROPERTY.name(), obj.getX(), obj.getY());
                switchForm("vector");
            }
            default -> {
                curWzObject = temp;
                setStatusText("%s 未知的节点类型 %s", wzObject.getName(), wzObject.getClass().getSimpleName());
                return;
            }
        }

        setStatusText(wzObject.getPath());
    }

    private void handleTreeDoubleClick(DefaultMutableTreeNode node, WzObject wzObject) {
        setStatusText("加载 %s...", wzObject.getName());

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                switch (wzObject) {
                    case WzFolder folder -> {
                        if (node.getChildCount() == 0) {
                            List<WzObject> children = folder.getChildren();
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
                            List<WzImageProperty> children = wzImg.getChildren();
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
                    setStatusText("%s 加载完毕", wzObject.getName());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }.execute();
    }

    // 递归方法：只插入子节点，不展开
    private void addChildrenRecursively(DefaultMutableTreeNode parentNode, WzObject wzObject, boolean expand) {
        if (parentNode.getChildCount() > 0) return;

        List<? extends WzObject> children = null;
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

    /**
     * 更新进度条
     *
     * @param current 当前进度
     * @param total   总量
     */
    public void updateProgress(int current, int total) {
        int percent = (int) ((double) current / total * 100);
        progressBar.setValue(percent);
        progressBar.setString(current + "/" + total);
    }

    public void setStatusText(String format, Object... args) {
        if (statusLabel != null) {
            statusLabel.setText(String.format(format, args));
        }
    }

    public static Image loadImage(String name) {
        try (InputStream in = MainFrame.class.getResourceAsStream("/" + name)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FlatSVGIcon getSVG(String filename, int width, int height) {
        return getSVG(filename, width, height, null);
    }

    public static FlatSVGIcon getSVG(String filename, int width, int height, Color color) {
        FlatSVGIcon svg = new FlatSVGIcon("icons/" + filename, width, height);
        if (color != null) {
            svg.setColorFilter(new FlatSVGIcon.ColorFilter() {
                @Override
                public Color filter(Color color) {
                    return new Color(0x2196F3);
                }
            });
        }

        return svg;
    }

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
}
