package orange.wz.lib.properties;

import orange.wz.lib.BinaryWriter;
import orange.wz.lib.WzImage;
import orange.wz.lib.WzObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class WzUOLProperty extends WzExtended {
    private String uol;
    private final String type = "uol";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzPropertyType.UOL.getString(), WzImage.wzImageHeaderByte_WithoutOffset, WzImage.wzImageHeaderByte_WithOffset);
        writer.putByte((byte) 0);
        writer.writeStringBlock(uol, 0x00, 0x01);
    }

    @Override
    public WzUOLProperty deepClone(WzObject parent) {
        return WzUOLProperty.builder().name(getName()).parent(parent).uol(uol).build();
    }
}
