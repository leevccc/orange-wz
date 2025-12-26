package orange.wz.gui.utils;

import orange.wz.gui.MainFrame;
import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzFile;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.WzListProperty;
import orange.wz.provider.properties.WzStringProperty;

public final class ChineseUtil {
    public static void chinese(WzObject from, WzObject to) {
        if (from == null || to == null) return;

        if (to instanceof WzFile toFile && from instanceof WzFile fromFile) {
            if (!toFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", toFile.getName(), toFile.getStatus().getMessage());
                throw new RuntimeException();
            }
            if (!fromFile.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", fromFile.getName(), fromFile.getStatus().getMessage());
                throw new RuntimeException();
            }

            toFile.getWzDirectory().getDirectories().forEach(toDir -> chinese(fromFile.getWzDirectory().getDirectory(toDir.getName()), toDir));
            toFile.getWzDirectory().getImages().forEach(toImage -> chinese(fromFile.getWzDirectory().getImage(toImage.getName()), toImage));
        } else if (to instanceof WzDirectory toDirectory && from instanceof WzDirectory fromDirectory) {
            toDirectory.getDirectories().forEach(toDir -> chinese(fromDirectory.getDirectory(toDir.getName()), toDir));
            toDirectory.getImages().forEach(toImage -> chinese(fromDirectory.getImage(toImage.getName()), toImage));
        } else if (to instanceof WzImage toImage && from instanceof WzImage fromImage) {
            if (!toImage.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", toImage.getName(), toImage.getStatus().getMessage());
                throw new RuntimeException();
            }
            if (!fromImage.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", fromImage.getName(), fromImage.getStatus().getMessage());
                throw new RuntimeException();
            }
            toImage.getChildren().forEach(img -> chinese(fromImage.getChild(img.getName()), img));
        } else if (to instanceof WzListProperty toListProperty && from instanceof WzListProperty fromList) {
            toListProperty.getChildren().forEach(prop -> chinese(fromList.getChild(prop.getName()), prop));
        } else if (to instanceof WzStringProperty toString && from instanceof WzStringProperty fromString) {
            String fromValue = fromString.getValue();
            if (fromValue != null && !isChineseStr(toString.getValue()) && isChineseStr(fromValue)) {
                toString.getWzImage().setChanged(true);
                toString.setValue(fromValue);
            }
        }
    }

    public static boolean isChineseStr(String str) {
        return !str.matches(".*[\\uAC00-\\uD7A3].*")  // 不能有韩文
                && str.matches(".*[\\u4e00-\\u9fa5].*");  // 有中文字符
    }
}
