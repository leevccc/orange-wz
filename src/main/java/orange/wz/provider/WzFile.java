package orange.wz.provider;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class WzFile extends WzObject {
    private final String path;
    private WzDirectory wzDirectory;
    private WzHeader header;
    private short version; // 实际版本
    private int versionHash = 0; // uint
    private short fileVersion; // 指定版本
    private byte[] wzIv;
    private byte[] userKey;
    private boolean parse = false;

    public WzFile(String filePath, short fileVersion, byte[] iv, byte[] key) {
        path = filePath;
        super.setName(Path.of(path).getFileName().toString());
        this.fileVersion = fileVersion;
        wzIv = iv;
        userKey = key;
    }

    /**
     * 如果你是要创建一个新的Wz文件，用这个方法初始化对象
     */
    public void initialize() {
        wzDirectory = new WzDirectory(getName(), this);
        BinaryReader reader = new BinaryReader(wzIv, userKey);
        wzDirectory.setReader(reader);
        header = WzHeader.getDefault();
        createWZVersionHash();
        parse = true;
    }

    private void createWZVersionHash() {
        int versionHash = 0;

        // mapleStoryPatchVersion 假设是 int 或其他可转字符串的类型
        String ver = String.valueOf(fileVersion);

        for (char ch : ver.toCharArray()) {
            versionHash = (versionHash * 32) + (byte) ch + 1;
        }

        byte wzVersionHeader = (byte) ~((versionHash >> 24) & 0xFF ^ (versionHash >> 16) & 0xFF ^ (versionHash >> 8) & 0xFF ^ versionHash & 0xFF);

        this.versionHash = versionHash;
        this.version = wzVersionHeader;
    }

    public void parse() {
        if (parse) return;

        if (path == null || !path.endsWith(".wz")) {
            log.error("路径为空或者不是wz文件");
            return;
        }

        Path wzPath = Path.of(path);

        if (!Files.exists(wzPath) || !Files.isRegularFile(wzPath)) {
            log.error("wz 文件不存在: {}", wzPath);
            return;
        }

        BinaryReader reader = new BinaryReader(wzPath.toString(), wzIv, userKey);
        if (reader.getBuffer() == null) {
            log.error("加载 wz 失败: {}", wzPath);
            return;
        }

        header = new WzHeader();
        header.setIdent(reader.readString(4));
        header.setSize(reader.getLong());
        header.setStart(reader.getInt());
        // header.setCopyright(reader.getString(header.getStart() - 17));
        header.setCopyright(reader.readNullTerminatedString());
        reader.setPosition(header.getStart());
        reader.setHeader(header);
        version = reader.getShort();

        versionHash = getVersionHash(version, fileVersion);
        reader.setHash(versionHash);
        WzDirectory directory = new WzDirectory(reader, super.getName(), versionHash, wzIv, this);
        directory.parse();
        wzDirectory = directory;
        parse = true;
    }

    private int getVersionHash(short encryptedVersion, int realVersion) {
        int encryptedVersionNumber = encryptedVersion;
        int versionNumber = realVersion;
        int versionHash = 0;
        int decryptedVersionNumber;
        String versionNumberString;
        int a, b, c, d, l;
        versionNumberString = String.valueOf(versionNumber);
        l = versionNumberString.length();
        for (int i = 0; i < l; i++) {
            versionHash = (32 * versionHash) + (int) versionNumberString.charAt(i) + 1;
        }
        a = (versionHash >> 24) & 0xFF;
        b = (versionHash >> 16) & 0xFF;
        c = (versionHash >> 8) & 0xFF;
        d = versionHash & 0xFF;
        decryptedVersionNumber = 0xFF ^ a ^ b ^ c ^ d;
        if (encryptedVersionNumber == decryptedVersionNumber) {
            return versionHash;
        } else {
            log.error("文件版本错误");
            return 0;
        }
    }

    public void save() {
        save(path);
    }

    public void save(String path) {
        log.info("保存 {} 开始", getName());
        createVersionHash();
        wzDirectory.setVersionHash(versionHash);
        String saveFile = Path.of(path).toString();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(saveFile, "rw")) {
            Map<String, Integer> tempStringCache = new HashMap<>();
            BinaryWriter tempWriter = new BinaryWriter();
            log.info("保存 {} Generate Data File 1/4", getName());
            wzDirectory.generateDataFile(tempWriter, tempStringCache);
            tempStringCache.clear();
            int totalLen = wzDirectory.getImgOffsets(wzDirectory.getOffsets(header.getStart() + 2));
            BinaryWriter writer = new BinaryWriter(true);
            writer.setWzKey(wzDirectory.getReader().getWzKey());
            writer.setHash(versionHash);
            header.setSize(totalLen - header.getStart());
            for (int i = 0; i < 4; i++) {
                writer.putByte((byte) header.getIdent().charAt(i));
            }
            writer.putLong(header.getSize());
            writer.putInt(header.getStart());
            writer.putAsciiString(header.getCopyright());
            long extraHeaderLength = header.getStart() - writer.getPosition();
            if (extraHeaderLength > 0) {
                writer.putBytes(new byte[(int) extraHeaderLength]);
            }
            writer.putShort(version);
            writer.setHeader(header);
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

    private void createVersionHash() {
        versionHash = 0;

        for (final byte c : String.valueOf(fileVersion).getBytes()) {
            versionHash = (versionHash * 32) + c + 1;
        }
        version = (short) (0xFF
                ^ ((versionHash >> 24) & 0xFF)
                ^ ((versionHash >> 16) & 0xFF)
                ^ ((versionHash >> 8) & 0xFF)
                ^ (versionHash & 0xFF));
    }

    public void exportFileToImg(Path basePath) {
        wzDirectory.exportDirectory(basePath);
    }

    public void exportFileToXml(Path basePath, boolean indent) {
        wzDirectory.exportToXml(basePath, indent);
    }

    public void changeKey(short gameVersion, byte[] iv, byte[] key) {
        parse(); // 先解析把原有内容解码出来缓存在内存里
        wzDirectory.parseAll();
        BinaryReader reader = wzDirectory.getReader();
        wzIv = iv;
        userKey = key;
        reader.setIv(wzIv);
        reader.setUserKey(userKey);
        reader.setWzKey(reader.generateWzKey());
        fileVersion = gameVersion;
    }
}
