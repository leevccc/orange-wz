package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public class SoundFormData extends NodeFormData {
    private final byte[] soundBytes;

    public SoundFormData(String name, String type, byte[] soundBytes) {
        super(name, type);
        this.soundBytes = soundBytes;
    }
}
