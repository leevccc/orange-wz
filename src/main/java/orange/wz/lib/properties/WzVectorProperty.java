package orange.wz.lib.properties;

import orange.wz.lib.BinaryWriter;
import orange.wz.lib.WzImage;
import orange.wz.lib.WzObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
public class WzVectorProperty extends WzExtended {
    private WzIntProperty x;
    private WzIntProperty y;
    private final String type = "vector";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzPropertyType.VECTOR.getString(), WzImage.wzImageHeaderByte_WithoutOffset, WzImage.wzImageHeaderByte_WithOffset);
        writer.writeCompressedInt(x.getValue());
        writer.writeCompressedInt(y.getValue());
    }

    @Override
    public WzVectorProperty deepClone(WzObject parent) {
        WzVectorProperty clone = WzVectorProperty.builder().name(getName()).parent(parent).build();
        clone.x = x.deepClone(clone);
        clone.y = y.deepClone(clone);
        return clone;
    }
}
