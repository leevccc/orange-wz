package orange.wz.lib.audio;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
public class WaveFormat {
    private WaveFormatEncoding waveFormatTag; // c# uShort
    private short channels;
    private int sampleRate;
    private int averageBytesPerSecond;
    private short blockAlign;
    private short bitsPerSample;
    private short extraSize;

    /**
     * Marshal.SizeOf<WaveFormat>() 各个成员参数占用内存大小的和
     */
    public final static int structSize = 18;

    public WaveFormat(WaveFormat other) {
        if (other != null) {
            waveFormatTag = other.waveFormatTag;
            channels = other.channels;
            sampleRate = other.sampleRate;
            averageBytesPerSecond = other.averageBytesPerSecond;
            blockAlign = other.blockAlign;
            bitsPerSample = other.bitsPerSample;
            extraSize = other.extraSize;
        }
    }

    public WaveFormat deepCopy() {
        return new WaveFormat(this);
    }
}
