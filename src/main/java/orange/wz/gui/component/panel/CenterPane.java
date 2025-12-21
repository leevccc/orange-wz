package orange.wz.gui.component.panel;

import lombok.Getter;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public final class CenterPane extends JSplitPane {
    @Getter
    private final EditPane leftEditPane;
    private final EditPane rightEditPane;

    private int lastDividerLocation = -1;
    private boolean showRight = false;

    public CenterPane() {
        super(JSplitPane.HORIZONTAL_SPLIT);

        leftEditPane = new EditPane(false);
        rightEditPane = new EditPane(true);

        setLeftComponent(leftEditPane);
        setRightComponent(rightEditPane);

        setResizeWeight(1.0);
        setDividerSize(0); // 初始不显示分割线

        // 等 UI ready 之后再隐藏右侧
        SwingUtilities.invokeLater(() -> {
            setDividerLocation(getWidth());
        });

        // 避免拉动边框的时候把隐藏的右侧面板拉出来
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!showRight) {
                    setDividerLocation(getWidth());
                }
            }
        });

    }

    public void switchRightEditPaneVisible() {
        if (showRight) {
            // 当前显示 → 隐藏
            lastDividerLocation = getDividerLocation();
            setDividerLocation(getWidth());
            setDividerSize(0);
        } else {
            // 当前隐藏 → 显示
            setDividerSize(6);
            if (lastDividerLocation > 0) {
                setDividerLocation(lastDividerLocation);
            } else {
                setDividerLocation(0.5); // 第一次显示给个合理默认
            }
        }

        showRight = !showRight;
    }
}
