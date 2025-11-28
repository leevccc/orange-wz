package orange.wz.lib;

import orange.wz.lib.properties.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter
@SuperBuilder
public abstract class WzImageProperty extends WzObject {
    private final String type = "property";

    public static List<WzImageProperty> parsePropertyList(int offset, BinaryReader reader, WzObject parent) {
        int entryCount = reader.readCompressedInt();
        List<WzImageProperty> properties = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            String name = reader.readStringBlock(offset);
            byte pType = reader.getByte();
            switch (pType) {
                case 0: // null
                    properties.add(WzNullProperty.builder().name(name).parent(parent).build());
                    break;
                case 2, 11: // short
                    properties.add(WzShortProperty.builder().name(name).parent(parent).value(reader.getShort()).build());
                    break;
                case 3, 19: // int
                    properties.add(WzIntProperty.builder().name(name).parent(parent).value(reader.readCompressedInt()).build());
                    break;
                case 20: // long
                    properties.add(WzLongProperty.builder().name(name).parent(parent).value(reader.readCompressedLong()).build());
                    break;
                case 4: // float
                    properties.add(WzFloatProperty.builder().name(name).parent(parent).value(reader.readCompressedFloat()).build());
                    break;
                case 5: // double
                    properties.add(WzDoubleProperty.builder().name(name).parent(parent).value(reader.getDouble()).build());
                    break;
                case 8: // string
                    properties.add(WzStringProperty.builder().name(name).parent(parent).value(reader.readStringBlock(offset)).build());
                    break;
                case 9:
                    int eob = reader.getInt() + reader.getPosition();
                    WzImageProperty exProp = parseExtendedProp(reader, offset, eob, name, parent);
                    properties.add(exProp);
                    if (reader.getPosition() != eob) reader.setPosition(eob);
                    break;
                default:
                    log.error("Unknown property type at ParsePropertyList, pType = {}", pType);
            }
        }
        return properties;
    }

    private static WzExtended parseExtendedProp(BinaryReader reader, int offset, long endOfBlock, String name, WzObject parent) {
        try {
            final byte stringType = reader.getByte();
            return switch (stringType) {
                case 0x01, WzImage.wzImageHeaderByte_WithOffset ->
                        extractMore(reader, offset, endOfBlock, name, reader.readStringAtOffset(offset + reader.getInt()), parent);
                case 0x00, WzImage.wzImageHeaderByte_WithoutOffset ->
                        extractMore(reader, offset, endOfBlock, name, "", parent);
                default -> throw new Exception("Invalid byte read at ParseExtendedProp");
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static WzExtended extractMore(BinaryReader reader, int offset, long eob, String name, String iname, WzObject parent) {
        if (iname.equalsIgnoreCase("")) {
            iname = reader.readString();
        }
        final WzPropertyType propertyType = WzPropertyType.getByString(iname);
        try {
            switch (propertyType) {
                case WzPropertyType.LIST: {
                    WzListProperty subProp = WzListProperty.builder().name(name).parent(parent).build();
                    reader.jumpPosition(2); // Reserved?
                    subProp.addProperties(WzImageProperty.parsePropertyList(offset, reader, subProp));
                    return subProp;
                }
                case WzPropertyType.CANVAS: {
                    WzCanvasProperty canvasProp = WzCanvasProperty.builder().name(name).parent(parent).build();
                    reader.jumpPosition(1);
                    if (reader.getByte() == 1) {
                        reader.jumpPosition(2);
                        canvasProp.addProperties(WzImageProperty.parsePropertyList(offset, reader, canvasProp));
                    }
                    WzPngProperty png = WzPngProperty.builder().name(name).parent(canvasProp).build();
                    png.setData(reader);
                    canvasProp.setPng(png);
                    return canvasProp;
                }
                case WzPropertyType.VECTOR: {
                    WzVectorProperty vecProp = WzVectorProperty.builder().name(name).parent(parent).build();
                    WzIntProperty xProp = WzIntProperty.builder().name("X").value(reader.readCompressedInt()).parent(vecProp).build();
                    WzIntProperty yProp = WzIntProperty.builder().name("Y").value(reader.readCompressedInt()).parent(vecProp).build();
                    vecProp.setX(xProp);
                    vecProp.setY(yProp);
                    return vecProp;
                }
                case WzPropertyType.CONVEX: {
                    WzConvexProperty convexProp = WzConvexProperty.builder().name(name).parent(parent).build();
                    int convexEntryCount = reader.readCompressedInt();
                    for (int i = 0; i < convexEntryCount; i++) {
                        convexProp.addProperty(parseExtendedProp(reader, offset, 0, name, convexProp));
                    }
                    return convexProp;
                }
                case WzPropertyType.SOUND: {
                    WzSoundProperty soundProp = WzSoundProperty.builder().name(name).parent(parent).build();
                    soundProp.setData(reader);
                    return soundProp;
                }
                case WzPropertyType.UOL: {
                    reader.jumpPosition(1);
                    return switch (reader.getByte()) {
                        case 0 ->
                                WzUOLProperty.builder().name(name).parent(parent).uol(reader.readString()).build();
                        case 1 ->
                                WzUOLProperty.builder().name(name).parent(parent).uol(reader.readStringAtOffset(offset + reader.getInt())).build();
                        default -> throw new Exception("Unsupported UOL type");
                    };
                }
                // case WzPropertyType.RAW_DATA: {  // GMS v220++
                //     WzRawDataProperty rawData = WzRawDataProperty.builder().name(name).parent(parent).build();
                //     rawData.setData(reader);
                //     return rawData;
                // }
                default: {
                    throw new Exception("Unknown iname: " + iname);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void writeValue(BinaryWriter writer);

    public abstract WzImageProperty deepClone(WzObject parent);
}
