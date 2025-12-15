package orange.wz.provider.properties;

import lombok.Getter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WzConvexProperty extends WzExtended {
    public WzConvexProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.CONVEX_PROPERTY, parent, wzImage);
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        List<WzExtended> extendedProps = new ArrayList<>();
        for (WzImageProperty prop : children.get()) {
            if (prop instanceof WzExtended) extendedProps.add((WzExtended) prop);
        }
        writer.writeStringBlock(WzExtendedType.CONVEX.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.writeCompressedInt(extendedProps.size());

        for (WzExtended extendedProp : extendedProps) {
            extendedProp.writeValue(writer);
        }
    }

    @Override
    public WzConvexProperty deepClone(WzObject parent) {
        WzConvexProperty clone = new WzConvexProperty(name, parent, null);
        for (WzImageProperty prop : children.get()) {
            clone.addChild(prop.deepClone(clone));
        }
        return clone;
    }
}
