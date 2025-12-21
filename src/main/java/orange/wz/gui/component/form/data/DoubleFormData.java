package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public class DoubleFormData extends NodeFormData {
    private final double value;

    public DoubleFormData(String name, String type, double value) {
        super(name, type);
        this.value = value;
    }
}
