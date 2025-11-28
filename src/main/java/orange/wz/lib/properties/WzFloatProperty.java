package orange.wz.lib.properties;

import orange.wz.lib.BinaryWriter;
import orange.wz.lib.WzImageProperty;
import orange.wz.lib.WzObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
public class WzFloatProperty extends WzImageProperty {
    private float value;
    private final String type = "float";

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
        return WzFloatProperty.builder().name(getName()).parent(parent).value(value).build();
    }
}
