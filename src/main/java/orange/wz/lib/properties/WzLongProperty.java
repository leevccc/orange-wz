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
public class WzLongProperty extends WzImageProperty {
    private long value;
    private final String type = "long";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 20);
        writer.writeCompressedLong(value);
    }

    @Override
    public WzLongProperty deepClone(WzObject parent) {
        return WzLongProperty.builder().name(getName()).parent(parent).value(value).build();
    }
}
