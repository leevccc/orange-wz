package orange.wz.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import orange.wz.lib.properties.WzPngFormat;
import lombok.Getter;

@Getter
public final class WzNodeValueDto {
    private final String name;
    private final WzNodeType type;
    private final String value;
    private final Integer x;
    private final Integer y;
    private final String png;
    private final WzPngFormat pngFormat;
    private final String mp3;

    public WzNodeValueDto(String name, WzNodeType type, String value, Integer x, Integer y, String png, String mp3) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.x = x;
        this.y = y;
        this.png = png;
        this.pngFormat = null;
        this.mp3 = mp3;
    }

    public WzNodeValueDto(String name, WzNodeType type, String value, Integer x, Integer y, String png, WzPngFormat pngFormat, String mp3) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.x = x;
        this.y = y;
        this.png = png;
        this.pngFormat = pngFormat;
        this.mp3 = mp3;
    }

    @JsonCreator
    public WzNodeValueDto(
            @JsonProperty("id") Integer id,
            @JsonProperty("name") String name,
            @JsonProperty("type") WzNodeType type,
            @JsonProperty("value") String value,
            @JsonProperty("x") Integer x,
            @JsonProperty("y") Integer y,
            @JsonProperty("png") String png,
            @JsonProperty("pngFormat") WzPngFormat pngFormat,
            @JsonProperty("mp3") String mp3) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.x = x;
        this.y = y;
        this.png = png;
        this.pngFormat = pngFormat;
        this.mp3 = mp3;
    }
}
