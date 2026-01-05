package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.WzFileStatus;

import java.nio.file.Path;
import java.util.Arrays;

@Getter
@Setter
@Slf4j
public class WzImageFile extends WzImage implements WzSavableFile {
    private String filePath;
    private String keyBoxName;
    private byte[] iv;
    private byte[] key;

    public WzImageFile(String name, String filePath, byte[] iv, byte[] key) {
        super(name, null);
        this.filePath = filePath;
        this.iv = Arrays.copyOf(iv, iv.length);
        this.key = Arrays.copyOf(key, key.length);
    }

    public WzImageFile(String name, String filePath, String keyBoxName, byte[] iv, byte[] key) {
        this(name, filePath, iv, key);
        this.keyBoxName = keyBoxName;
    }

    public boolean parse() {
        return parse(true);
    }

    public synchronized boolean parse(boolean realParse) {
        if (status == WzFileStatus.PARSE_SUCCESS) return true;

        BinaryReader reader = new BinaryReader(filePath, iv, key);
        super.setReader(reader);
        super.setDataSize(reader.getDataSize());
        super.setChecksum(0);
        byte[] bytes = reader.output();
        for (byte b : bytes) {
            super.addChecksum(b);
        }
        super.setOffset(0);
        return super.parse(realParse);
    }

    @Override
    public boolean save() {
        return save(Path.of(filePath));
    }
}
