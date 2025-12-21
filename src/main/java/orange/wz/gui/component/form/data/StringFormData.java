package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public class StringFormData extends NodeFormData {
    private final String value;

    public StringFormData(String name, String type, String value) {
        super(name, type);
        this.value = value;
    }
}
