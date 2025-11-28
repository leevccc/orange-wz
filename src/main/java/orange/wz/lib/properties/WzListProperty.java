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
public class WzListProperty extends WzExtended {
    private final List<WzImageProperty> properties = new ArrayList<>();
    private final String type = "list";

    public void addProperties(List<WzImageProperty> properties) {
        this.properties.addAll(properties);
    }

    public WzImageProperty get(String name) {
        for (WzImageProperty property : properties) {
            if (property.getName().equals(name)) return property;
        }
        return null;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzPropertyType.LIST.getString(), WzImage.wzImageHeaderByte_WithoutOffset, WzImage.wzImageHeaderByte_WithOffset);
        WzImage.writeListValue(writer, properties);
    }

    @Override
    public WzListProperty deepClone(WzObject parent) {
        WzListProperty clone = WzListProperty.builder().name(getName()).build();
        for (WzImageProperty property : properties) {
            clone.properties.add(property.deepClone(clone));
        }
        return clone;
    }
}
