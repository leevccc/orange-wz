package orange.wz.service;

import orange.wz.exception.BizException;
import orange.wz.exception.ExceptionEnum;
import orange.wz.model.WzKey;
import orange.wz.model.WzKeyStorage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WzKeyService {
    private final WzKeyStorage wzKeyRepository = new WzKeyStorage();

    public List<WzKey> findAll() {
        return wzKeyRepository.loadAll();
    }

    public void update(int id, WzKey data) {
        WzKey wzKey = wzKeyRepository.findById(id);
        if (wzKey == null) {
            throw new BizException(ExceptionEnum.NOT_FOUND_WZ_KEY);
        }

        String name = wzKey.getName();
        if (name.equalsIgnoreCase("GMS") || name.equalsIgnoreCase("CMS") || name.equalsIgnoreCase("LATEST")) {
            throw new BizException(ExceptionEnum.DISABLE_TO_UPDATE_SYSTEM_CONFIG);
        }

        WzKey testKey = wzKeyRepository.findByName(wzKey.getName());
        if (testKey != null && testKey.getId() != id) {
            throw new BizException(ExceptionEnum.REPEAT_NAME);
        }

        wzKey.setName(data.getName());
        wzKey.setIv(data.getIv());
        wzKey.setUserKey(data.getUserKey());
        wzKeyRepository.addWzKey(wzKey);
    }

    public WzKey save(WzKey wzKey) {
        String name = wzKey.getName();
        if (name.equalsIgnoreCase("GMS") || name.equalsIgnoreCase("CMS") || name.equalsIgnoreCase("LATEST")) {
            throw new BizException(ExceptionEnum.DISABLE_TO_UPDATE_SYSTEM_CONFIG);
        }
        if (wzKeyRepository.findByName(name) != null) {
            throw new BizException(ExceptionEnum.REPEAT_NAME);
        }
        WzKey newData = new WzKey();
        newData.setName(wzKey.getName());
        newData.setIv(wzKey.getIv());
        newData.setUserKey(wzKey.getUserKey());

        return wzKeyRepository.addWzKey(newData);
    }

    public void delete(int id) {
        WzKey wzKey = wzKeyRepository.findById(id);
        if (wzKey == null) throw new BizException(ExceptionEnum.NOT_FOUND_WZ_KEY);
        String name = wzKey.getName();
        if (name.equalsIgnoreCase("GMS") || name.equalsIgnoreCase("CMS") || name.equalsIgnoreCase("LATEST")) {
            throw new BizException(ExceptionEnum.DISABLE_TO_DELETE_SYSTEM_CONFIG);
        }
        wzKeyRepository.deleteById(wzKey.getId());
    }
}
