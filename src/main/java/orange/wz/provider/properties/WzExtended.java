package orange.wz.provider.properties;

import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.WzType;

public abstract class WzExtended extends WzImageProperty {
    protected WzExtended(String name, WzType type, WzObject parent, WzImage wzImage) {
        super(name, type, parent, wzImage);
    }
}
