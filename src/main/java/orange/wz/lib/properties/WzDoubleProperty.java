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
public class WzDoubleProperty extends WzImageProperty {
    private double value;
    private final String type = "double";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 5);
        writer.putDouble(value);
    }

    @Override
    public WzDoubleProperty deepClone(WzObject parent) {
        return WzDoubleProperty.builder().name(getName()).parent(parent).value(value).build();
    }
}
