package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public final class CreateFileData {
    private final String name;
    private final short version;

    public CreateFileData(String name, short version) {
        this.name = name;
        this.version = version;
    }
}
