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
public class WzNullProperty extends WzImageProperty {
    private final String type = "null";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 0);
    }

    @Override
    public WzNullProperty deepClone(WzObject parent) {
        return WzNullProperty.builder().name(getName()).parent(parent).build();
    }
}
