package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.properties.WzExtended;
import orange.wz.provider.properties.WzListProperty;
import orange.wz.provider.tools.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.List;

@Getter
@Setter
@Slf4j
public class WzImage extends WzObject {
    private int dataSize;
    private int checksum; // reader 所有 bytes 值的和
    private int offset;
    private BinaryReader reader;
    private final WzChildrenProperty children = new WzChildrenProperty();

    protected WzFileStatus status;
    private boolean changed;
    private int tempFileStart;
    private int tempFileEnd;

    public static final int withOffsetFlag = 0x1B;
    public static final int withoutOffsetFlag = 0x73;

    protected WzImage(String name, WzObject parent) {
        super(name, WzType.IMAGE, parent);
    }

    public WzImage(String name, WzObject parent, BinaryReader binaryReader) {
        super(name, WzType.IMAGE, parent);
        reader = binaryReader;
        status = WzFileStatus.PARSE_SUCCESS;
        changed = true;
    }

    public WzImage(String name, BinaryReader binaryReader, WzObject parent) { // 不要设置parsed 和 changed
        super(name, WzType.IMAGE, parent);
        reader = binaryReader;
    }

    public boolean parse() {
        return parse(true);
    }

    public synchronized boolean parse(boolean realParse) {
        if (status == WzFileStatus.PARSE_SUCCESS) {
            return true;
        } else if (changed) {
            status = WzFileStatus.PARSE_SUCCESS;
            return true;
        }

        if (!realParse) return true;
        reader.setPosition(offset);
        byte b = reader.getByte();
        if (b != withoutOffsetFlag) {
            status = WzFileStatus.ERROR_SPECIAL_ENCODE;
            return false;
        }

        if (!reader.readString().equals("Property") || reader.getShort() != 0) {
            status = WzFileStatus.ERROR_KEY;
            return false;
        }

        try {
            children.add(WzImageProperty.parsePropertyList(offset, reader, this));
            status = WzFileStatus.PARSE_SUCCESS;
            return true;
        } catch (Exception e) {
            log.error("WzImage 解析错误 : {}", name);
            return false;
        }
    }

    public void unparse() {
        children.clear();
        status = WzFileStatus.UNPARSE;
    }

    public void save(Path path) {
        if (path == null) return;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toString(), "rw")) {
            BinaryWriter writer = new BinaryWriter();
            writer.setWzMutableKey(reader.getWzMutableKey());
            save(writer);

            randomAccessFile.write(writer.output());
        } catch (IOException e) {
            throw new IllegalArgumentException("无法保存文件", e);
        }
    }

    public void exportToXml(Path path, int indent, boolean exportMedia) {
        boolean parseStatus = status == WzFileStatus.PARSE_SUCCESS;
        if (!parseStatus) {
            if (!parse()) {
                log.error("解析文件失败");
                return;
            }
        }
        XmlExport.export(this, path, indent, exportMedia);
        if (!parseStatus) unparse();
    }

    public void save(BinaryWriter writer) {
        if (changed) {
            // if (reader != null && !isParsed()) parse(); // 有修改说明已经解析了？
            int startPos = writer.getPosition();
            WzListProperty imgProp = new WzListProperty(children.get());
            imgProp.writeValue(writer);
            writer.getStringCache().clear();
            dataSize = writer.getPosition() - startPos;
        } else {
            int pos = reader.getPosition();
            reader.setPosition(offset);
            writer.putBytes(reader.getBytes(dataSize));
            reader.setPosition(pos);
        }
    }

    public static void writeListValue(BinaryWriter writer, List<WzImageProperty> properties) {
        writer.putShort((short) 0);
        writer.writeCompressedInt(properties.size());
        for (WzImageProperty property : properties) {
            writer.writeStringBlock(property.getName(), 0x00, 0x01);
            if (property instanceof WzExtended) { // "imgdir(WzList)", "canvas", "vector", "convex", "sound(WzBinary)", "uol", "(WzRawData)"
                writeExtendedValue(writer, (WzExtended) property);
            } else {
                property.writeValue(writer);
            }
        }
    }

    private static void writeExtendedValue(BinaryWriter writer, WzExtended property) {
        writer.putByte((byte) 9);
        int originPosition = writer.getPosition();
        writer.putInt(0); // size 预留
        property.writeValue(writer);
        int newPosition = writer.getPosition();
        int len = newPosition - originPosition;
        writer.setPosition(originPosition);
        writer.putInt(len - 4);
        writer.setPosition(newPosition);
    }

    public void addChecksum(byte value) {
        checksum += value;
    }

    public void changeKey(byte[] iv, byte[] key) {
        // 先解析把原有内容解码出来缓存在内存里
        if (!parse()) {
            log.error("文件 {} 解析失败", name);
            return;
        }
        changed = true; // 确保保存的时候重新写入，而不是取原来的
        reader.setWzMutableKey(new WzMutableKey(iv, key));
    }

    // DeepClone -------------------------------------------------------------------------------------------------------
    public WzImage deepClone(WzObject parent) {
        if (!parse()) {
            log.error("文件 {} 解析失败", name);
            throw new RuntimeException();
        }
        WzImage clone = new WzImage(getName(), parent, reader);
        for (WzImageProperty property : children.get()) {
            clone.addChild(property.deepClone(clone));
        }
        return clone;
    }

    // Children --------------------------------------------------------------------------------------------------------
    public WzImageProperty getChild(String name) {
        return children.get(name);
    }

    public List<WzImageProperty> getChildren() {
        return children.get();
    }

    public boolean addChild(WzImageProperty child) {
        return children.add(child);
    }

    public void addChildren(List<WzImageProperty> children) {
        this.children.add(children);
    }

    public boolean removeChild(String name) {
        return children.remove(name);
    }

    public boolean existChild(String name) {
        return children.existChild(name);
    }
}
