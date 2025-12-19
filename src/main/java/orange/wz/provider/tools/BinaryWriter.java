package orange.wz.provider.tools;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzAESConstant;
import orange.wz.provider.WzDirectoryType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class BinaryWriter {
    private ByteBuffer buffer;
    @Setter
    private WzMutableKey wzMutableKey;
    @Getter
    private final Map<String, Integer> stringCache = new HashMap<>();
    private long nextAllocateSize = 4 * 1024 * 1024; // 4MB

    public BinaryWriter() {
        buffer = ByteBuffer.allocate((int) nextAllocateSize);
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        nextAllocateSize *= 4; // 16MB
    }

    public BinaryWriter(boolean bigFile) {
        if (bigFile) {
            nextAllocateSize = 64 * 1024 * 1024; // 64 MB
        }
        buffer = ByteBuffer.allocate((int) nextAllocateSize);
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        nextAllocateSize *= 2; // 128 MB
    }

    public BinaryWriter(byte[] data) {
        buffer = ByteBuffer.allocate(16);
        putBytes(data);
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /* Position ------------------------------------------------------------------------------------------------------*/
    public int getPosition() {
        return buffer.position();
    }

    public void setPosition(int position) {
        buffer.position(position);
    }

    public void skip(int position) {
        buffer.position(buffer.position() + position);
    }

    /* Get -----------------------------------------------------------------------------------------------------------*/
    public byte[] getBytes(int length) {
        byte[] data = new byte[length];
        buffer.get(data);
        return data;
    }

    /* Put -----------------------------------------------------------------------------------------------------------*/
    private void expandBuffer(int needSize) {
        int remaining = buffer.capacity() - buffer.position();
        if (remaining >= needSize) { // 当前缓冲区剩余空间足够，不需要扩容
            return;
        }

        remaining = (int) (nextAllocateSize - buffer.position());
        while (remaining < needSize) {
            nextAllocateSize *= 2;
            remaining = (int) (nextAllocateSize - buffer.position());
        }

        int newSize = Math.toIntExact(nextAllocateSize);

        ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void putByte(byte value) {
        if (buffer.remaining() < 1) {
            expandBuffer(1);
        }
        buffer.put(value);
    }

    public void putBytes(byte[] data) {
        if (buffer.remaining() < data.length) {
            expandBuffer(data.length);
        }
        buffer.put(data);
    }

    public void putShort(short value) {
        if (buffer.remaining() < 2) {
            expandBuffer(2);
        }
        buffer.putShort(value);
    }

    public void putInt(int value) {
        if (buffer.remaining() < 4) {
            expandBuffer(4);
        }
        buffer.putInt(value);
    }

    public void putLong(long value) {
        if (buffer.remaining() < 8) {
            expandBuffer(8);
        }
        buffer.putLong(value);
    }

    public void putFloat(float value) {
        if (buffer.remaining() < 4) {
            expandBuffer(4);
        }
        buffer.putFloat(value);
    }

    public void putDouble(double value) {
        if (buffer.remaining() < 8) {
            expandBuffer(8);
        }
        buffer.putDouble(value);
    }

    public void putAsciiString(String string) {
        putBytes(string.getBytes(StandardCharsets.US_ASCII));
    }

    public void putString(String string) {
        putBytes(string.getBytes(StandardCharsets.UTF_16LE));
    }

    /* Write ---------------------------------------------------------------------------------------------------------*/
    private void writeString(String value) {
        if (value.isEmpty()) {
            putByte((byte) 0);
            return;
        }

        if (isUnicode(value)) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_16LE);
            short mask = (short) 0xAAAA;
            int length = bytes.length / 2;
            if (length >= Byte.MAX_VALUE) {
                putByte(Byte.MAX_VALUE);
                putInt(length);
            } else {
                putByte((byte) length);
            }

            for (int i = 0; i < bytes.length; i += 2) {
                bytes[i] = (byte) (bytes[i] ^ wzMutableKey.get(i) ^ (mask & 0xFF));
                bytes[i + 1] = (byte) (bytes[i + 1] ^ wzMutableKey.get(i + 1) ^ (mask >> 8));
                mask++;
            }
            putBytes(bytes);
        } else {
            byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
            byte mask = (byte) 0xAA;
            int length = bytes.length;
            if (length > Byte.MAX_VALUE) {
                putByte(Byte.MIN_VALUE);
                putInt(length);
            } else {
                putByte((byte) -length);
            }

            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ wzMutableKey.get(i) ^ mask);
                mask++;
            }
            putBytes(bytes);
        }
    }

    public void writeStringBlock(String s, int withoutOffset, int withOffset) {
        if (s.length() > 4 && stringCache.containsKey(s)) {
            putByte((byte) withOffset);
            putInt(stringCache.get(s));
        } else {
            putByte((byte) withoutOffset);
            int sOffset = getPosition();
            writeString(s);
            if (!stringCache.containsKey(s)) stringCache.put(s, sOffset);
        }
    }

    public static boolean isUnicode(String value) {
        return value.chars().anyMatch(c -> c > Byte.MAX_VALUE);
    }

    public void writeCompressedInt(int value) {
        if (value > Byte.MAX_VALUE || value <= Byte.MIN_VALUE) {
            putByte(Byte.MIN_VALUE);
            putInt(value);
        } else {
            putByte((byte) value);
        }
    }

    public void writeCompressedLong(long value) {
        if (value > Byte.MAX_VALUE || value <= Byte.MIN_VALUE) {
            putByte(Byte.MIN_VALUE);
            putLong(value);
        } else {
            putByte((byte) value);
        }
    }

    public void writeWzObjectValue(String name, WzDirectoryType type, int dataStartPos) {
        String storeName = type.name() + "_" + name;
        if (name.length() > 4 && stringCache.containsKey(storeName)) {
            putByte(WzDirectoryType.RetrieveStringFromOffset.getValue()); // 2
            putInt(stringCache.get(storeName));
        } else {
            int sOffset = buffer.position() - dataStartPos;
            putByte(type.getValue());
            writeString(name);
            if (!stringCache.containsKey(storeName)) {
                stringCache.put(storeName, sOffset);
            }
        }
    }

    public void writeOffset(long value, int dataStartPos, int versionHash) {
        int encOffset = buffer.position();
        encOffset = ~(encOffset - dataStartPos);
        encOffset *= versionHash;
        encOffset -= WzAESConstant.WZ_OFFSET_CONSTANT;
        encOffset = Integer.rotateLeft(encOffset, encOffset & 0x1F);
        int writeOffset = (int) (encOffset ^ (value - (dataStartPos * 2)));
        putInt(writeOffset);
    }

    /* Output --------------------------------------------------------------------------------------------------------*/
    public byte[] output() {
        byte[] array = buffer.array();
        int position = buffer.position();
        return Arrays.copyOf(array, position);
    }
}
