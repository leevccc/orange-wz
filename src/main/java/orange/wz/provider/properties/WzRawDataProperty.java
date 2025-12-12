package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import orange.wz.provider.BinaryWriter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@SuperBuilder
public class WzRawDataProperty extends WzExtended {
    private byte type;
    private int length;
    private byte[] bytes;
    private final List<WzImageProperty> properties = new ArrayList<>();

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzPropertyType.RAW_DATA.getString(), WzImage.wzImageHeaderByte_WithoutOffset, WzImage.wzImageHeaderByte_WithOffset);
        writer.putByte(type);
        if (type == 1) {
            if (!properties.isEmpty()) {
                writer.putByte((byte) 1);
                WzImage.writeListValue(writer, properties);
            } else {
                writer.putByte((byte) 0);
            }
        }
        writer.writeCompressedInt(bytes.length);
        writer.putBytes(bytes);
    }

    @Override
    public WzRawDataProperty deepClone(WzObject parent) {
        WzRawDataProperty clone = WzRawDataProperty.builder().name(getName()).parent(parent).type(type).length(length).build();
        for (WzImageProperty property : properties) {
            clone.properties.add(property.deepClone(clone));
        }
        int len = bytes.length;
        clone.bytes = new byte[len];
        System.arraycopy(bytes, 0, clone.bytes, 0, len);
        return clone;
    }
}
