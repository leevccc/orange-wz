package orange.wz.provider.properties;

import lombok.Getter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

import java.util.List;

@Getter
public class WzListProperty extends WzExtended {
    public WzListProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.LIST_PROPERTY, parent, wzImage);
    }

    public WzListProperty(List<WzImageProperty> properties) {
        super(null, WzType.LIST_PROPERTY, null, null);
        addChildren(properties);
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzExtendedType.LIST.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        WzImage.writeListValue(writer, children.get());
    }

    @Override
    public WzListProperty deepClone(WzObject parent) {
        WzListProperty clone = new WzListProperty(name, parent, null);
        for (WzImageProperty property : children.get()) {
            clone.addChild(property.deepClone(clone));
        }
        return clone;
    }
}
