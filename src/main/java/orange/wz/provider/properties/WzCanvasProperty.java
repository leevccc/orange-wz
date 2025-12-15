package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

import java.util.List;

@Setter
@Getter
public class WzCanvasProperty extends WzExtended {
    private WzPngProperty png;

    public WzCanvasProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.CANVAS_PROPERTY, parent, wzImage);
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzExtendedType.CANVAS.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.putByte((byte) 0);
        List<WzImageProperty> properties = children.get();
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
        WzCanvasProperty clone = new WzCanvasProperty(name, parent, null);
        clone.setPng(png.deepClone(clone));
        for (WzImageProperty property : children.get()) {
            clone.addChild(property.deepClone(clone));
        }
        return clone;
    }
}
