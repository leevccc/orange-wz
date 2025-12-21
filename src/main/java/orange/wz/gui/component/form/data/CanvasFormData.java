package orange.wz.gui.component.form.data;

import lombok.Getter;
import orange.wz.provider.properties.WzPngFormat;

import java.awt.image.BufferedImage;

@Getter
public class CanvasFormData extends NodeFormData {
    private final BufferedImage value;
    private final WzPngFormat format;

    public CanvasFormData(String name, String type, BufferedImage value, WzPngFormat format) {
        super(name, type);
        this.value = value;
        this.format = format;
    }
}
