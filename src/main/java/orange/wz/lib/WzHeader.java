package orange.wz.lib;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class WzHeader {
    private String ident;
    private String copyright;
    private long size; // uLong
    private int start; // uInt

    public void recalculateFileStart() {
        start = 17 + copyright.length(); // ident(4 byte) + fSize(8) + fStart(4) + copyright(size) + 1
    }

    public static WzHeader getDefault() {
        WzHeader header = new WzHeader();
        header.ident = "PKG1";
        header.copyright = "Package file v1.0 Copyright 2002 Wizet, ZMS";
        header.start = 60;
        header.size = 0;
        return header;
    }
}
