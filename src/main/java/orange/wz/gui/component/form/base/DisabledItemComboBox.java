package orange.wz.gui.component.form.base;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class DisabledItemComboBox<T> extends JComboBox<T> {

    private final Set<T> disabledItems = new HashSet<>();
    private T lastValidSelection = null;

    public DisabledItemComboBox(T[] items) {
        super(items);
        init();
    }

    private void init() {
        // 记录初始合法选项
        if (getItemCount() > 0) {
            lastValidSelection = getItemAt(0);
        }

        // 自定义渲染（灰色显示）
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                Component c = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value != null && disabledItems.contains(value)) {
                    c.setForeground(Color.GRAY);
                }

                return c;
            }
        });

        // 拦截非法选择
        addActionListener(e -> {
            T selected = (T) getSelectedItem();
            if (selected != null && disabledItems.contains(selected)) {
                // 回退到上一个合法值
                setSelectedItem(lastValidSelection);
            } else {
                lastValidSelection = selected;
            }
        });
    }

    public void disableItem(T item) {
        disabledItems.add(item);
        repaint();
    }

    public void enableItem(T item) {
        disabledItems.remove(item);
        repaint();
    }

    public boolean isItemDisabled(T item) {
        return disabledItems.contains(item);
    }
}
