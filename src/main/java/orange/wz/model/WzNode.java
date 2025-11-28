package orange.wz.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import orange.wz.lib.WzObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.*;

@Getter
public class WzNode {
    private final Integer id;
    @Setter
    private String name;
    private final WzNodeType type;
    private final boolean leaf;
    private final boolean file;
    @JsonIgnore
    private final Path path;
    @JsonIgnore
    private final boolean view;
    @JsonIgnore
    @Setter
    private WzNode parent;
    @Getter(AccessLevel.NONE)
    private final List<WzNode> children = new ArrayList<>();
    @JsonIgnore
    private WzObject wzObject;

    public WzNode(WzNode parent, Integer id, String name, WzNodeType type, Path path) {
        this(parent, id, name, type, path, null);
    }

    public WzNode(WzNode parent, Integer id, String name, WzNodeType type, Path path, WzObject wzObject) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.view = false;
        this.parent = parent;
        this.leaf = getLeafByType(type);
        this.file = path != null;
        this.wzObject = wzObject;
    }

    public WzNode(Integer id, String name, WzNodeType type, Path path, boolean view) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.view = view;
        this.parent = null;
        this.leaf = getLeafByType(type);
        this.file = path != null;
        this.wzObject = null;
    }

    public List<WzNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    // 添加子节点管理方法
    public void addChild(WzNode child) {
        children.add(child);
        sortChildren();
    }

    public void addChildren(List<WzNode> children) {
        this.children.addAll(children);
        sortChildren();
    }

    public void removeChild(Set<Integer> ids, int childId) {
        Iterator<WzNode> it = children.iterator();

        while (it.hasNext()) {
            WzNode child = it.next();
            if (child.getId() == childId) {
                child.removeAllChildren(ids);
                ids.add(child.getId());

                // 断开引用
                child.parent = null;
                child.wzObject = null;

                // 移除自身
                it.remove();

                break;
            }
        }
    }

    private void removeAllChildren(Set<Integer> ids) {
        for (WzNode sub : children) {
            sub.removeAllChildren(ids);
            ids.add(sub.getId());
            sub.parent = null;
            sub.wzObject = null;
        }
        children.clear();
    }

    public boolean unlinkChild(int id) {
        for (WzNode child : children) {
            if (child.getId() == id) {
                children.remove(child);
                return true;
            }
        }
        return false;
    }

    private static final List<WzNodeType> typePriority = List.of(
            WzNodeType.FOLDER,
            WzNodeType.WZ,
            WzNodeType.WZ_DIRECTORY
    );

    private void sortChildren() {
        children.sort(Comparator
                .comparing((WzNode node) -> {
                    int index = typePriority.indexOf(node.getType());
                    return index == -1 ? Integer.MAX_VALUE : index; // 未定义的type排最后
                })
                .thenComparing(WzNode::getName, (a, b) -> {
                    // 内联的自然排序比较器
                    if (a == null && b == null) return 0;
                    if (a == null) return -1;
                    if (b == null) return 1;

                    int aIndex = 0, bIndex = 0;
                    int aLength = a.length();
                    int bLength = b.length();

                    while (aIndex < aLength && bIndex < bLength) {
                        char aChar = a.charAt(aIndex);
                        char bChar = b.charAt(bIndex);

                        if (Character.isDigit(aChar) && Character.isDigit(bChar)) {
                            int aNumber = 0;
                            while (aIndex < aLength && Character.isDigit(a.charAt(aIndex))) {
                                aNumber = aNumber * 10 + (a.charAt(aIndex) - '0');
                                aIndex++;
                            }

                            int bNumber = 0;
                            while (bIndex < bLength && Character.isDigit(b.charAt(bIndex))) {
                                bNumber = bNumber * 10 + (b.charAt(bIndex) - '0');
                                bIndex++;
                            }

                            if (aNumber != bNumber) {
                                return Integer.compare(aNumber, bNumber);
                            }
                        } else {
                            int compare = Character.compare(Character.toLowerCase(aChar), Character.toLowerCase(bChar));
                            if (compare != 0) {
                                return compare;
                            }
                            aIndex++;
                            bIndex++;
                        }
                    }

                    return aLength - bLength;
                }));
    }

    private boolean getLeafByType(WzNodeType type) {
        return !(type == WzNodeType.FOLDER || type == WzNodeType.WZ || type == WzNodeType.WZ_DIRECTORY || type == WzNodeType.IMAGE || type == WzNodeType.IMAGE_LIST || type == WzNodeType.IMAGE_CANVAS || type == WzNodeType.IMAGE_CONVEX);
    }
}
