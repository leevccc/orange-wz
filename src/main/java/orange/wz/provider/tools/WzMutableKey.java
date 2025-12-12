package orange.wz.provider.tools;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

public final class WzMutableKey {
    private final byte[] iv;
    private final byte[] userKey;
    private final byte[] aesUserKey;
    private byte[] keys;

    private static final int batchSize = 4096;

    public WzMutableKey(byte[] iv, byte[] userKey) {
        this.iv = iv;
        this.userKey = userKey;
        this.aesUserKey = getTrimmedUserKey();
    }

    public byte get(int index) {
        try {
            ensureKeySize(index + 1);
            return keys[index];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void ensureKeySize(int size) throws Exception {
        if (keys != null && keys.length >= size) {
            return;
        }

        size = (int) Math.ceil(1.0 * size / batchSize) * batchSize;
        byte[] newKeys = new byte[size];

        if (ByteBuffer.wrap(iv).getInt() == 0) {
            keys = newKeys;
            return;
        }

        int startIndex = 0;
        if (keys != null) {
            System.arraycopy(keys, 0, newKeys, 0, keys.length);
            startIndex = keys.length;
        }

        keys = newKeys;

        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aesUserKey, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        byte[] block = new byte[16];

        for (int i = startIndex; i < size; i += 16) {
            if (i == 0) {
                for (int j = 0; j < block.length; j++) {
                    block[j] = iv[j % 4];
                }
            } else {
                System.arraycopy(newKeys, i - 16, block, 0, 16);
            }

            byte[] enc = cipher.update(block);
            System.arraycopy(enc, 0, newKeys, i, 16);
        }

        keys = newKeys;
    }

    private byte[] getTrimmedUserKey() {
        byte[] key = new byte[32];
        for (int i = 0; i < 128; i += 16) {
            key[i / 4] = userKey[i];
        }
        return key;
    }
}
