package orange.wz.gui;

import lombok.Getter;
import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class Clipboard {
    @Getter
    private final List<WzObject> items = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void clear() {
        items.clear();
    }

    public void add(WzObject item) {
        items.add(item);
    }

    public boolean canPaste(WzObject target) {
        WzObject child = items.getFirst();
        return switch (child) {
            case null -> false;
            case WzDirectory ignored when target instanceof WzDirectory -> true;
            case WzImage ignored when target instanceof WzDirectory -> true;
            case WzImageProperty ignored when target instanceof WzImage -> true;
            default ->
                    child instanceof WzImageProperty && target instanceof WzImageProperty prop && prop.isListProperty();
        };
    }
}
