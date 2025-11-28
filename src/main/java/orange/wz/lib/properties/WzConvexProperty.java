package orange.wz.lib.properties;

import orange.wz.lib.BinaryWriter;
import orange.wz.lib.WzImage;
import orange.wz.lib.WzImageProperty;
import orange.wz.lib.WzObject;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
public class WzConvexProperty extends WzExtended {
    private final List<WzImageProperty> properties = new ArrayList<>();
    private final String type = "convex";

    public void addProperty(WzImageProperty property) {
        properties.add(property);
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        List<WzExtended> extendedProps = new ArrayList<>();
        for (WzImageProperty prop : properties) {
            if (prop instanceof WzExtended) extendedProps.add((WzExtended) prop);
        }
        writer.writeStringBlock(WzPropertyType.CONVEX.getString(), WzImage.wzImageHeaderByte_WithoutOffset, WzImage.wzImageHeaderByte_WithOffset);
        writer.writeCompressedInt(extendedProps.size());

        for (WzExtended extendedProp : extendedProps) {
            extendedProp.writeValue(writer);
        }
    }

    @Override
    public WzConvexProperty deepClone(WzObject parent) {
        WzConvexProperty clone = WzConvexProperty.builder().name(getName()).parent(parent).build();
        for (WzImageProperty prop : properties) {
            clone.properties.add(prop.deepClone(clone));
        }
        return clone;
    }
}
