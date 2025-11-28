package orange.wz.lib.audio;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum Mp3WaveFormatId {
    Unknown(0),
    Mpeg(1),
    ConstantFrameSize(2);

    private final int value;
    private static final Map<Integer, Mp3WaveFormatId> map = new HashMap<>();

    static {
        for (Mp3WaveFormatId id : Mp3WaveFormatId.values()) {
            map.put(id.value, id);
        }
    }

    Mp3WaveFormatId(int value) {
        this.value = value;
    }

    public static Mp3WaveFormatId valueOf(int value) {
        return map.get(value);
    }

}
