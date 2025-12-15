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
public class WzFloatProperty extends WzImageProperty {
    private float value;

    public WzFloatProperty(String name, float value, WzObject parent, WzImage wzImage) {
        super(name, WzType.FLOAT_PROPERTY, parent, wzImage);
        this.value = value;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 4);
        if (value == 0f) {
            writer.putByte((byte) 0);
        } else {
            writer.putByte((byte) 0x80);
            writer.putFloat(value);
        }
    }

    @Override
    public WzFloatProperty deepClone(WzObject parent) {
        return new WzFloatProperty(name, value, parent, null);
    }
}
