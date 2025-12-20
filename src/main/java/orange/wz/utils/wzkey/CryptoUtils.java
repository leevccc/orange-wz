package orange.wz.utils.wzkey;

import orange.wz.manager.ServerManager;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public final class CryptoUtils {
    public static EncryptedData encrypt(byte[] plain) throws Exception {
        String ALGO = "AES/GCM/NoPadding";
        int GCM_TAG_LENGTH = 128;
        byte[] KEY = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(ServerManager.getSlog().getBytes(StandardCharsets.UTF_8)), 16);

        byte[] iv = new byte[12]; // GCM 推荐 12 字节
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(KEY, "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv)
        );

        byte[] encrypted = cipher.doFinal(plain);
        return new EncryptedData(iv, encrypted);
    }

    public static byte[] decrypt(byte[] iv, byte[] encrypted) throws Exception {
        String ALGO = "AES/GCM/NoPadding";
        int GCM_TAG_LENGTH = 128;
        byte[] KEY = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(ServerManager.getSlog().getBytes(StandardCharsets.UTF_8)), 16);

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(KEY, "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv)
        );
        return cipher.doFinal(encrypted);
    }

    public record EncryptedData(byte[] iv, byte[] data) {
    }
}
