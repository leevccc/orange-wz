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
public class WzShortProperty extends WzImageProperty {
    private short value;

    public WzShortProperty(String name, short value, WzObject parent, WzImage wzImage) {
        super(name, WzType.SHORT_PROPERTY, parent, wzImage);
        this.value = value;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 2);
        writer.putShort(value);
    }

    @Override
    public WzShortProperty deepClone(WzObject parent) {
        return new WzShortProperty(name, value, parent, null);
    }
}
