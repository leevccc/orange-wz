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
public class WzStringProperty extends WzImageProperty {
    private String value;

    public WzStringProperty(String name, String value, WzObject parent, WzImage wzImage) {
        super(name, WzType.STRING_PROPERTY, parent, wzImage);
        this.value = value;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 8);
        writer.writeStringBlock(value, 0x00, 0x01);
    }

    @Override
    public WzStringProperty deepClone(WzObject parent) {
        return new WzStringProperty(name, value, parent, null);
    }
}
