package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

@Setter
@Getter
public class WzLongProperty extends WzImageProperty {
    private long value;

    public WzLongProperty(String name, long value, WzObject parent, WzImage wzImage) {
        super(name, WzType.LONG_PROPERTY, parent, wzImage);
        this.value = value;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 20);
        writer.writeCompressedLong(value);
    }

    @Override
    public WzLongProperty deepClone(WzObject parent) {
        return new WzLongProperty(name, value, parent, null);
    }
}
