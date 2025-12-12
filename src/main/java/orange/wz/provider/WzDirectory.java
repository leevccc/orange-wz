package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.exception.BizException;
import orange.wz.exception.ExceptionEnum;
import orange.wz.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class WzDirectory extends WzObject {
    private final LinkedHashMap<String, WzImage> images = new LinkedHashMap<>();
    private final LinkedHashMap<String, WzDirectory> directories = new LinkedHashMap<>();
    private BinaryReader reader;
    private int offset;
    private int hash;
    private int size;
    private int checksum;
    private int offsetSize;
    private byte[] wzIv;
    private WzFile wzFile;

    public WzDirectory(String name, WzObject parent) {
        super.setName(name);
        super.setParent(parent);
    }

    public WzDirectory(BinaryReader reader, String name, int hash, byte[] wzIv, WzFile wzFile) {
        this.reader = reader;
        super.setName(name);
        this.hash = hash;
        this.wzIv = wzIv;
        this.wzFile = wzFile;
    }

    public void parse() {
        int entryCount = reader.readCompressedInt();
        for (int i = 0; i < entryCount; i++) {
            byte type = reader.getByte();
            String fname = null;
            int fSize;
            int checksum;
            int offset;

            int rememberPos = 0;
            switch (WzDirectoryType.getByValue(type)) {
                case WzDirectoryType.UnknownType_1:   // 01 XX 00 00 00 00 00 OFFSET (4 bytes)
                    int unknown = reader.getInt();
                    reader.getShort();
                    int offs = reader.readOffset();
                    continue;
                case WzDirectoryType.RetrieveStringFromOffset_2:
                    int stringOffset = reader.getInt();
                    rememberPos = reader.getPosition();
                    reader.setPosition(reader.getHeader().getStart() + stringOffset);
                    type = reader.getByte();
                    fname = reader.readString();
                    break;
                case WzDirectoryType.WzDirectory_3:
                case WzDirectoryType.WzImage_4:
                    fname = reader.readString();
                    rememberPos = reader.getPosition();
                    break;
                case null:
                default:
                    throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "[WzDirectory] 未知类型 = " + type);
            }
            reader.setPosition(rememberPos);
            fSize = reader.readCompressedInt();
            checksum = reader.readCompressedInt();
            offset = reader.readOffset();
            if (WzDirectoryType.getByValue(type) == WzDirectoryType.WzDirectory_3) {
                WzDirectory subDir = new WzDirectory(reader, fname, hash, wzIv, wzFile);
                subDir.setSize(fSize);
                subDir.setChecksum(checksum);
                subDir.setOffset(offset);
                subDir.setParent(this);
                directories.put(fname, subDir);
            } else {
                WzImage img = new WzImage(fname, reader);
                img.setSize(fSize);
                img.setChecksum(checksum);
                img.setOffset(offset);
                img.setParent(this);
                images.put(fname, img);
            }
        }

        for (WzDirectory dir : directories.values()) {
            reader.setPosition(dir.getOffset());
            dir.parse();
        }
    }

    public void setVersionHash(int versionHash) {
        hash = versionHash;
        directories.forEach((k, v) -> v.setVersionHash(versionHash));
    }

    public void saveImages(BinaryWriter writer, BinaryWriter tempWriter) {
        for (WzImage img : images.values()) {
            if (img.isChanged()) {
                tempWriter.setPosition(img.getTempFileStart());
                byte[] buffer = tempWriter.getBytes(img.getSize());
                writer.putBytes(buffer);
            } else {
                img.getReader().setPosition(img.getTempFileStart());
                writer.putBytes(img.getReader().getBytes(img.getTempFileEnd() - img.getTempFileStart()));
            }
        }
        for (WzDirectory dir : directories.values()) {
            dir.saveImages(writer, tempWriter);
        }
    }

    public int generateDataFile(BinaryWriter tempWriter, Map<String, Integer> tempStringCache) {
        size = 0;
        int entryCount = directories.size() + images.size();
        if (entryCount == 0) {
            offsetSize = 1;
            return 0;
        }
        size = WzTool.getCompressedIntLength(entryCount);
        offsetSize = WzTool.getCompressedIntLength(entryCount);

        BinaryWriter imgWriter;
        for (WzImage img : images.values()) {
            if (img.isChanged()) {
                imgWriter = new BinaryWriter();
                imgWriter.setWzMutableKey(reader.getWzMutableKey());
                img.save(imgWriter);
                img.setChecksum(0);
                byte[] data = imgWriter.output();
                for (byte b : data) {
                    img.addChecksum(b);
                }
                img.setTempFileStart(tempWriter.getPosition());
                tempWriter.putBytes(data);
                img.setTempFileEnd(tempWriter.getPosition());
            } else {
                img.setTempFileStart(img.getOffset());
                img.setTempFileEnd(img.getOffset() + img.getSize());
            }

            int nameLen = WzTool.getWzObjectValueLength(img.getName(), (byte) 4, tempStringCache);
            size += nameLen;
            int imgLen = img.getSize();
            size += WzTool.getCompressedIntLength(imgLen);
            size += imgLen;
            size += WzTool.getCompressedIntLength(img.getChecksum());
            size += 4;
            offsetSize += nameLen;
            offsetSize += WzTool.getCompressedIntLength(imgLen);
            offsetSize += WzTool.getCompressedIntLength(img.getChecksum());
            offsetSize += 4;
        }

        for (WzDirectory dir : directories.values()) {
            int nameLen = WzTool.getWzObjectValueLength(dir.getName(), (byte) 3, tempStringCache);
            size += nameLen;
            size += dir.generateDataFile(tempWriter, tempStringCache);
            size += WzTool.getCompressedIntLength(dir.getSize());
            size += WzTool.getCompressedIntLength(dir.getChecksum());
            size += 4;
            offsetSize += nameLen;
            offsetSize += WzTool.getCompressedIntLength(dir.getSize());
            offsetSize += WzTool.getCompressedIntLength(dir.getChecksum());
            offsetSize += 4;
        }

        return size;
    }

    public int getOffsets(int curOffset) {
        offset = curOffset;
        curOffset += offsetSize;

        for (WzDirectory dir : directories.values()) {
            curOffset = dir.getOffsets(curOffset);
        }

        return curOffset;
    }

    public int getImgOffsets(int curOffset) {
        for (WzImage img : images.values()) {
            img.setOffset(curOffset);
            curOffset += img.getSize();
        }

        for (WzDirectory dir : directories.values()) {
            curOffset = dir.getImgOffsets(curOffset);
        }

        return curOffset;
    }

    public void saveDirectory(BinaryWriter writer) {
        offset = writer.getPosition();
        int entryCount = directories.size() + images.size();
        if (entryCount == 0) {
            size = 0;
            return;
        }
        writer.writeCompressedInt(entryCount);
        for (WzImage img : images.values()) {
            writer.writeWzObjectValue(img.getName(), WzDirectoryType.WzImage_4);
            writer.writeCompressedInt(img.getSize());
            writer.writeCompressedInt(img.getChecksum());
            writer.writeOffset(img.getOffset());
        }
        for (WzDirectory dir : directories.values()) {
            writer.writeWzObjectValue(dir.getName(), WzDirectoryType.WzDirectory_3);
            writer.writeCompressedInt(dir.getSize());
            writer.writeCompressedInt(dir.getChecksum());
            writer.writeOffset(dir.getOffset());
        }

        for (WzDirectory dir : directories.values()) {
            if (dir.getSize() > 0) {
                dir.saveDirectory(writer);
            } else {
                writer.putByte((byte) 0);
            }
        }
    }

    public void exportDirectory(Path parentPath) {
        String name = getName().replaceAll("(?i)\\.wz$", "");
        Path p = parentPath.resolve(name);
        try {
            FileUtils.createNewDirectory(p);
        } catch (IOException e) {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "目录操作失败: " + p + ", " + e.getMessage());
        }

        directories.forEach((s, directory) -> directory.exportDirectory(p));

        images.forEach((s, image) -> image.save(p.resolve(image.getName())));
    }

    public void exportToXml(Path parentPath, boolean indent) {
        Path p = parentPath.resolve(getName());
        try {
            FileUtils.createNewDirectory(p);
        } catch (IOException e) {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "目录操作失败: " + p + ", " + e.getMessage());
        }

        directories.forEach((s, directory) -> directory.exportToXml(p, indent));

        images.forEach((s, image) -> image.exportToXml(p.resolve(image.getName() + ".xml"), indent));
    }

    public void parseAll() {
        directories.forEach((s, directory) -> directory.parseAll());
        images.forEach((s, image) -> {
            image.parse();
            image.setChanged(true); // 确保保存的时候重新写入，而不是取原来的
        });
    }

    public WzDirectory deepClone(WzObject parent) {
        WzDirectory clone = new WzDirectory(getName(), parent);
        for (WzDirectory wzDirectory : directories.values()) {
            clone.directories.put(wzDirectory.getName(), wzDirectory.deepClone(clone));
        }
        for (WzImage wzImage : images.values()) {
            clone.images.put(wzImage.getName(), wzImage.deepClone(clone));
        }
        return clone;
    }
}
