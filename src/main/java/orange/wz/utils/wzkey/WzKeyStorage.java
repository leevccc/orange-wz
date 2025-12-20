package orange.wz.utils.wzkey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static orange.wz.provider.WzAESConstant.*;

public class WzKeyStorage {
    private static final Path STORAGE_FILE = Path.of("keys.dat");
    private final ObjectMapper objectMapper;

    public WzKeyStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void saveAll(List<WzKey> keys) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(keys);
            EncryptedFileIO.write(STORAGE_FILE, json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<WzKey> loadAll() {
        try {
            if (!Files.exists(STORAGE_FILE)) {
                initialFile();
            }

            byte[] json = EncryptedFileIO.read(STORAGE_FILE);
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WzKey.class)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initialFile() {
        List<WzKey> keys = new ArrayList<>();
        keys.add(new WzKey(1, "国际服务器(低版本)", WZ_GMS_IV, DEFAULT_KEY));
        keys.add(new WzKey(2, "亚洲服务器(低版本)", WZ_CMS_IV, DEFAULT_KEY));
        keys.add(new WzKey(3, "新版本客户端", WZ_LATEST_IV, DEFAULT_KEY));
        saveAll(keys);
    }

    // 添加新记录
    public WzKey addWzKey(String name) {
        WzKey wzKey = new WzKey();
        wzKey.setName(name);
        wzKey.setIv(WZ_LATEST_IV);
        wzKey.setUserKey(DEFAULT_KEY);
        return addWzKey(wzKey);
    }

    public WzKey addWzKey(WzKey key) {
        List<WzKey> keys = loadAll();

        for (WzKey wzKey : keys) {
            if (wzKey.getName().equalsIgnoreCase(key.getName())) return null;
        }

        // 生成ID（简单的自增）
        int maxId = keys.stream().mapToInt(WzKey::getId).max().orElse(0);
        key.setId(maxId + 1);

        keys.add(key);
        saveAll(keys);
        return key;
    }

    // 根据ID查找
    public WzKey findById(Integer id) {
        try {
            return loadAll().stream()
                    .filter(key -> key.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 根据名称查找
    public WzKey findByName(String name) {
        return loadAll().stream()
                .filter(key -> key.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public boolean renameById(int id, String name) {
        boolean result = false;
        boolean exists = false;
        List<WzKey> keys = loadAll();
        for (WzKey key : keys) {
            if (key.getId() != id && key.getName().equalsIgnoreCase(name)) {
                exists = true;
                break;
            }
        }

        if (exists) return false;

        for (WzKey key : keys) {
            if (key.getId() == id) {
                key.setName(name);
                result = true;
                break;
            }
        }

        if (result) {
            saveAll(keys);
            return true;
        }
        return false;
    }

    // 删除记录
    public boolean deleteById(Integer id) {
        List<WzKey> keys = loadAll();
        boolean removed = keys.removeIf(key -> key.getId().equals(id));
        if (removed) {
            saveAll(keys);
        }
        return removed;
    }
}
