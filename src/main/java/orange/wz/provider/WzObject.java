package orange.wz.provider;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import orange.wz.provider.tools.WzType;

@NoArgsConstructor
public abstract class WzObject {
    @Getter
    @Setter
    protected String name;
    @Getter
    protected String path;
    @Getter
    protected WzType type;
    @Getter
    protected WzObject parent;
    @Getter
    @Setter
    private boolean tempChanged;

    protected WzObject(String name, WzType type, WzObject parent) {
        this.name = name;
        this.parent = parent;

        if (parent == null || name.equals(parent.getName()) && name.endsWith(".wz")) {
            this.path = name;
        } else {
            this.path = parent.getPath() + "/" + name;
        }

        this.type = type;
    }

    public void setParent(WzObject parent) {
        this.parent = parent;

        if (parent == null || name.equals(parent.getName()) && name.endsWith(".wz")) {
            this.path = name;
        } else {
            this.path = parent.getPath() + "/" + name;
        }
    }

    public abstract WzObject deepClone(WzObject parent);
}
