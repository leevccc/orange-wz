package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public final class KeyData {
    private final String name;
    private final short version;
    private final byte[] iv;
    private final byte[] key;

    public KeyData(String name, short version, byte[] iv, byte[] key) {
        this.name = name;
        this.version = version;
        this.iv = iv;
        this.key = key;
    }
}
