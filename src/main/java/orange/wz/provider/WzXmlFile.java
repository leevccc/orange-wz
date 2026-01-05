package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.MediaExportType;
import orange.wz.provider.tools.WzFileStatus;
import orange.wz.provider.tools.XmlImport;

import java.nio.file.Path;
import java.util.Arrays;

@Getter
@Setter
@Slf4j
public class WzXmlFile extends WzImage implements WzSavableFile {
    private String filePath;
    private String keyBoxName;
    private byte[] iv;
    private byte[] key;
    private int indent = 2;
    private MediaExportType meType = MediaExportType.BASE64;

    public WzXmlFile(String name, String filePath, String keyBoxName, byte[] iv, byte[] key) {
        super(name, null);
        this.filePath = filePath;
        this.keyBoxName = keyBoxName;
        this.iv = Arrays.copyOf(iv, iv.length);
        this.key = Arrays.copyOf(key, key.length);
        super.setReader(new BinaryReader(this.iv, this.key));
        super.setChanged(true);
        super.setStatus(WzFileStatus.UNPARSE);
    }

    public String getImgName() {
        String name = super.getName();
        return name.endsWith(".xml")
                ? name.substring(0, name.length() - 4)
                : name;
    }

    public boolean parse() {
        if (XmlImport.importXml(this, Path.of(filePath))) {
            super.setStatus(WzFileStatus.PARSE_SUCCESS);
            return true;
        }
        return false;
    }

    @Override
    public boolean save() {
        return exportToXml(Path.of(filePath), indent, meType);
    }
}
