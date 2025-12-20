package orange.wz.utils.wzkey;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EncryptedFileIO {
    public static void write(Path path, byte[] plain) throws Exception {
        byte[] MAGIC = new byte[]{'W', 'Z', 'K', 'Y'};
        byte VERSION = 1;

        CryptoUtils.EncryptedData encrypted = CryptoUtils.encrypt(plain);

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path)))) {

            out.write(MAGIC);                 // Magic
            out.writeByte(VERSION);           // Version
            out.writeByte(encrypted.iv().length);
            out.write(encrypted.iv());
            out.write(encrypted.data());
        }
    }

    public static byte[] read(Path path) throws Exception {
        byte[] MAGIC = new byte[]{'W', 'Z', 'K', 'Y'};
        byte VERSION = 1;

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {

            byte[] magic = in.readNBytes(4);
            if (!java.util.Arrays.equals(magic, MAGIC)) {
                throw new IllegalStateException("非法文件格式");
            }

            byte version = in.readByte();
            if (version != VERSION) {
                throw new IllegalStateException("不支持的版本: " + version);
            }

            int ivLen = in.readByte();
            byte[] iv = in.readNBytes(ivLen);
            byte[] encrypted = in.readAllBytes();

            return CryptoUtils.decrypt(iv, encrypted);
        }
    }

    private EncryptedFileIO() {
    }
}
