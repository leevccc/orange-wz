package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.panel.ImagePanel;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.properties.WzCanvasProperty;
import orange.wz.provider.properties.WzPngFormat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public final class ImageCompareDialog extends JDialog {

    private JList<String> stringList;
    private DefaultListModel<String> listModel;
    private final List<String> collections = new ArrayList<>();
    private ImagePanel imagePanel1;
    private ImagePanel imagePanel2;
    private JLabel widthLabel1;
    private JLabel widthLabel2;
    private JLabel heightLabel1;
    private JLabel heightLabel2;
    private JLabel formatLabel1;
    private JLabel formatLabel2;
    private JLabel scaleLabel1;
    private JLabel scaleLabel2;

    private JCheckBox includeChildren;
    private JLabel statusLabel;
    private final Map<String, WzCanvasProperty> toMap = new HashMap<>();
    private final Map<String, WzCanvasProperty> fromMap = new HashMap<>();
    private WzCanvasProperty curTo;
    private WzCanvasProperty curFrom;

    private final Set<Integer> changedList = new HashSet<>();

    public ImageCompareDialog(Frame owner) {
        super(owner, MainFrame.i18n.get("test.temp0118"), false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(buildMainPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        bindKeys();

        // --- 添加窗口关闭监听 ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onDialogClosing();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                onDialogClosing();
            }
        });

        setVisible(true);
    }

    private JComponent buildMainPanel() {
        JScrollPane left = buildStringListPanel();
        JPanel center = buildImageInfoPanel(MainFrame.i18n.get("test.temp0119"), true);
        JPanel right = buildImageInfoPanel(MainFrame.i18n.get("test.temp0120"), false);

        // 中 + 右 先合成一个面板
        JPanel rightGroup = new JPanel(new GridLayout(1, 2, 10, 10));
        rightGroup.add(center);
        rightGroup.add(right);

        // 左 + (中右) 用 JSplitPane 控制比例
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, rightGroup);
        split.setResizeWeight(0.2);           // 左侧占 20%
        split.setDividerLocation(220);        // 初始宽度 220px
        split.setOneTouchExpandable(false);

        return split;
    }

    /**
     * 左侧字符串列表
     */
    private JScrollPane buildStringListPanel() {
        listModel = new DefaultListModel<>();
        stringList = new JList<>(listModel);
        stringList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 超出宽度用 ... 显示
        stringList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                // 默认文本颜色
                lbl.setForeground(Color.BLACK);

                // 如果被标记了特殊颜色
                if (changedList.contains(index)) {
                    lbl.setForeground(Color.MAGENTA);
                }

                // 文本省略显示
                lbl.setToolTipText(value.toString());
                lbl.setText(ellipsis(value.toString(), list.getWidth() - 20, lbl.getFontMetrics(lbl.getFont())));

                return lbl;
            }
        });

        stringList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int index = stringList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String sel = stringList.getModel().getElementAt(index);
                        onStringSelected(sel);
                    }
                }
            }
        });

        stringList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // 只触发一次
                String sel = stringList.getSelectedValue();
                if (sel != null) {
                    onStringSelected(sel);
                }
            }
        });


        // 让 JList 也支持快捷键
        InputMap im = stringList.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = stringList.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "replace");
        am.put("replace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                replaceImage();
            }
        });


        return new JScrollPane(stringList);
    }

    /**
     * 中 / 右 图片 + 参数
     */
    private JPanel buildImageInfoPanel(String title, boolean first) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        // 图片
        ImagePanel imageLabel = new ImagePanel();
        imageLabel.setPreferredSize(new Dimension(250, 250));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 参数信息（横向排列）
        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        info.setBorder(new EmptyBorder(5, 8, 5, 8));
        JLabel widthLabel = new JLabel("Width: ");
        JLabel heightLabel = new JLabel("Height: ");
        JLabel formatLabel = new JLabel("Format: ");
        JLabel scaleLabel = new JLabel("Scale: ");
        info.add(widthLabel);
        info.add(heightLabel);
        info.add(formatLabel);
        info.add(scaleLabel);

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(info, BorderLayout.SOUTH);

        if (first) {
            imagePanel1 = imageLabel;
            widthLabel1 = widthLabel;
            heightLabel1 = heightLabel;
            formatLabel1 = formatLabel;
            scaleLabel1 = scaleLabel;
        } else {
            imagePanel2 = imageLabel;
            widthLabel2 = widthLabel;
            heightLabel2 = heightLabel;
            formatLabel2 = formatLabel;
            scaleLabel2 = scaleLabel;
        }

        return panel;
    }

    /**
     * 底部按钮
     */
    private JPanel buildBottomPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());

        // 按钮区
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 8));
        includeChildren = new JCheckBox(MainFrame.i18n.get("test.temp0121"));
        includeChildren.setSelected(true);
        JButton replaceBtn = new JButton(MainFrame.i18n.get("test.temp0122"));

        replaceBtn.addActionListener(e -> replaceImage());

        buttons.add(includeChildren);
        buttons.add(replaceBtn);

        // 状态栏
        statusLabel = new JLabel(MainFrame.i18n.get("test.temp0123"));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));

        wrapper.add(buttons, BorderLayout.NORTH);
        wrapper.add(statusLabel, BorderLayout.SOUTH);

        return wrapper;
    }


    /**
     * 绑定快捷键
     */
    private void bindKeys() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "replace");
        am.put("replace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                replaceImage();
            }
        });
    }

    private void replaceImage() {
        BufferedImage image = curFrom.getPngImage(false);
        WzPngFormat format = curFrom.getFormat();
        int scale = curFrom.getScale();
        curTo.setPng(image, format, scale);
        imagePanel1.setImage(image);

        if (includeChildren.isSelected()) {
            List<WzImageProperty> children = new ArrayList<>();
            curFrom.getChildren().forEach(child -> children.add(child.deepClone(curTo)));
            curTo.replaceChildrenList(children);
        }

        int index = stringList.getSelectedIndex();
        changedList.add(index);
        stringList.setSelectedIndex(index + 1);

        setStatus(MainFrame.i18n.get("test.temp0124", changedList.size(), listModel.getSize()));
    }

    /**
     * 文本省略显示
     */
    private String ellipsis(String text, int maxWidth, FontMetrics fm) {
        if (fm.stringWidth(text) <= maxWidth) return text;
        String dots = "...";
        int w = fm.stringWidth(dots);
        int i = text.length() - 1;
        while (i > 0 && fm.stringWidth(text.substring(0, i)) + w > maxWidth) {
            i--;
        }
        return text.substring(0, i) + dots;
    }

    private void onStringSelected(String value) {
        curTo = toMap.get(value);
        imagePanel1.setImage(curTo.getPngImage(false));
        widthLabel1.setText("Width: " + curTo.getWidth());
        heightLabel1.setText("Height: " + curTo.getHeight());
        formatLabel1.setText("Format: " + curTo.getFormat().toString());
        scaleLabel1.setText("Scale: " + curTo.getScale());

        curFrom = fromMap.get(value);
        imagePanel2.setImage(curFrom.getPngImage(false));
        widthLabel2.setText("Width: " + curFrom.getWidth());
        heightLabel2.setText("Height: " + curFrom.getHeight());
        formatLabel2.setText("Format: " + curFrom.getFormat().toString());
        scaleLabel2.setText("Scale: " + curFrom.getScale());
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public synchronized void addCompare(WzCanvasProperty to, WzCanvasProperty from) {
        String path = to.getPath();
        // listModel.addElement(path);
        collections.add(path);
        toMap.put(path, to);
        fromMap.put(path, from);
        setStatus(MainFrame.i18n.get("test.temp0124", changedList.size(), listModel.getSize()));

        if (collections.size() > 25) {
            boolean selFirst = listModel.getSize() == 0;

            listModel.addAll(collections);
            collections.clear();

            if (selFirst) { // 选中第一个元素并触发选中事件
                stringList.setSelectedIndex(0);
                stringList.ensureIndexIsVisible(0);
                onStringSelected(path);
            }
        }
    }

    public void completeScan() {
        listModel.addAll(collections);
        collections.clear();
        if (listModel.getSize() == 0) setStatus(MainFrame.i18n.get("test.temp0125"));
    }

    /**
     * 在对话框关闭时清理数据
     */
    private void onDialogClosing() {
        listModel.clear();
        toMap.clear();
        fromMap.clear();
        changedList.clear();
        curTo = null;
        curFrom = null;
    }

}
