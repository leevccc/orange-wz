package orange.wz.gui.component;

import com.formdev.flatlaf.util.SystemFileChooser;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

public class FileDialog {
    private static final Preferences prefs = Preferences.userNodeForPackage(FileDialog.class);

    /**
     * 文件选择器
     *
     * @param parent     父组件，可 null
     * @param title      对话框标题
     * @param allowMulti 是否允许多选
     * @param filters    扩展名过滤器，例如 {"txt","md"}，null = 不过滤
     * @return 用户选择的文件列表，取消选择返回空列表
     */
    public static List<File> chooseOpenFiles(Component parent, String title, boolean allowMulti, String[] filters) {
        List<File> result = new ArrayList<>();

        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(allowMulti);

        // 恢复上次目录
        String lastDir = prefs.get("last", null);
        if (lastDir != null) {
            chooser.setCurrentDirectory(new File(lastDir));
        }

        // 添加扩展名过滤
        if (filters != null && filters.length > 0) {
            String desc = String.join(", ", filters) + " 文件";
            chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter(desc, filters));
            chooser.setAcceptAllFileFilterUsed(false);
        }

        if (chooser.showOpenDialog(parent) == SystemFileChooser.APPROVE_OPTION) {
            File[] selected = chooser.getSelectedFiles();
            if (selected != null) {
                Collections.addAll(result, selected);
            }
        }

        if (!result.isEmpty()) {
            prefs.put("last", result.getFirst().toString());
        }

        return result;
    }

    /**
     * 文件夹选择器
     *
     * @param parent     父组件，可 null
     * @param title      对话框标题
     * @param allowMulti 是否允许多选
     * @return 用户选择的文件夹列表，取消选择返回空列表
     */
    public static List<File> chooseOpenFolders(Component parent, String title, boolean allowMulti) {
        List<File> result = new ArrayList<>();

        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(allowMulti);

        // 恢复上次目录
        String lastDir = prefs.get("last", null);
        if (lastDir != null) {
            chooser.setCurrentDirectory(new File(lastDir));
        }

        if (chooser.showOpenDialog(parent) == SystemFileChooser.APPROVE_OPTION) {
            File[] selected = chooser.getSelectedFiles();
            if (selected != null) {
                Collections.addAll(result, selected);
            }
        }

        if (!result.isEmpty()) {
            prefs.put("last", result.getFirst().toString());
        }

        return result;
    }

    public static List<File> chooseOpenFolders() {
        return chooseOpenFolders(null, "请选择文件夹", true);
    }

    public static List<File> chooseOpenFiles(String[] filters) {
        return chooseOpenFiles(null, "请选择文件", true, filters);
    }

    public static File chooseOpenFolder(String title) {
        List<File> selected = chooseOpenFolders(null, title, false);
        if (selected.isEmpty()) return null;
        return selected.getFirst();
    }

    public static File chooseOpenFile(String[] filters) {
        List<File> files = chooseOpenFiles(null, "请选择文件", false, filters);
        if (files.isEmpty()) {
            return null;
        }
        return files.getFirst();
    }

    public static File chooseSaveFile(Component parent, String title, File defaultFile, String[] filters) {
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setSelectedFile(defaultFile);

        // 添加扩展名过滤
        boolean hasFilter = filters != null && filters.length > 0;
        if (hasFilter) {
            String desc = String.join(", ", filters) + " 文件";
            chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter(desc, filters));
            chooser.setAcceptAllFileFilterUsed(false);
        }

        File file = null;
        if (chooser.showSaveDialog(parent) == SystemFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            if (hasFilter) { // 检查输入的后缀名
                String name = file.getName();

                boolean hasExt = false;
                for (String ext : filters) {
                    if (name.toLowerCase().endsWith("." + ext.toLowerCase())) {
                        hasExt = true;
                        break;
                    }
                }

                if (!hasExt) {
                    file = new File(file.getParentFile(), name + "." + filters[0]);
                }
            }
        }

        return file;
    }
}
