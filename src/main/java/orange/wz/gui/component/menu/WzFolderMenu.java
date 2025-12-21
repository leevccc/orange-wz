package orange.wz.gui.component.menu;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static orange.wz.gui.Icons.AiOutlineCloseIcon;
import static orange.wz.gui.Icons.FiPackage;

@Slf4j
public final class WzFolderMenu extends JPopupMenu {
    private final EditPane editPane;
    private final JTree tree;

    public WzFolderMenu(EditPane editPane, JTree tree) {
        super();
        this.editPane = editPane;
        this.tree = tree;

        JMenuItem packageBtn = new JMenuItem("打包", FiPackage);
        JMenuItem unloadBtn = new JMenuItem("卸载", AiOutlineCloseIcon);

        packageBtnAction(packageBtn);
        unloadBtnAction(unloadBtn);

        add(packageBtn);
        add(unloadBtn);
    }

    private void packageBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;
            if (selectedPaths.length != 1) {
                JMessageUtil.error("右键文件夹使用打包功能，不要多选！");
                return;
            }
            Short fileVersion = null;
            while (fileVersion == null) {
                String input = JOptionPane.showInputDialog("版本号(79、83等)：");
                if (input == null) return;
                try {
                    short value = Short.parseShort(input.trim());
                    if (value < 0) JMessageUtil.error("版本号只能是大于0的纯数字");
                    fileVersion = value;
                } catch (NumberFormatException ex) {
                    JMessageUtil.error("版本号只能是大于0的纯数字");
                }
            }

            List<File> folders = FileDialog.chooseOpenFolders(null, "请选择输出目录", false);
            if (folders.size() != 1) {
                JMessageUtil.error("一个奇怪的问题导致程序无法继续");
                return;
            }
            File folder = folders.getFirst();

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
            WzFolder wzFolder = (WzFolder) node.getUserObject();

            Short finalFileVersion = fileVersion;
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    if (wzFolder.getName().equals("Data")) {
                        List<WzObject> children = wzFolder.getChildren();
                        int count = 0;
                        int total = children.size() + 1;
                        MainFrame.getInstance().updateProgress(0, total);

                        String savePath = Path.of(folder.getAbsolutePath(), "Base.wz").toString();
                        packageBase(finalFileVersion, wzFolder, savePath);
                        MainFrame.getInstance().updateProgress(++count, total);

                        for (WzObject wzObject : children) {
                            if (wzObject instanceof WzFolder child) {
                                savePath = Path.of(folder.getAbsolutePath(), child.getName() + ".wz").toString();
                                packageFolder(finalFileVersion, child, savePath);
                            }
                            MainFrame.getInstance().updateProgress(++count, total);
                        }
                    } else {
                        String savePath = Path.of(folder.getAbsolutePath(), wzFolder.getName() + ".wz").toString();
                        packageFolder(finalFileVersion, wzFolder, savePath);
                    }

                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        MainFrame.getInstance().setStatusText("%s 打包完成", wzFolder.getName());
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }.execute();
        });
    }

    private void unloadBtnAction(JMenuItem item) {
        item.addActionListener(e -> {
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null) return;

            for (TreePath treePath : selectedPaths) {
                editPane.removeNodeFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
            }

            System.gc();
        });
    }

    // 打包
    private void packageBase(short fileVersion, WzFolder wzFolder, String savePath) {
        Set<String> directories = new HashSet<>();
        List<WzImageFile> imageFiles = new ArrayList<>();
        for (WzObject child : wzFolder.getChildren()) {
            if (child instanceof WzDirectory directory) {
                directories.add(directory.getName());
            } else if (child instanceof WzImageFile imageFile) {
                imageFiles.add(imageFile);
            }
        }

        WzFile wzFile = WzFile.createNewFile(savePath, fileVersion, wzFolder.getIv(), wzFolder.getKey());
        directories.forEach(directory -> wzFile.getWzDirectory().addChild(new WzDirectory(directory, wzFile.getWzDirectory(), wzFile)));
        imageFiles.forEach(imageFile -> {
            imageFile.parse(false);
            wzFile.getWzDirectory().addChild(imageFile);
        });
        wzFile.save();
    }

    private void packageFolder(short fileVersion, WzFolder wzFolder, String savePath) {
        WzFile wzFile = WzFile.createNewFile(savePath, fileVersion, wzFolder.getIv(), wzFolder.getKey());
        packageSubToWz(wzFolder, wzFile.getWzDirectory());
        wzFile.save();
    }

    private void packageSubToWz(WzFolder wzFolder, WzDirectory parent) {
        for (WzObject child : wzFolder.getChildren()) {
            if (child instanceof WzFolder subFolder) {
                WzDirectory wzDirectory = new WzDirectory(child.getName(), parent, parent.getWzFile());
                packageSubToWz(subFolder, wzDirectory);
                parent.addChild(wzDirectory);
            } else if (child instanceof WzImageFile imageFile) {
                imageFile.parse(false);
                parent.addChild(imageFile);
            }
        }
    }
}
