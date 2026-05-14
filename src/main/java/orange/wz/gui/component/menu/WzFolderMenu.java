package orange.wz.gui.component.menu;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.gui.utils.TreePathUtil;
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

import static orange.wz.gui.Icons.FiPackage;

@Slf4j
public final class WzFolderMenu extends TreeMenu {
    private final JTree tree;

    public WzFolderMenu(EditPane editPane, JTree tree) {
        super(editPane);
        this.tree = tree;

        JMenuItem btnPackage = new JMenuItem(MainFrame.i18n.get("test.temp0127"), FiPackage);
        btnPackage.addActionListener(e -> packageBtnAction());

        add(btnSave);
        add(btnPackage);
        add(btnUnload);
        add(btnReload);
        add(btnChangeKey);
        add(btnExport);
    }

    private void packageBtnAction() {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (TreePathUtil.isNullOrMultiple(selectedPaths)) return;

        Short fileVersion = null;
        while (fileVersion == null) {
            String input = JOptionPane.showInputDialog(MainFrame.i18n.get("test.temp0128"));
            if (input == null) return;
            try {
                short value = Short.parseShort(input.trim());
                if (value < 0) JMessageUtil.error(MainFrame.i18n.get("test.temp0129"));
                fileVersion = value;
            } catch (NumberFormatException ex) {
                JMessageUtil.error(MainFrame.i18n.get("test.temp0129"));
            }
        }

        File folder = FileDialog.chooseOpenFolder(MainFrame.i18n.get("test.temp0130"));
        if (folder == null) {
            log.info(MainFrame.i18n.get("test.temp0131"));
            return;
        }

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
                    String savePath = Path.of(folder.getAbsolutePath(), wzFolder.getName()).toString();
                    if (!savePath.endsWith(".wz")) savePath = savePath + ".wz";
                    packageFolder(finalFileVersion, wzFolder, savePath);
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    MainFrame.getInstance().setStatusText(MainFrame.i18n.get("status.package_success", wzFolder.getName()));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }.execute();
    }

    private void packageBase(short fileVersion, WzFolder wzFolder, String savePath) {
        Set<String> directories = new HashSet<>();
        List<WzImageFile> imageFiles = new ArrayList<>();
        for (WzObject child : wzFolder.getChildren()) {
            if (child instanceof WzFolder directory) {
                directories.add(directory.getName());
            } else if (child instanceof WzImageFile imageFile) {
                imageFiles.add(imageFile);
            }
        }

        WzFile wzFile = WzFile.createNewFile(savePath, fileVersion, wzFolder.getKeyBoxName(), wzFolder.getIv(), wzFolder.getKey());
        directories.forEach(directory -> wzFile.getWzDirectory().addChild(new WzDirectory(directory, wzFile.getWzDirectory(), wzFile)));
        imageFiles.forEach(imageFile -> {
            if (!imageFile.parse(false)) {
                MainFrame.getInstance().setStatusTextWithErrLog(MainFrame.i18n.get("error.parse", imageFile.getName(), imageFile.getStatus().getMessage()));
                throw new RuntimeException();
            }
            wzFile.getWzDirectory().addChild(imageFile);
        });
        wzFile.save();
    }

    private void packageFolder(short fileVersion, WzFolder wzFolder, String savePath) {
        WzFile wzFile = WzFile.createNewFile(savePath, fileVersion, wzFolder.getKeyBoxName(), wzFolder.getIv(), wzFolder.getKey());
        packageSubToWz(wzFolder, wzFile.getWzDirectory());
        MainFrame.getInstance().setStatusText(MainFrame.i18n.get("status.package_start", wzFile.getName()));
        wzFile.save();
        MainFrame.getInstance().setStatusText(MainFrame.i18n.get("status.package_success", wzFile.getName()));
    }

    private void packageSubToWz(WzFolder wzFolder, WzDirectory parent) {
        List<WzObject> children = wzFolder.getChildren();
        int total = children.size();
        int current = 0;
        MainFrame.getInstance().setStatusText(MainFrame.i18n.get("status.package_running", wzFolder.getName()));
        for (WzObject child : children) {
            if (child instanceof WzFolder subFolder) {
                WzDirectory wzDirectory = new WzDirectory(child.getName(), parent, parent.getWzFile());
                packageSubToWz(subFolder, wzDirectory);
                parent.addChild(wzDirectory);
            } else if (child instanceof WzImageFile imageFile) {
                if (!imageFile.parse(false)) {
                    MainFrame.getInstance().setStatusTextWithErrLog(MainFrame.i18n.get("error.parse", imageFile.getName(), imageFile.getStatus().getMessage()));
                    throw new RuntimeException();
                }
                parent.addChild(imageFile);
            } else if (child instanceof WzXmlFile xmlFile) {
                if (!xmlFile.parse()) {
                    MainFrame.getInstance().setStatusTextWithErrLog(MainFrame.i18n.get("error.parse", xmlFile.getName(), xmlFile.getStatus().getMessage()));
                    throw new RuntimeException();
                }
                parent.addChild(xmlFile);
            }
            MainFrame.getInstance().updateProgress(++current, total);
        }
    }
}
