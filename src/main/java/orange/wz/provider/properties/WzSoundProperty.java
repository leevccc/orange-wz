package orange.wz.provider.properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.audio.*;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzMutableKey;
import orange.wz.provider.tools.WzType;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
public class WzSoundProperty extends WzExtended {
    private static final byte[] soundHeader = new byte[]{
            0x02,
            (byte) 0x83, (byte) 0xEB, 0x36, (byte) 0xE4, 0x4F, 0x52, (byte) 0xCE, 0x11, (byte) 0x9F, 0x53, 0x00, 0x20, (byte) 0xAF, 0x0B, (byte) 0xA7, 0x70,
            (byte) 0x8B, (byte) 0xEB, 0x36, (byte) 0xE4, 0x4F, 0x52, (byte) 0xCE, 0x11, (byte) 0x9F, 0x53, 0x00, 0x20, (byte) 0xAF, 0x0B, (byte) 0xA7, 0x70,
            0x00,
            0x01,
            (byte) 0x81, (byte) 0x9F, 0x58, 0x05, 0x56, (byte) 0xC3, (byte) 0xCE, 0x11, (byte) 0xBF, 0x01, 0x00, (byte) 0xAA, 0x00, 0x55, 0x59, 0x5A
    };

    private byte[] fileBytes;
    @Getter
    private int lenMs;
    private byte[] header;
    private boolean headerEncrypted = false; // List.wz, header is encrypted
    private int offset;
    private int soundDataLen;
    private WaveFormat waveFormat;

