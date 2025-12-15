package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

@Getter
@Setter
public class WzUOLProperty extends WzExtended {
    private String value;

    public WzUOLProperty(String name, String value, WzObject parent, WzImage wzImage) {
        super(name, WzType.UOL_PROPERTY, parent, wzImage);
        this.value = value;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzExtendedType.UOL.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.putByte((byte) 0);
        writer.writeStringBlock(value, 0x00, 0x01);
    }

    @Override
    public WzUOLProperty deepClone(WzObject parent) {
        return new WzUOLProperty(name, value, parent, null);
    }
}
