package orange.wz.provider;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzMutableKey;
import orange.wz.provider.tools.WzType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Getter
@Slf4j
public class WzFile extends WzObject {
    private final String filePath;
    private WzDirectory wzDirectory;
    private WzHeader header;
    private byte[] wzIv;
    private byte[] userKey;
    private BinaryReader reader;
    private boolean load = false;

    // 初始化 -----------------------------------------------------------------------------------------------------------
    public WzFile(String filePath, short fileVersion, byte[] iv, byte[] key) {
        super(Path.of(filePath).getFileName().toString(), WzType.WZ_FILE, null);
        this.filePath = filePath;
        header = new WzHeader(fileVersion);
        wzIv = iv;
        userKey = key;
    }

    public static WzFile createNewFile(String filePath, short fileVersion, byte[] iv, byte[] key) {
        WzFile wzFile = new WzFile(filePath, fileVersion, iv, key);

        wzFile.wzDirectory = new WzDirectory(wzFile.getName(), wzFile, wzFile);
        wzFile.header = WzHeader.getDefault(fileVersion);
        wzFile.load = true;
        return wzFile;
    }

    public WzMutableKey getWzMutableKey() {
        return reader.getWzMutableKey();
    }

    public void load() {
        if (load) return;

        if (filePath == null || !filePath.endsWith(".wz")) {
            log.error("路径为空或者不是wz文件");
            return;
        }

        Path wzPath = Path.of(filePath);

        if (!Files.exists(wzPath) || !Files.isRegularFile(wzPath)) {
            log.error("wz 文件不存在: {}", wzPath);
            return;
        }

        reader = new BinaryReader(wzPath.toString(), wzIv, userKey);
        header.setSignature(reader.readString(4));
        header.setFileSize(reader.getLong());
        header.setDataStartPos(reader.getInt());
        header.setCopyright(reader.readNullTerminatedString());
        reader.setPosition(header.getDataStartPos());
        header.setEncVersion(reader.getShort());
        header.verifiedFileVersion();

        wzDirectory = new WzDirectory(getName(), this, this);
        wzDirectory.parse(reader);
        load = true;
    }

    public void save() {
        save(filePath);
    }

    public void save(String path) {
        log.info("保存 {} 开始", getName());
        String saveFile = Path.of(path).toString();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(saveFile, "rw")) {
            Map<String, Integer> tempStringCache = new HashMap<>();
            BinaryWriter tempWriter = new BinaryWriter();
            log.info("保存 {} Generate Data File 1/4", getName());
            wzDirectory.generateDataFile(tempWriter, tempStringCache);
            tempStringCache.clear();
            int totalLen = wzDirectory.getImgOffsets(wzDirectory.getOffsets(header.getDataStartPos() + 2));
            BinaryWriter writer = new BinaryWriter(true);
            writer.setWzMutableKey(wzDirectory.getWzMutableKey());
            header.setFileSize(totalLen - header.getDataStartPos());
            for (int i = 0; i < 4; i++) {
                writer.putByte((byte) header.getSignature().charAt(i));
            }
            writer.putLong(header.getFileSize());
            writer.putInt(header.getDataStartPos());
            writer.putAsciiString(header.getCopyright());
            long extraHeaderLength = header.getDataStartPos() - writer.getPosition();
            if (extraHeaderLength > 0) {
                writer.putBytes(new byte[(int) extraHeaderLength]);
            }
            writer.putShort(header.getEncVersion());
            log.info("保存 {} Wz Dirs 2/4", getName());
            wzDirectory.saveDirectory(writer);
            writer.getStringCache().clear();
            log.info("保存 {} Wz Images 3/4", getName());
            wzDirectory.saveImages(writer, tempWriter);
            writer.getStringCache().clear();
            log.info("保存 {} Wz 写入文件 4/4", getName());
            randomAccessFile.write(writer.output());
            log.info("保存 {} Wz 完成", getName());
        } catch (IOException e) {
            throw new IllegalArgumentException("无法保存文件", e);
        }
    }

    public void exportFileToImg(Path basePath) {
        wzDirectory.exportDirectory(basePath);
    }

    public void exportFileToXml(Path basePath, boolean indent) {
        wzDirectory.exportToXml(basePath, indent);
    }

    public void changeKey(short gameVersion, byte[] iv, byte[] key) {
        load(); // 先解析把原有内容解码出来缓存在内存里
        wzDirectory.parseAllImages();
        wzIv = iv;
        userKey = key;
        reader.setWzMutableKey(new WzMutableKey(wzIv, userKey));
        header.setFileVersion(gameVersion);
    }
}
