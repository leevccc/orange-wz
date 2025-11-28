package orange.wz.lib.properties;

import orange.wz.lib.BinaryWriter;
import orange.wz.lib.WzImage;
import orange.wz.lib.WzImageProperty;
import orange.wz.lib.WzObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@SuperBuilder
public class WzCanvasProperty extends WzExtended {
    private final List<WzImageProperty> properties = new ArrayList<>();
    private WzPngProperty png;
    private final String type = "canvas";

    public void addProperties(List<WzImageProperty> properties) {
        this.properties.addAll(properties);
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzPropertyType.CANVAS.getString(), WzImage.wzImageHeaderByte_WithoutOffset, WzImage.wzImageHeaderByte_WithOffset);
        writer.putByte((byte) 0);
        if (!properties.isEmpty()) {
            writer.putByte((byte) 1);
            WzImage.writeListValue(writer, properties);
        } else {
            writer.putByte((byte) 0);
        }

        // Image Info
        writer.writeCompressedInt(png.getWidth());
        writer.writeCompressedInt(png.getHeight());
        writer.writeCompressedInt(png.getFormat());
        writer.putByte((byte) png.getFormat2());
        writer.putInt(0);

        // Write image
        byte[] bytes = png.getCompressedBytes();
        writer.putInt(bytes.length + 1);
        writer.putByte((byte) 0);
        writer.putBytes(bytes);
    }

    @Override
    public WzCanvasProperty deepClone(WzObject parent) {
        WzCanvasProperty clone = WzCanvasProperty.builder().name(getName()).parent(parent).build();
        clone.setPng(png.deepClone(clone));
        for (WzImageProperty property : properties) {
            clone.properties.add(property.deepClone(clone));
        }
        return clone;
    }
}