    public WzSoundProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.SOUND_PROPERTY, parent, wzImage);
    }

    public void setSound(String base64String, WzMutableKey wzMutableKey) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Base64字符串不能为空");
        }

        byte[] soundBytes = Base64.getDecoder().decode(base64String);
        Mp3FileReader reader = new Mp3FileReader(soundBytes);
        waveFormat = reader.getWaveFormat();
        lenMs = reader.getLenMs();
        rebuildHeader(wzMutableKey);
        fileBytes = soundBytes;
    }

    public void setData(BinaryReader reader) {
        reader.skip(1);

        soundDataLen = reader.readCompressedInt();
        lenMs = reader.readCompressedInt();

        byte[] soundHeaderBytes = reader.getBytes(soundHeader.length);
        int wavFormatLen = reader.getByte();
        byte[] waveFormatBytes = reader.getBytes(wavFormatLen);

        header = ByteBuffer.allocate(soundHeaderBytes.length + 1 + waveFormatBytes.length)
                .put(soundHeaderBytes)
                .put((byte) wavFormatLen)  // 或者直接使用读取的字节值
                .put(waveFormatBytes)
                .array();

        parseWzSoundPropertyHeader(reader.getWzMutableKey(), waveFormatBytes);

        offset = reader.getPosition();
        fileBytes = reader.getBytes(soundDataLen);
    }

    private void parseWzSoundPropertyHeader(WzMutableKey wzMutableKey, byte[] waveFormatBytes) {
        if (waveFormatBytes.length < WaveFormat.structSize) {
            return;
        }

        // 解析 wave 头信息
        WaveFormat wavFmt = bytesToWaveStruct(waveFormatBytes);
        if (WaveFormat.structSize + wavFmt.getExtraSize() != waveFormatBytes.length) {
            // 尝试用key解密
            for (int i = 0; i < waveFormatBytes.length; i++) {
                waveFormatBytes[i] ^= wzMutableKey.get(i);
            }
            wavFmt = bytesToWaveStruct(waveFormatBytes);

            if (WaveFormat.structSize + wavFmt.getExtraSize() != waveFormatBytes.length) {
                log.error("解析音频头失败");
                return;
            }
            headerEncrypted = true;
        }

        // 解析 mp3 头信息
        if (wavFmt.getWaveFormatTag() == WaveFormatEncoding.MPEGLAYER3 && waveFormatBytes.length >= Mp3WaveFormat.structSize) {
            waveFormat = bytesToMp3WaveStruct(waveFormatBytes);
        } else if (wavFmt.getWaveFormatTag() == WaveFormatEncoding.PCM) {
            waveFormat = wavFmt;
        } else {
            log.error("未知的 wave 编码");
        }
    }

    public void rebuildHeader(WzMutableKey wzMutableKey) {
        BinaryWriter writer = new BinaryWriter();
        writer.putBytes(soundHeader);
        byte[] wavHeader = mp3StructToBytes((Mp3WaveFormat) waveFormat);
        if (headerEncrypted) {
            for (int i = 0; i < wavHeader.length; i++) {
                wavHeader[i] ^= wzMutableKey.get(i);
            }
        }
        writer.putByte((byte) wavHeader.length);
        writer.putBytes(wavHeader);
        header = writer.output();
    }

    private WaveFormat bytesToWaveStruct(byte[] waveFormatBytes) {
        BinaryReader reader = new BinaryReader(waveFormatBytes);
        return WaveFormat.builder()
                .waveFormatTag(WaveFormatEncoding.valueOf(reader.getShort()))
                .channels(reader.getShort())
                .sampleRate(reader.getInt())
                .averageBytesPerSecond(reader.getInt())
                .blockAlign(reader.getShort())
                .bitsPerSample(reader.getShort())
                .extraSize(reader.getShort())
                .build();
    }

    private WaveFormat bytesToMp3WaveStruct(byte[] waveFormatBytes) {
        BinaryReader reader = new BinaryReader(waveFormatBytes);
        return Mp3WaveFormat.builder()
                .waveFormatTag(WaveFormatEncoding.valueOf(reader.getShort()))
                .channels(reader.getShort())
                .sampleRate(reader.getInt())
                .averageBytesPerSecond(reader.getInt())
                .blockAlign(reader.getShort())
                .bitsPerSample(reader.getShort())
                .extraSize(reader.getShort())
                .id(Mp3WaveFormatId.valueOf(reader.getShort()))
                .flags(Mp3WaveFormatFlags.valueOf(reader.getInt()))
                .blockSize(reader.getShort())
                .framesPerBlock(reader.getShort())
                .codecDelay(reader.getShort())
                .build();
    }

    private byte[] mp3StructToBytes(Mp3WaveFormat waveFormat) {
        BinaryWriter writer = new BinaryWriter();
        writer.putShort((short) waveFormat.getWaveFormatTag().getValue());
        writer.putShort(waveFormat.getChannels());
        writer.putInt(waveFormat.getSampleRate());
        writer.putInt(waveFormat.getAverageBytesPerSecond());
        writer.putShort(waveFormat.getBlockAlign());
        writer.putShort(waveFormat.getBitsPerSample());
        writer.putShort(waveFormat.getExtraSize());
        writer.putShort((short) waveFormat.getId().getValue());
        writer.putInt(waveFormat.getFlags().getValue());
        writer.putShort(waveFormat.getBlockSize());
        writer.putShort(waveFormat.getFramesPerBlock());
        writer.putShort(waveFormat.getCodecDelay());

        return writer.output();
    }

    public String getBase64() {
        if (fileBytes == null) return "";
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    public String getHeaderBase64() {
        if (header == null) return "";
        return Base64.getEncoder().encodeToString(header);
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzExtendedType.SOUND.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.putByte((byte) 0);
        writer.writeCompressedInt(fileBytes.length);
        writer.writeCompressedInt(lenMs);
        writer.putBytes(header);
        writer.putBytes(fileBytes);
    }

    @Override
    public WzSoundProperty deepClone(WzObject parent) {
        WzSoundProperty clone = new WzSoundProperty(name, parent, null);
        clone.fileBytes = Arrays.copyOf(fileBytes, fileBytes.length);
        clone.lenMs = lenMs;
        clone.header = Arrays.copyOf(header, header.length);
        clone.headerEncrypted = headerEncrypted;
        clone.offset = offset;
        clone.soundDataLen = soundDataLen;
        clone.waveFormat = waveFormat.deepCopy();
        return clone;
    }
}
