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
public class WzShortProperty extends WzImageProperty {
    private short value;
    private final String type = "short";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 2);
        writer.putShort(value);
    }

    @Override
    public WzShortProperty deepClone(WzObject parent) {
        return WzShortProperty.builder().name(getName()).parent(parent).value(value).build();
    }
}
