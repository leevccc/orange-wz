package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.tools.WzMutableKey;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public final class BinaryReader {
    private ByteBuffer buffer;
    private WzHeader header;
    private int hash;
    private byte[] iv;
    private byte[] userKey;
    private WzMutableKey wzMutableKey;

    public BinaryReader(String wzPath, byte[] iv, byte[] userKey) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(wzPath, "r")) {
            final FileChannel channel = randomAccessFile.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length());
            buffer.position(0);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            this.iv = iv;
            this.userKey = userKey;
            wzMutableKey = new WzMutableKey(iv, userKey);
        } catch (IOException e) {
            buffer = null;
            throw new RuntimeException(e);
        }
    }

    /**
     * 只做临时使用，不做解析
     *
     */
    public BinaryReader(byte[] data) {
        buffer = ByteBuffer.allocate(16);
        putBytes(data);
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * 只做临时使用，不做解析
     *
     */
    public BinaryReader(byte[] iv, byte[] userKey) {
        this.iv = iv;
        this.userKey = userKey;
        wzMutableKey = new WzMutableKey(iv, userKey);
    }

    /* Put -----------------------------------------------------------------------------------------------------------*/
    private void expandBuffer(int needSize) {
        int remaining = buffer.capacity() - buffer.position();

        if (remaining >= needSize) { // 当前缓冲区剩余空间足够，不需要扩容
            return;
        }

        int additionalSize = needSize - remaining;
        int requiredCapacity = buffer.capacity() + additionalSize;

        ByteBuffer newBuffer = ByteBuffer.allocate(requiredCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
    }

    public void putBytes(byte[] data) {
        if (buffer.remaining() < data.length) {
            expandBuffer(data.length);
        }
        buffer.put(data);
    }

    /* Position ------------------------------------------------------------------------------------------------------*/
    public int getPosition() {
        return buffer.position();
    }

    public void setPosition(int position) {
        buffer.position(position);
    }

    public void skip(int bytes) {
        buffer.position(buffer.position() + bytes);
    }

    /* Get -----------------------------------------------------------------------------------------------------------*/
    public byte getByte() {
        return buffer.get();
    }

    public byte[] getBytes(int length) {
        byte[] data = new byte[length];
        buffer.get(data);
        return data;
    }

    public byte[] getBytes(int offset, int length) {
        int currentOffset = buffer.position();
        buffer.position(offset);
        byte[] data = new byte[length];
        buffer.get(data);
        buffer.position(currentOffset);
        return data;
    }

    public short getShort() {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    public int getInt() {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt();
    }

    public long getLong() {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getLong();
    }

    public float getFloat() {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getFloat();
    }

    public double getDouble() {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getDouble();
    }

    /* Read ----------------------------------------------------------------------------------------------------------*/
    public String readStringAtOffset(int offset) {
        int currentOffset = getPosition();
        setPosition(offset);
        String returnString = readString();
        setPosition(currentOffset);
        return returnString;
    }

    public String readString() {
        int length = buffer.get();
        if (length < 0) {
            if (length == Byte.MIN_VALUE) {
                length = getInt();
            } else {
                length = -length;
            }

            if (length > 0) {
                final byte[] data = getBytes(length);
                byte mask = (byte) 0xAA;
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) (data[i] ^ wzMutableKey.get(i) ^ mask);
                    mask++;
                }
                return new String(data, StandardCharsets.US_ASCII);
            }
        } else if (length > 0) {
            if (length == Byte.MAX_VALUE) {
                length = getInt();
            }
            if (length > 0) {
                length *= 2; // UTF16
                final byte[] data = getBytes(length);
                short mask = (short) 0xAAAA;
                for (int i = 0; i < data.length; i += 2) {
                    data[i] = (byte) (data[i] ^ wzMutableKey.get(i) ^ (mask & 0xFF));
                    data[i + 1] = (byte) (data[i + 1] ^ wzMutableKey.get(i + 1) ^ (mask >> 8));
                    mask++;
                }
                return new String(data, StandardCharsets.UTF_16LE);
            }
        }

        return "";
    }

    public String readString(int length) {
        final byte[] data = new byte[length];
        buffer.get(data);
        return new String(data, StandardCharsets.US_ASCII);
    }

    public String readNullTerminatedString() {
        StringBuilder retString = new StringBuilder();
        byte b = buffer.get();
        while (b != 0) {
            retString.append((char) b);
            b = buffer.get();
        }
        return retString.toString();
    }

    public int readCompressedInt() {
        byte b = buffer.get();
        if (b == Byte.MIN_VALUE) {
            return getInt();
        }
        return b;
    }

    public long readCompressedLong() {
        byte b = buffer.get();
        if (b == Byte.MIN_VALUE) {
            return getLong();
        }
        return b;
    }

    public float readCompressedFloat() {
        final byte floatType = buffer.get();
        switch (floatType) {
            case 0x00 -> {
                return 0f;
            }
            case (byte) 0x80 -> {
                return getFloat();
            }
            default -> throw new WzReaderError("Unknown float type : %d", floatType);
        }
    }

    public int readOffset() {
        int offset = getPosition();
        offset = ~(offset - header.getStart());
        offset = offset * hash;
        offset = offset - WzAESConstant.WZ_OFFSET_CONSTANT;
        offset = Integer.rotateLeft(offset, offset & 0x1F);
        int encryptedOffset = getInt();
        offset = offset ^ encryptedOffset;
        offset = offset + header.getStart() * 2;
        return offset;
    }

    public String readStringBlock(int offset) {
        return switch (buffer.get()) {
            case 0, 0x73 -> readString();
            case 1, 0x1B -> readStringAtOffset(offset + getInt());
            default -> "";
        };
    }

    /* Output --------------------------------------------------------------------------------------------------------*/
    public byte[] output() {
        ByteBuffer result = buffer.duplicate();
        result.flip();
        byte[] bytes = new byte[result.remaining()];
        result.get(bytes);
        return bytes;
    }
}
