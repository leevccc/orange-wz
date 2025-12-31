package orange.wz.provider.properties;

import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

import java.awt.image.BufferedImage;
import java.util.List;

public class WzCanvasProperty extends WzExtended {
    private WzPngProperty png;

    public WzCanvasProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.CANVAS_PROPERTY, parent, wzImage);
    }

    public WzCanvasProperty(String name, int width, int height, int format, int scale, byte[] imageBytes, WzObject parent, WzImage wzImage) {
        this(name, parent, wzImage);
        png = new WzPngProperty(name, width, height, format, scale, imageBytes, this, wzImage);
    }

    // Png -------------------------------------------------------------------------------------------------------------
    public byte[] getImageBytes(boolean saveInMem) {
        return png.getImageBytes(saveInMem);
    }

    public int getWidth() {
        return png.getWidth();
    }

    public int getHeight() {
        return png.getHeight();
    }

    public WzPngFormat getFormat() {
        return png.getFormat();
    }

    public int getScale() {
        return png.getScale();
    }

    public BufferedImage getPngImage(boolean saveInMem) {
        return png.getImage(saveInMem);
    }

    public void initPngProperty(String name, WzObject parent, WzImage wzImage) {
        png = new WzPngProperty(name, parent, wzImage);
    }

    public void initPngProperty(String name, WzObject parent, WzImage wzImage, BinaryReader reader) {
        png = new WzPngProperty(name, parent, wzImage);
        png.setData(reader);
    }

    public void setPng(BufferedImage pngImage, WzPngFormat pngFormat) {
        png.setImage(pngImage, pngFormat);
    }

    public void setPng(BufferedImage pngImage) {
        png.setImage(pngImage);
    }

    public void setFormat(WzPngFormat format, int scale) {
        png.setFormat(format, scale);
    }

    // Override --------------------------------------------------------------------------------------------------------
    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzExtendedType.CANVAS.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.putByte((byte) 0);
        List<WzImageProperty> properties = children.get();
        if (!properties.isEmpty()) {
            writer.putByte((byte) 1);
            WzImage.writeListValue(writer, properties);
        } else {
            writer.putByte((byte) 0);
        }

        // Image Info
        writer.writeCompressedInt(png.getWidth());
        writer.writeCompressedInt(png.getHeight());
        writer.writeCompressedInt(png.getFormat().getValue());
        writer.putByte((byte) png.getScale());
        writer.putInt(0);

        // Write image
        byte[] bytes = png.getCompressedBytes(false);
        writer.putInt(bytes.length + 1);
        writer.putByte((byte) 0);
        writer.putBytes(bytes);
    }

    @Override
    public WzCanvasProperty deepClone(WzObject parent) {
        WzCanvasProperty clone = new WzCanvasProperty(name, parent, null);
        clone.png = png.deepClone(clone);
        for (WzImageProperty property : children.get()) {
            clone.addChild(property.deepClone(clone));
        }
        return clone;
    }
}
