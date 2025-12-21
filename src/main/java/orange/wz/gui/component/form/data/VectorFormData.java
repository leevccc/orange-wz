package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public class VectorFormData extends NodeFormData {
    private final int x;
    private final int y;

    public VectorFormData(String name, String type, int x, int y) {
        super(name, type);
        this.x = x;
        this.y = y;
    }
}
