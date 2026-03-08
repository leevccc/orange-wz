package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.properties.WzCanvasProperty;
import orange.wz.provider.properties.WzExtended;
import orange.wz.provider.properties.WzListProperty;
import orange.wz.provider.tools.*;

import java.io.IOException;
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

    protected WzFileStatus status = WzFileStatus.UNPARSE;
    private boolean changed;
    private int tempFileStart;
    private int tempFileEnd;

    public static final int withLuaFlag = 0x1;
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

    public boolean isErrorStatus() {
        return status != WzFileStatus.UNPARSE && status != WzFileStatus.PARSE_SUCCESS;
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
        if (b == withLuaFlag) {
            if (!name.endsWith(".lua")) {
                status = WzFileStatus.ERROR_SPECIAL_ENCODE;
                return false;
            }

            children.add(WzImageProperty.parseLuaProperty(reader, this));
            status = WzFileStatus.PARSE_SUCCESS;
            return true;
        } else if (b != withoutOffsetFlag) {
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

    public void clear() {
        getChildren().forEach(WzImageProperty::clear);
        parent = null;
        reader = null;
        unparse();
    }

    public boolean save(Path path) {
        if (path == null) return false;
        boolean parseStatus = status == WzFileStatus.PARSE_SUCCESS;
        if (!parseStatus) {
            if (!parse()) {
                log.error("解析文件失败");
                return false;
            }
        }

        try {
            BinaryWriter writer = new BinaryWriter();
            writer.setWzMutableKey(reader.getWzMutableKey());
            save(writer);

            byte[] context = writer.output();
            if (this instanceof WzImageFile) {
                clear();
            }
            String filePath = path.toString();
            Path savePath = Path.of(filePath + ".bak");
            if (FileTool.saveFile(savePath, context)) {
                for (int i = 0; i < 10; i++) {
                    try {
                        FileTool.moveAndReplace(savePath, Path.of(filePath));
                        log.info("{} 已保存", getName());
                        return true;
                    } catch (IOException e) {
                        if (i == 0) {
                            System.gc();
                        } else if (i == 9) {
                            log.error("{} 替换 {} 失败: {}", savePath, Path.of(filePath), e.getMessage());
                        } else {
                            log.warn("{} 处于被占用的状态，如果你运行的游戏客户端在使用该文件，请立刻关闭。第 {}/10 次尝试", getName(), i + 1);
                        }
                    }
                    Thread.sleep(500);
                }

                log.warn("由于文件处于占用状态，已经尝试了10次均无法写入，已将 {} 保存为 {}.bak", getName(), getName());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("保存出错 Img: {} 错误消息: {}", getName(), e.getMessage());
            return false;
        }
    }

    public boolean exportToXml(Path path, int indent, MediaExportType mediaExportType) {
        boolean parseStatus = status == WzFileStatus.PARSE_SUCCESS;
        if (!parseStatus) {
            if (!parse()) {
                log.error("解析文件失败");
                return false;
            }
        }
        if (!XmlExport.export(this, path, indent, mediaExportType)) {
            return false;
        }
        if (!parseStatus) unparse();
        return true;
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
            log.debug("writeListValue: {}", property.getPath());
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
        checksum += Byte.toUnsignedInt(value);
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
        return addChild(child, false);
    }

    public boolean addChild(WzImageProperty child, boolean isParseXml) {
        if (children.add(child)) {
            if (!isParseXml) {
                setChanged(true);
                setTempChanged(true);
            }
            return true;
        }
        return false;
    }

    public boolean removeChild(String name) {
        if (children.remove(name)) {
            setChanged(true);
            setTempChanged(true);
            return true;
        }
        return false;
    }

    public boolean existChild(String name) {
        return children.existChild(name);
    }

    public void setChildrenWzImage() {
        for (WzImageProperty property : children.get()) {
            property.setWzImage(this);
            property.setChildrenWzImage(this);
        }
    }

    public int removeAllChildWithName(String name) {
        int count = 0;

        for (WzImageProperty child : children.get()) {
            if (child.getName().equals(name)) {
                count += removeChild(name) ? 1 : 0;
            } else {
                count += child.removeAllChildWithName(name);
            }
        }

        return count;
    }

    // ChangeKey -------------------------------------------------------------------------------------------------------
    public void rebuildCompressedForPngBelongListWz(List<WzImageProperty> propertyList, WzMutableKey wzMutableKey) {
        for (WzImageProperty property : propertyList) {
            if (property.isListProperty()) {
                rebuildCompressedForPngBelongListWz(property.getChildren(), wzMutableKey);
            }
            if (property instanceof WzCanvasProperty canvas) {
                canvas.rebuildCompressedBytesUseNewWzKey(wzMutableKey);
            }
        }
    }

    // 工具方法 ---------------------------------------------------------------------------------------------------------
    public boolean sortAndReindexChildren() {
        List<WzImageProperty> list = children.get();

        boolean renamed = WzTool.sortAndReindexChildren(list);

        children.clear();
        children.add(list);
        setChanged(true);
        setTempChanged(true);
        return renamed;
    }
}
