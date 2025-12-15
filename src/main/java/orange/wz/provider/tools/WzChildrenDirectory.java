package orange.wz.provider.tools;

import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WzChildrenDirectory {
    private final List<WzDirectory> directories = Collections.synchronizedList(new ArrayList<>());
    private final List<WzImage> images = Collections.synchronizedList(new ArrayList<>());

    public WzDirectory getDirectory(String name) {
        synchronized (directories) {
            for (WzDirectory directory : directories) {
                if (directory.getName().equalsIgnoreCase(name))
                    return directory;
            }
        }
        return null;
    }

    public WzImage getImage(String name) {
        synchronized (images) {
            for (WzImage image : images) {
                if (image.getName().equalsIgnoreCase(name))
                    return image;
            }
        }
        return null;
    }

    public List<WzDirectory> getDirectories() {
        synchronized (directories) {
            return new ArrayList<>(directories);
        }
    }

    public List<WzImage> getImages() {
        synchronized (images) {
            return new ArrayList<>(images);
        }
    }

    public List<WzObject> getAllChildren() {
        List<WzObject> allChildren = new ArrayList<>();
        allChildren.addAll(getDirectories());
        allChildren.addAll(getImages());

        return allChildren;
    }

    public int getEntryCount() {
        return directories.size() + images.size();
    }

    public void add(WzDirectory directory) {
        directories.add(directory);
    }

    public void add(WzImage image) {
        images.add(image);
    }

    public boolean removeDirectory(String name) {
        synchronized (directories) {
            return directories.removeIf(directory -> directory.getName().equalsIgnoreCase(name));
        }
    }

    public boolean removeImage(String name) {
        synchronized (images) {
            return images.removeIf(image -> image.getName().equalsIgnoreCase(name));
        }
    }

    public boolean existDirectory(String name) {
        return getDirectory(name) != null;
    }

    public boolean existImage(String name) {
        return getImage(name) != null;
    }
}
