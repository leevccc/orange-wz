package orange.wz.provider.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

import java.util.List;

@Setter
@Getter
public class WzRawDataProperty extends WzExtended {
    private byte dataType;
    private int length;
    private int offset;
    @Getter(AccessLevel.NONE)
    private byte[] bytes;

    public WzRawDataProperty(String name, byte dataType, int length, WzObject parent, WzImage wzImage) {
        super(name, WzType.RAW_DATA_PROPERTY, parent, wzImage);
        this.dataType = dataType;
        this.length = length;
    }

    public byte[] getBytes(boolean saveInMem) {
        if (bytes == null) {
            byte[] returnBytes = null;
            BinaryReader reader = wzImage.getReader();
            int curOffset = reader.getPosition();
            reader.setPosition(offset);
            returnBytes = reader.getBytes(length);
            reader.setPosition(curOffset);
            if (saveInMem) {
                bytes = returnBytes;
            }
            return returnBytes;
        }
        return bytes;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzExtendedType.RAW_DATA.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.putByte(dataType);
        if (dataType == 1) {
            List<WzImageProperty> properties = children.get();
            if (!properties.isEmpty()) {
                writer.putByte((byte) 1);
                WzImage.writeListValue(writer, properties);
            } else {
                writer.putByte((byte) 0);
            }
        }
        byte[] bytes = getBytes(false);
        writer.writeCompressedInt(bytes.length);
        writer.putBytes(bytes);
    }

    @Override
    public WzRawDataProperty deepClone(WzObject parent) {
        WzRawDataProperty clone = new WzRawDataProperty(name, dataType, length, parent, null);
        for (WzImageProperty property : children.get()) {
            clone.addChild(property.deepClone(clone));
        }
        byte[] bytes = getBytes(false);
        int len = bytes.length;
        clone.bytes = new byte[len];
        System.arraycopy(bytes, 0, clone.bytes, 0, len);
        return clone;
    }
}
