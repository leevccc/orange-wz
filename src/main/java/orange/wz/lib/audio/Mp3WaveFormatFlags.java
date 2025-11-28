package orange.wz.lib.audio;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum Mp3WaveFormatFlags {
    PaddingIso(0),
    PaddingOn(1),
    PaddingOff(2);

    private final int value;
    private static final Map<Integer, Mp3WaveFormatFlags> map = new HashMap<>();

    static {
        for (Mp3WaveFormatFlags flag : Mp3WaveFormatFlags.values()) {
            map.put(flag.value, flag);
        }
    }

    Mp3WaveFormatFlags(int value) {
        this.value = value;
    }

    public static Mp3WaveFormatFlags valueOf(int value) {
        return map.get(value);
    }

}
