package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public class ShortFormData extends NodeFormData {
    private final short value;

    public ShortFormData(String name, String type, short value) {
        super(name, type);
        this.value = value;
    }
}
