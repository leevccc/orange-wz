package orange.wz.provider;

import orange.wz.provider.properties.WzExtended;
import orange.wz.provider.properties.WzListProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class WzImage extends WzObject {
    private boolean parsed = false;
    private boolean changed = false;
    private int size;
    private int checksum;
    private int offset;
    private BinaryReader reader;
    private final List<WzImageProperty> properties = new ArrayList<>();
    private int tempFileStart;
    private int tempFileEnd;

    // 以下字段只为 img 文件使用
    private String path;
    private byte[] iv;
    private byte[] key;
    private boolean loaded = true;

    public static final int wzImageHeaderByte_WithOffset = 0x1B;
    public static final int wzImageHeaderByte_WithoutOffset = 0x73;

    public WzImage(String name, WzObject parent) {
        super.setName(name);
        super.setParent(parent);
        parsed = true;
        changed = true;
    }

    public WzImage(String name, BinaryReader reader) { // 不要设置parsed 和 changed
        this.reader = reader;
        super.setName(name);
    }

    public WzImage(String name, String path, byte[] iv, byte[] key) {
        setName(name);
        this.reader = null;
        this.path = path;
        this.iv = iv;
        this.key = key;
        this.loaded = false;
    }

    public void parse() {
        if (!loaded) {
            reader = new BinaryReader(path, iv, key);
            size = reader.getBuffer().capacity();
            checksum = 0;
            byte[] bytes = reader.output();
            for (byte b : bytes) {
                checksum += b;
            }
            offset = 0;
        } else if (parsed) {
            return;
        } else if (changed) {
            parsed = true;
            return;
        }

        reader.setPosition(offset);
        byte b = reader.getByte();
        if (b != wzImageHeaderByte_WithoutOffset || !reader.readString().equals("Property") || reader.getShort() != 0) {
            return;
        }

        properties.addAll(WzImageProperty.parsePropertyList(offset, reader, this));
        parsed = true;
    }

    public void unparse() {
        properties.clear();
        parsed = false;
    }

    public void save(Path path) {
        if (path == null) return;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toString(), "rw")) {
            BinaryWriter writer = new BinaryWriter();
            writer.setWzKey(reader.getWzKey());
            save(writer);

            randomAccessFile.write(writer.output());
        } catch (IOException e) {
            throw new IllegalArgumentException("无法保存文件", e);
        }
    }

    public void exportToXml(Path path, boolean indent) {
        boolean parseStatus = parsed;
        if (!parseStatus) parse();
        XmlExport.export(this, path, indent, false);
        if (!parseStatus) unparse();
    }

    public void save(BinaryWriter writer) {
        if (changed) {
            // if (reader != null && !isParsed()) parse(); // 有修改说明已经解析了？
            WzListProperty imgProp = WzListProperty.builder().build();
            int startPos = writer.getPosition();
            imgProp.addProperties(properties);
            imgProp.writeValue(writer);
            writer.getStringCache().clear();
            size = writer.getPosition() - startPos;
        } else {
            int pos = reader.getPosition();
            reader.setPosition(offset);
            writer.putBytes(reader.getBytes(size));
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
        parse(); // 先解析把原有内容解码出来缓存在内存里
        changed = true; // 确保保存的时候重新写入，而不是取原来的
        reader.setIv(iv);
        reader.setUserKey(key);
        reader.setWzKey(reader.generateWzKey());
    }

    public WzImageProperty get(String name) {
        for (WzImageProperty property : properties) {
            if (property.getName().equals(name)) return property;
        }
        return null;
    }

    public WzImage deepClone(WzObject parent) {
        parse();
        WzImage clone = new WzImage(getName(), parent);
        for (WzImageProperty property : properties) {
            clone.properties.add(property.deepClone(clone));
        }
        return clone;
    }
}
