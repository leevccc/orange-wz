package orange.wz.provider.properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public enum WzPngFormat {
    Format1(1),
    Format2(2),
    Format3(3),
    Format257(257),
    Format513(513),
    Format517(517),
    Format1026(1026),
    Format2050(2050);

    private final int value;

    WzPngFormat(int value) {
        this.value = value;
    }

    public static WzPngFormat getByValue(int value) {
        for (WzPngFormat e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        log.warn("未知的图片压缩格式 {}", value);
        throw new RuntimeException("未知的图片压缩格式 " + value);
    }
}
