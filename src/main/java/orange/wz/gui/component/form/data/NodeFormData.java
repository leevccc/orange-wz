package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public class NodeFormData {
    protected final String name;
    protected final String type;

    public NodeFormData(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
