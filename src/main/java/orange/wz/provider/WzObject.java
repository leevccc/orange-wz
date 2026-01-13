package orange.wz.provider;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import orange.wz.provider.tools.WzType;

@NoArgsConstructor
public abstract class WzObject {
    @Getter
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

    public void setNameAnyway(String name) {
        this.name = name;
    }

    public boolean setName(String newName) {
        if (parent != null) {
            switch (parent) {
                case WzDirectory pDir when this instanceof WzDirectory -> {
                    if (pDir.existDirectory(newName)) return false;
                }
                case WzDirectory pDir when this instanceof WzImage -> {
                    if (pDir.existImage(newName)) return false;
                }
                case WzImage pImg -> {
                    if (pImg.existChild(newName)) return false;
                }
                case WzImageProperty pProp when pProp.isListProperty() -> {
                    if (pProp.existChild(newName)) return false;
                }
                default -> {
                }
            }
        }

        name = newName;
        return true;
    }
}
