package orange.wz.gui.component.menu;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.Clipboard;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.ChineseUtil;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.time.Duration;
import java.time.Instant;

import static orange.wz.gui.Icons.AiOutlineCopy;
import static orange.wz.gui.Icons.AiOutlineDelete;

@Slf4j
public final class WzValuePropertyMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;
    @Getter
    private final JMenuItem deleteBtn;
    @Getter
    private final JMenuItem copyBtn;

    public WzValuePropertyMenu(EditPane editPane, JTree tree) {
        super();
        this.editPane = editPane;
        this.tree = tree;

        copyBtn = new JMenuItem("复制", AiOutlineCopy);
        deleteBtn = new JMenuItem("删除节点", AiOutlineDelete);
        JMenuItem chineseBtn = new JMenuItem("汉化");

        addCopyBtnAction(copyBtn);
        deleteBtnAction(deleteBtn);
        addChineseBtnAction(chineseBtn);

        add(copyBtn);
        add(deleteBtn);
        add(chineseBtn);
    }

    private void addCopyBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            Clipboard clipboard = MainFrame.getInstance().getClipboard();
            clipboard.lock();
            clipboard.clear();
            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzImageProperty wzObject = (WzImageProperty) node.getUserObject();
                clipboard.add(wzObject.deepClone(null));
            }
            clipboard.unlock();
        });
    }

    private void deleteBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzObject wzObject = (WzObject) node.getUserObject();
                WzObject pWzObject = wzObject.getParent();

                if (pWzObject instanceof WzImage image && image.removeChild(wzObject.getName())) {
                    editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
                } else if (pWzObject instanceof WzImageProperty property && property.removeChild(wzObject.getName())) {
                    editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
                } else {
                    log.error("无法删除节点, 父节点类型: {}", pWzObject.getClass().getName());
                }
            }
        });
    }

    private void addChineseBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            Instant start = Instant.now();
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                WzImageProperty to = (WzImageProperty) node.getUserObject();

                WzImageProperty from = (WzImageProperty) MainFrame.getInstance().getCenterPane().getAnotherPane(editPane).findAnotherTreeWzObjectByPath(to.getPath());
                if (from == null) {
                    log.error("找不到中文版本的 {}", to.getName());
                    continue;
                }

                ChineseUtil.chinese(from, to);
            }

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            MainFrame.getInstance().setStatusText("汉化完成! 耗时 %d ms", duration.toMillis());
        });
    }
}
