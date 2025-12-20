package orange.wz.gui.key;

import orange.wz.utils.wzkey.WzKey;

import javax.swing.*;
import java.awt.*;

public final class KeyBox extends JComboBox<WzKey> {
    public KeyBox(WzKey[] keys) {
        super(keys);

        // 固定宽度
        Dimension size = new Dimension(140, super.getPreferredSize().height);
        setPreferredSize(size);
        setMaximumSize(size);

        // 自定义渲染
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value instanceof WzKey key) {
                    setText(key.getName());
                } else {
                    setText("");
                }
                return this;
            }
        });

        // 初始化选项
        for (WzKey key : keys) {
            if (key.isSelected()) {
                setSelectedItem(key);
                break;
            }
        }
    }
}
