package orange.wz.gui.utils;

import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.WzCanvasProperty;

import java.util.List;

public final class CanvasUtil {
    public static void search(List<CanvasUtilData> result, List<? extends WzObject> wzObjects) {
        for (WzObject wzObject : wzObjects) {
            if (wzObject instanceof WzCanvasProperty canvas) {
                result.add(new CanvasUtilData(
                        canvas.getPath(),
                        canvas.getPngImage(false),
                        canvas.getWidth(),
                        canvas.getHeight(),
                        canvas.getPngFormat()
                ));
            } else if (wzObject instanceof WzImageProperty prop && prop.isListProperty()) {
                search(result, prop.getChildren());
            } else if (wzObject instanceof WzDirectory wzDir) {
                if (wzDir.isWzFile()) wzDir.getWzFile().load();
                search(result, wzDir.getChildren());
            } else if (wzObject instanceof WzImage wzImg) {
                wzImg.parse();
                search(result, wzImg.getChildren());
            }
        }
    }
}
