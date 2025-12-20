package orange.wz.gui.key;

import orange.wz.gui.filter.HexFilter;
import orange.wz.gui.MainFrame;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.utils.wzkey.WzKey;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static orange.wz.provider.WzAESConstant.DEFAULT_KEY;

public final class KeyManager extends JDialog {
    private KeyBox keyBox;
    private final JTextField[] ivFields = new JTextField[4];
    private final int rows = 8;
    private final int cols = 4;
    private final JTextField[][] userKeyFields = new JTextField[rows][cols];

    private final JButton btnNew = new JButton("新建方案");
    private final JButton btnDelete = new JButton("删除方案");
    private final JButton btnRename = new JButton("重命名");
    private final JButton btnReset = new JButton("重置");
    private final JButton btnSave = new JButton("保存");

    public KeyManager(Window owner) {
        super(owner, "密钥管理器", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(350, 520);
        setLocationRelativeTo(owner);

        setLayout(new BorderLayout(8, 8));
        add(createMainPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        keyBox.setSelectedIndex(0);
        keyBoxChanged((WzKey) Objects.requireNonNull(keyBox.getSelectedItem()));
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createNamePanel());
        panel.add(Box.createVerticalStrut(8));
        panel.add(createIvPanel());
        panel.add(Box.createVerticalStrut(8));
        panel.add(createUserKeyPanel());

        return panel;
    }

    // ---------- Name ----------
    private JPanel createNamePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new TitledBorder("方案"));

