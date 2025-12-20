package orange.wz.provider.tools;

import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzFolder;
import orange.wz.provider.WzImageFile;
import orange.wz.provider.WzObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WzChildrenFolder {
    private final List<WzFolder> folders = Collections.synchronizedList(new ArrayList<>());
    private final List<WzDirectory> wzFiles = Collections.synchronizedList(new ArrayList<>());
    private final List<WzImageFile> wzImages = Collections.synchronizedList(new ArrayList<>());

    public List<WzFolder> getFolders() {
        synchronized (folders) {
            return new ArrayList<>(folders);
        }
    }

    public List<WzDirectory> getWzFiles() {
        synchronized (wzFiles) {
            return new ArrayList<>(wzFiles);
        }
    }

    public List<WzImageFile> getWzImages() {
        synchronized (wzImages) {
            return new ArrayList<>(wzImages);
        }
    }

    public List<WzObject> getAllChildren() {
        List<WzObject> allChildren = new ArrayList<>();
        allChildren.addAll(getFolders());
        allChildren.addAll(getWzFiles());
        allChildren.addAll(getWzImages());

        return allChildren;
    }

    public void add(WzFolder folder) {
        folders.add(folder);
    }

    public void add(WzDirectory wzFile) {
        wzFiles.add(wzFile);
    }

    public void add(WzImageFile wzImage) {
        wzImages.add(wzImage);
    }
}
