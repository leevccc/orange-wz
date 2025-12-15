package orange.wz.provider.tools;

import orange.wz.provider.WzImageProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WzChildrenProperty {
    private final List<WzImageProperty> properties = Collections.synchronizedList(new ArrayList<>());

    public WzImageProperty get(String name) {
        synchronized (properties) {
            for (WzImageProperty property : properties) {
                if (property.getName().equalsIgnoreCase(name)) {
                    return property;
                }
            }
        }
        return null;
    }

    public List<WzImageProperty> get() {
        synchronized (properties) {
            return new ArrayList<>(properties);
        }
    }

    public void add(WzImageProperty child) {
        properties.add(child);
    }

    public void add(List<WzImageProperty> children) {
        properties.addAll(children);
    }

    public boolean remove(String name) {
        synchronized (properties) {
            return properties.removeIf(property -> property.getName().equalsIgnoreCase(name));
        }
    }

    public boolean remove(Set<String> names) {
        synchronized (properties) {
            return properties.removeIf(property -> names.contains(property.getName()));
        }
    }

    public boolean existChild(String name) {
        return get(name) != null;
    }

    public void clear() {
        properties.clear();
    }
}
