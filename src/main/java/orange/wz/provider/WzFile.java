package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.model.Pair;
import orange.wz.provider.tools.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Slf4j
public final class WzFile extends WzObject implements WzSavableFile {
    @Setter
    private String filePath;
    private final WzDirectory wzDirectory;
    private WzHeader header;
    private String keyBoxName;
    private byte[] iv;
    private byte[] key;
    private BinaryReader reader;
    private WzFileStatus status = WzFileStatus.UNPARSE;

    private boolean withEncVerHeader = true;  // KMS update after Q4 2021, ver 1.2.357 does not contain any wz enc header information
    public static final short verHeader64BitStart = 770;

    // 初始化 -----------------------------------------------------------------------------------------------------------
    public WzFile(String filePath, short fileVersion, byte[] iv, byte[] key) {
        super(Path.of(filePath).getFileName().toString(), WzType.WZ_FILE, null);
        this.filePath = filePath;
        this.header = new WzHeader(fileVersion);
        this.iv = Arrays.copyOf(iv, iv.length);
        this.key = Arrays.copyOf(key, key.length);
        this.wzDirectory = new WzDirectory(name, this, this);
    }

    public WzFile(String filePath, short fileVersion, String keyBoxName, byte[] iv, byte[] key) {
        this(filePath, fileVersion, iv, key);
        this.keyBoxName = keyBoxName;
    }

    public static WzFile createNewFile(String filePath, short fileVersion, byte[] iv, byte[] key) {
        iv = Arrays.copyOf(iv, iv.length);
        key = Arrays.copyOf(key, key.length);

        WzFile wzFile = new WzFile(filePath, fileVersion, iv, key);

        wzFile.header = WzHeader.getDefault(fileVersion);
        wzFile.reader = new BinaryReader(iv, key);
        wzFile.status = WzFileStatus.PARSE_SUCCESS;
        return wzFile;
    }

    public static WzFile createNewFile(String filePath, short fileVersion, String keyBoxName, byte[] iv, byte[] key) {
        WzFile wzFile = createNewFile(filePath, fileVersion, iv, key);
        wzFile.keyBoxName = keyBoxName;
        return wzFile;
    }

    public boolean isErrorStatus() {
        return status != WzFileStatus.UNPARSE && status != WzFileStatus.PARSE_SUCCESS;
    }

    public boolean is64BitWzFile() {
        return !withEncVerHeader;
    }

    public WzMutableKey getWzMutableKey() {
        return reader.getWzMutableKey();
    }

    public boolean parse() {
        if (status == WzFileStatus.PARSE_SUCCESS) return true;

        if (filePath == null || !filePath.endsWith(".wz")) {
            log.error("路径为空或者不是wz文件");
            status = WzFileStatus.ERROR_PATH;
            return false;
        }

        Path wzPath = Path.of(filePath);

        if (!Files.exists(wzPath) || !Files.isRegularFile(wzPath)) {
            log.error("wz 文件不存在: {}", wzPath);
            status = WzFileStatus.ERROR_PATH;
            return false;
        }

        reader = new BinaryReader(wzPath.toString(), iv, key);
        header.setSignature(reader.readString(4));
        if (!header.getSignature().equals("PKG1") && !header.getSignature().equals("PKG2")) {
            log.error("{} 错误的包头 {}", name, header.getSignature());
            return false;
        }
        header.setFileSize(reader.getLong());
        header.setHeaderSize(reader.getInt());
        header.setCopyright(reader.readString(header.getDataStartPos() - reader.getPosition()));

        withEncVerHeader = check64BitClient();
        reader.setPosition(header.getDataStartPos());

        // the value of wzVersionHeader is less important. It is used for reading/writing from/to WzFile Header, and calculating the versionHash.
        // it can be any number if the client is 64-bit. Assigning 777 is just for convenience when calculating the versionHash.
        short encVersion = withEncVerHeader ? reader.getShort() : verHeader64BitStart;
        header.setEncVersion(encVersion);

        if (header.getFileVersion() == -1) {
            if (is64BitWzFile()) {
                for (short testVersion = verHeader64BitStart; testVersion < verHeader64BitStart + 10; testVersion++) { // 770 ~ 780
                    if (tryDecode(encVersion, testVersion)) {
                        header.setFileVersion(testVersion);
                        wzDirectory.parse(reader);
                        status = WzFileStatus.PARSE_SUCCESS;
                        return true;
                    }
                }
            }

            short TEST_VERSION_MAX = 2000;
            for (short testVersion = 0; testVersion < TEST_VERSION_MAX; testVersion++) {
                if (tryDecode(encVersion, testVersion)) {
                    // tryDecode 只会设置 versionHash 用于解密，这里的 wzDir 对象已经绑定到 JTree 里，不能在 tryDecode 重设，只能在这重解析
                    header.setFileVersion(testVersion);
                    wzDirectory.parse(reader);
                    status = WzFileStatus.PARSE_SUCCESS;
                    return true;
                }
            }

            status = WzFileStatus.ERROR_FILE_VERSION;
            log.error(status.getMessage());
            return false;
        } else {
            int versionHash = header.checkAndGetVersionHash(encVersion, header.getFileVersion());
            if (versionHash == 0) {
                status = WzFileStatus.ERROR_FILE_VERSION;
                log.error(status.getMessage());
                return false;
            }
            header.setVersionHash(versionHash);
            wzDirectory.parse(reader);
            status = WzFileStatus.PARSE_SUCCESS;
            return true;
        }
    }

