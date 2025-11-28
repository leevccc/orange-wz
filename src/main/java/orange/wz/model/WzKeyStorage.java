package orange.wz.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.util.*;

import static orange.wz.lib.WzAESConstant.*;

public class WzKeyStorage {
    private static final String STORAGE_FILE = "wz_keys.json";
    private final ObjectMapper objectMapper;

    public WzKeyStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    // 保存所有数据
    public void saveAll(List<WzKey> keys) {
        try {
            objectMapper.writeValue(new File(STORAGE_FILE), keys);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 读取所有数据
    public List<WzKey> loadAll() {
        try {
            File file = new File(STORAGE_FILE);
            if (!file.exists()) {
                initialFile();
                return loadAll();
            }
            return objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WzKey.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initialFile() {
        List<WzKey> keys = new ArrayList<>();
        keys.add(new WzKey(1, "GMS", WZ_GMS_IV, DEFAULT_KEY));
        keys.add(new WzKey(2, "CMS", WZ_CMS_IV, DEFAULT_KEY));
        keys.add(new WzKey(2, "LATEST", WZ_LATEST_IV, DEFAULT_KEY));
        saveAll(keys);
    }

    // 添加新记录
    public WzKey addWzKey(WzKey key) {
        try {
            List<WzKey> keys = loadAll();

            // 生成ID（简单的自增）
            int maxId = keys.stream().mapToInt(WzKey::getId).max().orElse(0);
            key.setId(maxId + 1);

            keys.add(key);
            saveAll(keys);
            return key;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    // 删除记录
    public boolean deleteById(Integer id) {
        try {
            List<WzKey> keys = loadAll();
            boolean removed = keys.removeIf(key -> key.getId().equals(id));
            if (removed) {
                saveAll(keys);
            }
            return removed;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
