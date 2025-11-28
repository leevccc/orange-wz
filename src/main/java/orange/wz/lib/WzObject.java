package orange.wz.lib;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
@Setter
@Getter
public abstract class WzObject {
    private WzObject parent;
    private String name;
}