        WzKey[] wzKeys = MainFrame.getInstance().getWzKeyStorage().loadAll().toArray(new WzKey[0]);
        keyBox = new KeyBox(wzKeys);
        keyBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                keyBoxChanged((WzKey) e.getItem());
            }
        });

        panel.add(keyBox, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.add(btnNew);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRename);

        btnNew.addActionListener(e -> {
            while (true) {
                String input = JOptionPane.showInputDialog("方案名称：");

                if (input == null) break; // 用户取消

                input = input.trim();
                if (input.isEmpty()) {
                    JMessageUtil.error("请输入方案名称");
                    continue; // 回到输入框
                }

                if (addClick(input)) break;
            }
        });
        btnDelete.addActionListener(e -> {
            WzKey wzKey = (WzKey) Objects.requireNonNull(keyBox.getSelectedItem());
            removeClick(wzKey.getId());
        });
        btnRename.addActionListener(e -> {
            WzKey wzKey = (WzKey) Objects.requireNonNull(keyBox.getSelectedItem());
            while (true) {
                String input = JOptionPane.showInputDialog("方案名称：");

                if (input == null) break; // 用户取消

                input = input.trim();
                if (input.isEmpty()) {
                    JMessageUtil.error("请输入方案名称");
                    continue; // 回到输入框
                }

                if (renameClick(wzKey.getId(), input)) break;
            }
        });

        panel.add(btnPanel, BorderLayout.SOUTH);

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ---------- AES IV ----------
    private JPanel createIvPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 8, 4));
        panel.setBorder(new TitledBorder("IV"));

        for (int i = 0; i < 4; i++) {
            ivFields[i] = createHexField();
            panel.add(ivFields[i]);
        }
        return panel;
    }

    // ---------- AES User Key ----------
    private JPanel createUserKeyPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new TitledBorder(
                "User Key (128 字节, 16 列 x 8 行, 每行设置4列有效字节)"
        ));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(createUserKeyGrid(), BorderLayout.CENTER);
        center.add(btnReset, BorderLayout.SOUTH);
        btnReset.addActionListener(e -> {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 4; c++) {
                    userKeyFields[r][c].setText(toHex(DEFAULT_KEY[r * 8 + c * 4]));
                }
            }
        });

        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createUserKeyGrid() {
        JPanel grid = new JPanel(new GridLayout(8, 4, 6, 6));
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 4; c++) {
                userKeyFields[r][c] = createHexField();
                grid.add(userKeyFields[r][c]);
            }
        }
        return grid;
    }

    // ---------- Bottom ----------
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnSave.addActionListener(e -> {
            saveClick();
        });
        panel.add(btnSave);
        return panel;
    }

    // ---------- Utils ----------
    private JTextField createHexField() {
        JTextField field = new JTextField("00");
        field.setColumns(2);
        field.setHorizontalAlignment(JTextField.CENTER);

        // 只允许 0-9A-F 2个数字
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new HexFilter());
        return field;
    }

    private static String toHex(byte b) {
        return String.format("%02X", b & 0xFF);
    }

    private static byte fromHex(String hex) {
        if (!hex.matches("[0-9A-Fa-f]{2}")) {
            throw new IllegalArgumentException("非法字节: " + hex);
        }
        return (byte) Integer.parseInt(hex, 16);
    }

    private void keyBoxChanged(WzKey wzKey) {
        boolean editable = wzKey.getId() > 3;
        btnDelete.setEnabled(editable);
        btnRename.setEnabled(editable);
        btnReset.setEnabled(editable);
        btnSave.setEnabled(editable);

        byte[] iv = wzKey.getIv();
        for (int i = 0; i < 4; i++) {
            ivFields[i].setText(toHex(iv[i]));
            ivFields[i].setEditable(editable);
        }
        byte[] userKey = wzKey.getUserKey();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 4; c++) {
                userKeyFields[r][c].setText(toHex(userKey[r * 8 + c * 4]));
                userKeyFields[r][c].setEditable(editable);
            }
        }
    }

    private boolean addClick(String name) {
        WzKey wzKey = MainFrame.getInstance().getWzKeyStorage().addWzKey(name);
        if (wzKey == null) {
            JMessageUtil.error("已存在同名方案");
            return false;
        }

        DefaultComboBoxModel<WzKey> model = (DefaultComboBoxModel<WzKey>) keyBox.getModel();
        model.addElement(wzKey);
        model = (DefaultComboBoxModel<WzKey>) MainFrame.getInstance().getKeyBox().getModel();
        model.addElement(wzKey);
        return true;
    }

    private boolean renameClick(int id, String name) {
        if (!MainFrame.getInstance().getWzKeyStorage().renameById(id, name)) {
            JMessageUtil.error("新名称已被使用或者要改名的方案不存在");
            return false;
        }

        int itemCount = keyBox.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            WzKey key = keyBox.getItemAt(i);
            if (key.getId() == id) {
                key.setName(name);
                break;
            }
        }
        keyBox.repaint();
        itemCount = MainFrame.getInstance().getKeyBox().getItemCount();
        for (int i = 0; i < itemCount; i++) {
            WzKey key = MainFrame.getInstance().getKeyBox().getItemAt(i);
            if (key.getId() == id) {
                key.setName(name);
                break;
            }
        }
        MainFrame.getInstance().getKeyBox().repaint();

        return true;
    }

    private void removeClick(int id) {
        if (MainFrame.getInstance().getWzKeyStorage().deleteById(id)) {
            DefaultComboBoxModel<WzKey> model = (DefaultComboBoxModel<WzKey>) keyBox.getModel();
            for (int i = 0; i < model.getSize(); i++) {
                WzKey key = model.getElementAt(i);
                if (key.getId() == id) {
                    model.removeElementAt(i);
                    break;
                }
            }

            model = (DefaultComboBoxModel<WzKey>) MainFrame.getInstance().getKeyBox().getModel();
            for (int i = 0; i < model.getSize(); i++) {
                WzKey key = model.getElementAt(i);
                if (key.getId() == id) {
                    model.removeElementAt(i);
                    break;
                }
            }
        }
    }

    private void saveClick() {
        WzKey wzKey = (WzKey) Objects.requireNonNull(keyBox.getSelectedItem());
        byte[] iv = new byte[4];
        byte[] userKey = new byte[128];

        for (int i = 0; i < 4; i++) {
            iv[i] = fromHex(ivFields[i].getText());
        }

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 4; c++) {
                userKey[r * 8 + c * 4] = fromHex(userKeyFields[r][c].getText());
            }
        }

        // 更新管理器的
        wzKey.setIv(iv);
        wzKey.setUserKey(userKey);

        // 更新主界面的
        List<WzKey> wzKeys = new ArrayList<>();
        int itemCount = MainFrame.getInstance().getKeyBox().getItemCount();
        for (int i = 0; i < itemCount; i++) {
            WzKey key = MainFrame.getInstance().getKeyBox().getItemAt(i);
            if (key.getId().equals(wzKey.getId())) {
                key.setIv(iv);
                key.setUserKey(userKey);
            }
            wzKeys.add(key);
        }

        MainFrame.getInstance().getWzKeyStorage().saveAll(wzKeys);
        JMessageUtil.info("密钥已保存");
    }
}
