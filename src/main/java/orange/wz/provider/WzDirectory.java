package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.exception.BizException;
import orange.wz.exception.ExceptionEnum;
import orange.wz.provider.tools.*;
import orange.wz.provider.tools.FileTool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class WzDirectory extends WzObject {
    private final WzChildrenDirectory children = new WzChildrenDirectory();
    private int offset;
    private int dataSize;
    private int checksum; // reader 所有 bytes 值的和 todo 计算
    private int offsetSize;
    private WzFile wzFile;

    public WzDirectory(String name, WzObject parent, WzFile file) {
        super(name, WzType.DIRECTORY, parent);
        wzFile = file;
    }

    /**
     * 该方法用于 GUI 判断 dir是代表 wzFile 的 dir 还是 subDir
     *
     * @return 该对象是 WzFile 的参数 WzDirectory 的值
     */
    public boolean isWzFile() {
        return wzFile != null && wzFile == parent;
    }

    public void parse(BinaryReader reader) {
        int entryCount = reader.readCompressedInt();
        for (int i = 0; i < entryCount; i++) {
            byte type = reader.getByte();
            String fname;
            int fSize;
            int checksum;
            int offset;

            int rememberPos = 0;
            switch (WzDirectoryType.getByValue(type)) {
                case WzDirectoryType.UnknownType:   // 01 XX 00 00 00 00 00 OFFSET (4 bytes)
                    int unknown = reader.getInt();
                    reader.getShort();
                    int offs = reader.readOffset(wzFile.getHeader().getDataStartPos(), wzFile.getHeader().getVersionHash());
                    continue;
                case WzDirectoryType.RetrieveStringFromOffset:
                    int stringOffset = reader.getInt();
                    rememberPos = reader.getPosition();
                    reader.setPosition(wzFile.getHeader().getDataStartPos() + stringOffset);
                    type = reader.getByte();
                    fname = reader.readString();
                    break;
                case WzDirectoryType.WzDirectory:
                case WzDirectoryType.WzImage:
                    fname = reader.readString();
                    rememberPos = reader.getPosition();
                    break;
                case null:
                default:
                    throw new RuntimeException("[WzDirectory] 未知类型 = " + type);
            }
            reader.setPosition(rememberPos);
            fSize = reader.readCompressedInt();
            checksum = reader.readCompressedInt();
            offset = reader.readOffset(wzFile.getHeader().getDataStartPos(), wzFile.getHeader().getVersionHash());
            if (WzDirectoryType.getByValue(type) == WzDirectoryType.WzDirectory) {
                WzDirectory subDir = new WzDirectory(fname, this, wzFile);
                subDir.setDataSize(fSize);
                subDir.setChecksum(checksum);
                subDir.setOffset(offset);
                children.add(subDir);
            } else {
                WzImage img = new WzImage(fname, reader, this);
                img.setDataSize(fSize);
                img.setChecksum(checksum);
                img.setOffset(offset);
                children.add(img);
            }
        }

        for (WzDirectory dir : children.getDirectories()) {
            reader.setPosition(dir.getOffset());
            dir.parse(reader);
        }
    }

    public void saveImages(BinaryWriter writer, BinaryWriter tempWriter) {
        for (WzImage img : children.getImages()) {
            if (img.isChanged()) {
                tempWriter.setPosition(img.getTempFileStart());
                byte[] buffer = tempWriter.getBytes(img.getDataSize());
                writer.putBytes(buffer);
            } else {
                img.getReader().setPosition(img.getTempFileStart());
                writer.putBytes(img.getReader().getBytes(img.getTempFileEnd() - img.getTempFileStart()));
            }
        }
        for (WzDirectory dir : children.getDirectories()) {
            dir.saveImages(writer, tempWriter);
        }
    }

    public int generateDataFile(BinaryWriter tempWriter, Map<String, Integer> tempStringCache) {
        dataSize = 0;
        int entryCount = children.getEntryCount();
        if (entryCount == 0) {
            offsetSize = 1;
            return 0;
        }
        dataSize = WzTool.getCompressedIntLength(entryCount);
        offsetSize = WzTool.getCompressedIntLength(entryCount);

        BinaryWriter imgWriter;
        for (WzImage img : children.getImages()) {
            if (img.isChanged()) {
                imgWriter = new BinaryWriter();
                imgWriter.setWzMutableKey(wzFile.getReader().getWzMutableKey());
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
                img.setTempFileEnd(img.getOffset() + img.getDataSize());
            }

            int nameLen = WzTool.getWzObjectValueLength(img.getName(), (byte) 4, tempStringCache);
            dataSize += nameLen;
            int imgLen = img.getDataSize();
            dataSize += WzTool.getCompressedIntLength(imgLen);
            dataSize += imgLen;
            dataSize += WzTool.getCompressedIntLength(img.getChecksum());
            dataSize += 4;
            offsetSize += nameLen;
            offsetSize += WzTool.getCompressedIntLength(imgLen);
            offsetSize += WzTool.getCompressedIntLength(img.getChecksum());
            offsetSize += 4;
        }

        for (WzDirectory dir : children.getDirectories()) {
            int nameLen = WzTool.getWzObjectValueLength(dir.getName(), (byte) 3, tempStringCache);
            dataSize += nameLen;
            dataSize += dir.generateDataFile(tempWriter, tempStringCache);
            dataSize += WzTool.getCompressedIntLength(dir.getDataSize());
            dataSize += WzTool.getCompressedIntLength(dir.getChecksum());
            dataSize += 4;
            offsetSize += nameLen;
            offsetSize += WzTool.getCompressedIntLength(dir.getDataSize());
            offsetSize += WzTool.getCompressedIntLength(dir.getChecksum());
            offsetSize += 4;
        }

        return dataSize;
    }

    public int getOffsets(int curOffset) {
        offset = curOffset;
        curOffset += offsetSize;

        for (WzDirectory dir : children.getDirectories()) {
            curOffset = dir.getOffsets(curOffset);
        }

        return curOffset;
    }

    public int getImgOffsets(int curOffset) {
        for (WzImage img : children.getImages()) {
            img.setOffset(curOffset);
            curOffset += img.getDataSize();
        }

        for (WzDirectory dir : children.getDirectories()) {
            curOffset = dir.getImgOffsets(curOffset);
        }

        return curOffset;
    }

    public void saveDirectory(BinaryWriter writer) {
        offset = writer.getPosition();
        int entryCount = children.getEntryCount();
        if (entryCount == 0) {
            dataSize = 0;
            return;
        }
        writer.writeCompressedInt(entryCount);
        for (WzImage img : children.getImages()) {
            writer.writeWzObjectValue(img.getName(), WzDirectoryType.WzImage, wzFile.getHeader().getDataStartPos());
            writer.writeCompressedInt(img.getDataSize());
            writer.writeCompressedInt(img.getChecksum());
            writer.writeOffset(img.getOffset(), wzFile.getHeader().getDataStartPos(), wzFile.getHeader().getVersionHash());
        }
        for (WzDirectory dir : children.getDirectories()) {
            writer.writeWzObjectValue(dir.getName(), WzDirectoryType.WzDirectory, wzFile.getHeader().getDataStartPos());
            writer.writeCompressedInt(dir.getDataSize());
            writer.writeCompressedInt(dir.getChecksum());
            writer.writeOffset(dir.getOffset(), wzFile.getHeader().getDataStartPos(), wzFile.getHeader().getVersionHash());
        }

        for (WzDirectory dir : children.getDirectories()) {
            if (dir.getDataSize() > 0) {
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
            FileTool.createDirectory(p);
        } catch (IOException e) {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "目录操作失败: " + p + ", " + e.getMessage());
        }

        children.getDirectories().forEach(directory -> directory.exportDirectory(p));
        children.getImages().forEach(image -> image.save(p.resolve(image.getName())));
    }

    public void exportToXml(Path parentPath, int indent, MediaExportType meType) {
        Path p = parentPath.resolve(getName());
        try {
            FileTool.createDirectory(p);
        } catch (IOException e) {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR, "目录操作失败: " + p + ", " + e.getMessage());
        }

        children.getDirectories().forEach(directory -> directory.exportToXml(p, indent, meType));
        children.getImages().forEach(image -> image.exportToXml(p.resolve(image.getName() + ".xml"), indent, meType));
    }

    public void parseAllImages() {
        children.getDirectories().forEach(WzDirectory::parseAllImages);
        children.getImages().forEach(image -> {
            if (!image.parse()) {
                log.error("文件 {} 解析失败", name);
                throw new RuntimeException();
            }
            image.setChanged(true); // 确保保存的时候重新写入，而不是取原来的
        });
    }

    // DeepClone -------------------------------------------------------------------------------------------------------
    public WzDirectory deepClone(WzObject parent) {
        WzDirectory clone = new WzDirectory(getName(), parent, null);
        for (WzDirectory wzDirectory : children.getDirectories()) {
            clone.addChild(wzDirectory.deepClone(clone));
        }
        for (WzImage wzImage : children.getImages()) {
            clone.addChild(wzImage.deepClone(clone));
        }
        return clone;
    }

    // Children --------------------------------------------------------------------------------------------------------
    public WzDirectory getDirectory(String name) {
        return children.getDirectory(name);
    }

    public WzImage getImage(String name) {
        return children.getImage(name);
    }

    public List<WzDirectory> getDirectories() {
        return children.getDirectories();
    }

    public List<WzImage> getImages() {
        return children.getImages();
    }

    public List<WzObject> getChildren() {
        return children.getAllChildren();
    }

    public boolean addChild(WzDirectory directory) {
        return children.add(directory);
    }

    public boolean addChild(WzImage image) {
        return children.add(image);
    }

    public boolean removeDirectoryChild(String name) {
        return children.removeDirectory(name);
    }

    public boolean removeImageChild(String name) {
        return children.removeImage(name);
    }

    public boolean existDirectory(String name) {
        return children.existDirectory(name);
    }

    public boolean existImage(String name) {
        return children.existImage(name);
    }
}
