package orange.wz.lib.audio;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class Mp3WaveFormat extends WaveFormat {
    private Mp3WaveFormatId id;
    private Mp3WaveFormatFlags flags;
    private short blockSize;
    private short framesPerBlock;
    private short codecDelay;

    /**
     * Marshal.SizeOf<Mp3WaveFormat>() 各个成员参数占用内存大小的和 Wave 18 + Mp3 Extra 12
     */
    public final static int structSize = 30;

    public Mp3WaveFormat(Mp3WaveFormat other) {
        super(other); // 调用父类复制构造函数
        if (other != null) {
            id = other.id;
            flags = other.flags;
            blockSize = other.blockSize;
            framesPerBlock = other.framesPerBlock;
            codecDelay = other.codecDelay;
        }
    }

    @Override
    public Mp3WaveFormat deepCopy() {
        return new Mp3WaveFormat(this);
    }
}
