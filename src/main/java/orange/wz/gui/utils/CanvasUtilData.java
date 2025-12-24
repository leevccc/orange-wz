package orange.wz.gui.utils;

import lombok.Getter;
import orange.wz.provider.properties.WzPngFormat;

import java.awt.image.BufferedImage;

@Getter
public final class CanvasUtilData {
    private final String path;
    private final BufferedImage image;
    private final int width;
    private final int height;
    private final WzPngFormat format;

    public CanvasUtilData(String path, BufferedImage image, int width, int height, WzPngFormat format) {
        this.path = path;
        this.image = image;
        this.width = width;
        this.height = height;
        this.format = format;
    }
}
