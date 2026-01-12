package orange.wz.provider.properties;

import lombok.Getter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.*;

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

    public boolean sortAndReindexChildren() {
        List<WzImageProperty> list = children.get();

        WzTool.sortWzObjects(list);

        int i = 0;
        boolean changed = false;
        for (WzImageProperty property : list) {
            String name = property.getName();
            String newName = String.valueOf(i);
            if (StringTool.isInteger(name)) {
                if (!name.equals(newName)) {
                    property.setName(newName);
                    property.setTempChanged(true);
                    property.getWzImage().setChanged(true);
                    changed = true;
                }
                i++;
            }
        }

        children = new WzChildrenProperty();
        children.add(list);
        if (changed) {
            setTempChanged(true);
            return true;
        }
        return false;
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