    private boolean check64BitClient() {
        boolean withEncVerHeader = true;
        if (header.getFileSize() >= 2) {
            reader.setPosition(header.getDataStartPos());
            int encVersion = reader.getShort();
            if (encVersion > 0xff) {  // encVersion 永远低于 256
                withEncVerHeader = false;
            } else if (encVersion == 0x80) {
                // there's an exceptional case that the first field of data part is a compressed int which determined property count,
                // if the value greater than 127 and also to be a multiple of 256, the first 5 bytes will become to
                //   80 00 xx xx xx
                // so we additional check the int value, at most time the child node count in a wz won't greater than 65536.
                if (header.getFileSize() >= 5) {
                    reader.setPosition(header.getDataStartPos());
                    int propCount = reader.getInt();
                    if (propCount > 0 && (propCount & 0xff) == 0 && propCount <= 0xffff) {
                        withEncVerHeader = false;
                    }
                }
            }
            // old wz file with header version
        } else {
            // Obviously, if data part have only 1 byte, encVersion must be deleted.
            withEncVerHeader = false;
        }

        return withEncVerHeader;
    }

    @Override
    public boolean save() {
        return save(filePath);
    }

    private boolean save(String path) {
        if (status != WzFileStatus.PARSE_SUCCESS) return false;
        log.info("保存 {} 开始", getName());
        String saveFile = Path.of(path).toString();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(saveFile, "rw")) {
            Map<String, Integer> tempStringCache = new HashMap<>();
            BinaryWriter tempWriter = new BinaryWriter();
            log.info("保存 {} Generate Data File 1/4", getName());
            wzDirectory.generateDataFile(tempWriter, tempStringCache);
            tempStringCache.clear();
            int totalLen = wzDirectory.getImgOffsets(wzDirectory.getOffsets(header.getDataStartPos() + (is64BitWzFile() ? 0 : 2)));
            BinaryWriter writer = new BinaryWriter(true);
            writer.setWzMutableKey(getWzMutableKey());
            header.setFileSize(totalLen - header.getHeaderSize());
            for (int i = 0; i < 4; i++) {
                writer.putByte((byte) header.getSignature().charAt(i));
            }
            writer.putLong(header.getFileSize());
            writer.putInt(header.getHeaderSize());
            writer.putAsciiString(header.getCopyright());
            if (!is64BitWzFile()) {
                writer.putShort(header.getEncVersion());
            }
            log.info("保存 {} Wz Dirs 2/4", getName());
            wzDirectory.saveDirectory(writer);
            writer.getStringCache().clear();
            log.info("保存 {} Wz Images 3/4", getName());
            wzDirectory.saveImages(writer, tempWriter);
            writer.getStringCache().clear();
            log.info("保存 {} Wz 写入文件 4/4", getName());
            randomAccessFile.write(writer.output());
            log.info("保存 {} Wz 完成", getName());
            return true;
        } catch (IOException e) {
            log.error("无法保存文件 {}", e.getMessage());
            return false;
        }
    }

    /**
     * 导出Img
     *
     * @param basePath  上级路径
     * @param collector 只收集需要导出的WzImage 存入 collector
     */
    public void exportFileToImg(Path basePath, List<Pair<WzImage, Path>> collector) {
        wzDirectory.exportDirectory(basePath, collector);
    }

    /**
     * 导出XML
     *
     * @param basePath  上级路径
     * @param collector 只收集需要导出的WzImage 存入 collector
     */
    public void exportFileToXml(Path basePath, List<Pair<WzImage, Path>> collector) {
        wzDirectory.exportToXml(basePath, collector);
    }

    public void changeKey(short gameVersion, String keyBoxName, byte[] iv, byte[] key) {
        // 先解析把原有内容解码出来缓存在内存里
        if (!parse()) return;

        wzDirectory.parseAllImages();
        this.keyBoxName = keyBoxName;
        this.iv = Arrays.copyOf(iv, iv.length);
        this.key = Arrays.copyOf(key, key.length);
        reader.setWzMutableKey(new WzMutableKey(this.iv, this.key));
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

        if (testDirectory.getImages().isEmpty()) {
            if (is64BitWzFile() && fileVersion == 113) {
                header.setVersionHash(originalHash);
                reader.setPosition(originalPosition);
                return false;
            } else {
                reader.setPosition(originalPosition);
                return true;
            }
        }
        WzImage testImage = testDirectory.getImages().getFirst();
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
