package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

@Setter
@Getter
public class WzVectorProperty extends WzExtended {
    private int x;
    private int y;

    public WzVectorProperty(String name, int x, int y, WzObject parent, WzImage wzImage) {
        super(name, WzType.VECTOR_PROPERTY, parent, wzImage);
        this.x = x;
        this.y = y;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzExtendedType.VECTOR.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.writeCompressedInt(x);
        writer.writeCompressedInt(y);
    }

    @Override
    public WzVectorProperty deepClone(WzObject parent) {
        return new WzVectorProperty(name, x, y, parent, null);
    }
}
