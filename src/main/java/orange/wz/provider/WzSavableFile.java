package orange.wz.provider;

import orange.wz.provider.tools.WzFileStatus;

public interface WzSavableFile {
    String getFilePath();

    void setFilePath(String filePath);

    String getName();

    String getKeyBoxName();

    byte[] getIv();

    byte[] getKey();

    WzFileStatus getStatus();

    boolean save();
}
