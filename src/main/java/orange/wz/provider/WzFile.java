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
    private final WzDirectory wzDirectory;
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
        wzDirectory = new WzDirectory(name, this, this);
    }

    public static WzFile createNewFile(String filePath, short fileVersion, byte[] iv, byte[] key) {
        WzFile wzFile = new WzFile(filePath, fileVersion, iv, key);

        wzFile.header = WzHeader.getDefault(fileVersion);
        wzFile.reader = new BinaryReader(iv, key);
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
        short encVersion = reader.getShort();
        header.setEncVersion(encVersion);

        if (header.getFileVersion() == -1) {
            short TEST_VERSION_MAX = 2000;
            boolean checked = false;
            for (short testVersion = 0; testVersion < TEST_VERSION_MAX; testVersion++) {
                if (tryDecode(encVersion, testVersion)) {
                    // tryDecode 只会设置 versionHash 用于解密，这里的 wzDir 对象已经绑定到 JTree 里，不能在 tryDecode 重设，只能在这重解析
                    header.setFileVersion(testVersion);
                    wzDirectory.parse(reader);
                    load = true;
                    checked = true;
                    break;
                }
            }

            if (!checked) {
                throw new RuntimeException("文件版本错误");
            }
        } else {
            int versionHash = header.checkAndGetVersionHash(encVersion, header.getFileVersion());
            if (versionHash == 0) {
                throw new RuntimeException("文件版本错误");
            }
            header.setVersionHash(versionHash);
            wzDirectory.parse(reader);
            load = true;
        }
    }

    public void save() {
        save(filePath);
    }

    public void save(String path) {
        if (!load) return;
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
            writer.setWzMutableKey(getWzMutableKey());
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

    private boolean tryDecode(short encVersion, short fileVersion) {
        int versionHash = header.checkAndGetVersionHash(encVersion, fileVersion);
        if (versionHash == 0) return false;

        /*
        Hash 计算通过也不代表版本号是对的，还要靠计算出来的Hash去解码文件才能确认
        比如 95 计算出来的 encVer 和 16 计算出来的是一样的，但是 Hash 依然对不上，导致无法解码文件内容
        因此有了下面的二次确认
         */

        int originalHash = header.getVersionHash();
        int originalPosition = reader.getPosition();
        WzDirectory testDirectory = new WzDirectory(name, this, this);

        header.setVersionHash(versionHash);

        try {
            testDirectory.parse(reader);
        } catch (Exception e) {
            // log.error(e.getMessage());
            header.setVersionHash(originalHash);
            reader.setPosition(originalPosition);
            return false;
        }

        WzImage testImage = testDirectory.getImages().getFirst();
        if (testImage == null) { // todo 还有特别版本的需要处理
            reader.setPosition(originalPosition);
            return true;
        }
        try {
            reader.setPosition(testImage.getOffset());
            byte checkByte = reader.getByte();
            reader.setPosition(originalPosition);

            switch (checkByte) {
                case (byte) 0x73:
                case (byte) 0x1B:
                    reader.setPosition(originalPosition);
                    return true;
                case (byte) 0x30:
                case (byte) 0x6C:
                case (byte) 0xBC:
                default:
                    log.warn("发现新的 image 类型. checkByte = {}. 文件名 = {}", checkByte, name);
            }
        } catch (Exception e) {
            header.setVersionHash(originalHash);
            reader.setPosition(originalPosition);
            return false;
        }

        reader.setPosition(originalPosition);
        return false;
    }
}
