package orange.wz.provider.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@Getter
@Slf4j
public class WzPngProperty extends WzImageProperty {
    private int width;
    private int height;
    private WzPngFormat format;
    private int scale;
    private int offset;
    @Getter(AccessLevel.NONE)
    private byte[] compressedBytes;
    private boolean listWzUsed;
    @Getter(AccessLevel.NONE)
    private BufferedImage image;

    public WzPngProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.PNG_PROPERTY, parent, wzImage);
    }

    public WzPngProperty(String name, int width, int height, int format, int scale, byte[] imageBytes, WzObject parent, WzImage wzImage) {
        this(name, parent, wzImage);
        this.width = width;
        this.height = height;
        this.format = WzPngFormat.getByValue(format);
        this.scale = scale;

        if (imageBytes != null && imageBytes.length > 0) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                BufferedImage image = ImageIO.read(bis);
                if (image == null) {
                    throw new IOException("无法解码图片数据，可能不是支持的图片格式");
                }
                this.image = image;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Setter And Parse ------------------------------------------------------------------------------------------------
    public void setData(BinaryReader reader) {
        width = reader.readCompressedInt();
        height = reader.readCompressedInt();
        format = WzPngFormat.getByValue(reader.readCompressedInt());
        scale = reader.getByte();
        reader.skip(4); // 跳过4个字节
        offset = reader.getPosition();
    }

    public void setImage(BufferedImage image, WzPngFormat format, int scale) {
        this.format = format;
        this.scale = scale;
        this.image = image;
    }

    private void parse(boolean saveInMem) {
        byte[] compressedBytes = getCompressedBytes(saveInMem);
        byte[] rawBytes = decompress(compressedBytes);
        if (rawBytes.length == 0) {
            throw new RuntimeException("rawBytes 是空的");
        }
        BinaryReader rawBytesReader = new BinaryReader(rawBytes); // rawBytes 是小端序的，用Reader读更方便

        int[] argb32 = new int[width * height];
        int imageType = ImgTool.getBufferImageType(format);
        BufferedImage img = new BufferedImage(width, height, imageType);
        int actualScale = getActualScale();

        switch (format) {
            case WzPngFormat.ARGB4444:
                if (getActualScale() == 1) {
                    for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                        argb32[i] = ImgTool.Argb32.fromArgb4444(rawBytesReader.getShort());
                    }
                    img.setRGB(0, 0, width, height, argb32, 0, width);
                } else {
                    // 原来的 Format3 (format 1 + scale 2) 实际上几乎没见过这个 // https://forum.ragezone.com/threads/new-wz-png-format-decode-code.1114978/
                    if (width % actualScale != 0 || height % actualScale != 0) {
                        throw new IllegalArgumentException("width 和 height 不能被 scale 整除");
                    }
                    int rawWidth = width / actualScale;
                    int rawHeight = height / actualScale;
                    int[] rawArgb32 = new int[rawWidth * rawHeight];
                    for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                        rawArgb32[i] = ImgTool.Argb32.fromArgb4444(rawBytesReader.getShort());
                    }
                    argb32 = ImgTool.Argb32.upscale(rawArgb32, rawWidth, rawHeight, actualScale);
                    img.setRGB(0, 0, width, height, argb32, 0, width);
                }
                break;
            case WzPngFormat.ARGB8888:
                // UI.wz/UIWindow2.img/MonsterKilling/Count/keyBackgrd/ing
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB8888 + " 不支持 scale");
                }
                for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                    argb32[i] = rawBytesReader.getInt();
                }
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
            case WzPngFormat.ARGB1555:
                // "Npc.wz\\2570101.img\\info\\illustration2\\face\\0" // 2570107 is a decent example. Used KMS 353
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB1555 + " 不支持 scale");
                }
                for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                    argb32[i] = ImgTool.Argb32.fromArgb1555(rawBytesReader.getShort());
                }
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
            case WzPngFormat.RGB565:
                if (getActualScale() == 1) {
                    // UI.wz/Logo.img v95
                    for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                        argb32[i] = ImgTool.Argb32.fromRgb565(rawBytesReader.getShort());
                    }
                    img.setRGB(0, 0, width, height, argb32, 0, width);
                } else {
                    // 原来的 Format 517 (format 513 + scale 4) // FullPath = "Map.wz\\Back\\midForest.img\\back\\0"
                    if (width % actualScale != 0 || height % actualScale != 0) {
                        throw new IllegalArgumentException("width 和 height 不能被 scale 整除");
                    }
                    int rawWidth = width / actualScale;
                    int rawHeight = height / actualScale;
                    int[] rawArgb32 = new int[rawWidth * rawHeight];
                    for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                        rawArgb32[i] = ImgTool.Argb32.fromRgb565(rawBytesReader.getShort());
                    }
                    argb32 = ImgTool.Argb32.upscale(rawArgb32, rawWidth, rawHeight, actualScale);
                    img.setRGB(0, 0, width, height, argb32, 0, width);
                }
                break;
            case WzPngFormat.DXT3:
                // Familiar_000.wz\9960688.img\attack\info\hit\0
                // Effect_004.wz\Direction17.img\effect\ark\noise\800\0\24
                // Effect_017.wz\EliteMobEff.img\eliteMonster\0\0
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.DXT3 + " 不支持 scale");
                }
                argb32 = ImgTool.Argb32.fromDXT3(rawBytesReader, width, height);
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
            case WzPngFormat.DXT5:
                // Skill_022.wz/40002.img/skill/400021006/effect
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.DXT5 + " 不支持 scale");
                }
                argb32 = ImgTool.Argb32.fromDXT5(rawBytesReader, width, height);
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
        }

        image = img;
    }

    // Getter ----------------------------------------------------------------------------------------------------------
    public BufferedImage getImage(boolean saveInMem) {
        if (image == null) {
            parse(saveInMem);
        }

        return image;
    }

    public byte[] getImageBytes(boolean saveInMem) {
        BufferedImage image = getImage(saveInMem);
        if (image == null) return null;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", stream);
            return stream.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private int getActualScale() {
        return scale > 0 ? (1 << scale) : 1;
    }

    // Decompress ------------------------------------------------------------------------------------------------------
    private InflaterInputStream createZlibStream(byte[] compressedBytes, WzMutableKey wzMutableKey) {
        // C# CompressionMode.Decompress -> Java InflaterInputStream
        // C# CompressionMode.Compress -> Java DeflaterOutputStream
        InflaterInputStream zlib;

        BinaryReader reader = new BinaryReader(compressedBytes);
        int header = reader.getShort() & 0xFFFF;  // 读取无符号短整型
        listWzUsed = header != 0x9C78 && header != 0xDA78 && header != 0x0178 && header != 0x5E78;
        if (!listWzUsed) {
            zlib = new InflaterInputStream(new ByteArrayInputStream(compressedBytes));
        } else {
            reader.setPosition(0);
            BinaryWriter writer = new BinaryWriter();

            while (reader.hasRemaining()) {
                int blockSize = reader.getInt();
                for (int i = 0; i < blockSize; i++) {
                    writer.putByte((byte) (reader.getByte() ^ wzMutableKey.get(i)));
                }
            }
            zlib = new InflaterInputStream(new ByteArrayInputStream(writer.output()));
        }

        return zlib;
    }

    private byte[] decompress(byte[] compressedBytes) {
        WzMutableKey wzMutableKey = wzImage.getReader().getWzMutableKey();
        int size = ImgTool.getRawByteSize(format, getActualScale(), width, height);
        byte[] rawBytes = new byte[size]; // decompress byte
        // 使用 try-with-resources 确保资源正确关闭
        try (InflaterInputStream zlib = createZlibStream(compressedBytes, wzMutableKey)) {
            // zlib.read(decBuf, 0, uncompressedSize); 可能一次读不完全部数据，要循环确认，所以有了这个方法
            int totalRead = 0;
            int bytesRead;
            while (totalRead < size && (bytesRead = zlib.read(rawBytes, totalRead, size - totalRead)) != -1) {
                totalRead += bytesRead;
            }

            return rawBytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Compress --------------------------------------------------------------------------------------------------------
    private byte[] getRawBytes(BufferedImage img, WzPngFormat format) {
        int[] argb32 = ImgTool.Argb32.fromBufferedImage(img);
        BinaryWriter writer = new BinaryWriter(false); // 把数据转为小端序
        int actualScale = getActualScale();
        return switch (format) {
            case WzPngFormat.ARGB4444 -> {
                if (actualScale > 1) {
                    argb32 = ImgTool.Argb32.downscale(argb32, width, height, actualScale, true);
                }

                for (int v : argb32) {
                    writer.putShort(ImgTool.Argb32.toArgb4444(v));
                }
                yield writer.output();
            }
            case WzPngFormat.ARGB8888 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB8888 + " 不支持 scale");
                }
                for (int v : argb32) {
                    writer.putInt(v);
                }
                yield writer.output();
            }
            case WzPngFormat.ARGB1555 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB1555 + " 不支持 scale");
                }
                for (int v : argb32) {
                    writer.putShort(ImgTool.Argb32.toArgb1555(v));
                }
                yield writer.output();
            }
            case WzPngFormat.RGB565 -> {
                if (actualScale > 1) {
                    argb32 = ImgTool.Argb32.downscale(argb32, width, height, actualScale, true);
                }

                for (int v : argb32) {
                    writer.putShort(ImgTool.Argb32.toRgb565(v));
                }
                yield writer.output();
            }
            case WzPngFormat.DXT3 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB1555 + " 不支持 scale");
                }
                ImgTool.Argb32.toDXT3(img,writer);
                yield writer.output();
            }
            case WzPngFormat.DXT5 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB1555 + " 不支持 scale");
                }
                ImgTool.Argb32.toDXT5(img,writer);
                yield writer.output();
            }
        };
    }

    private byte[] zlibCompress(byte[] rawBytes) {
        ByteArrayOutputStream memStream = new ByteArrayOutputStream();

        try (DeflaterOutputStream zip = new DeflaterOutputStream(memStream)) {
            zip.write(rawBytes);
        } catch (IOException e) {
            throw new RuntimeException("压缩失败", e);
        }

        return memStream.toByteArray();
    }

    public void compressImage() {
        WzMutableKey wzMutableKey = wzImage.getReader().getWzMutableKey();
        width = image.getWidth();
        height = image.getHeight();

        byte[] rawBytes = getRawBytes(image, format);
        compressedBytes = zlibCompress(rawBytes);
        if (listWzUsed) {
            BinaryWriter writer = new BinaryWriter(compressedBytes);
            writer.setWzMutableKey(wzMutableKey);
            writer.putInt(2);
            for (int i = 0; i < 2; i++) {
                writer.putByte((byte) (compressedBytes[i] ^ wzMutableKey.get(i)));
            }
            writer.putInt(compressedBytes.length - 2);
            for (int i = 2; i < compressedBytes.length; i++)
                writer.putByte((byte) (compressedBytes[i] ^ wzMutableKey.get(i - 2)));
            compressedBytes = writer.output();
        }
    }

    public byte[] getCompressedBytes(boolean saveInMem) {
        if (compressedBytes == null) {
            byte[] returnBytes = null;
            if (offset != 0) {
                BinaryReader reader = wzImage.getReader();
                int curOffset = reader.getPosition();
                reader.setPosition(offset);
                int len = reader.getInt() - 1;
                reader.skip(1); // 跳过1个字节
                if (len > 0) {
                    returnBytes = reader.getBytes(len);
                }
                reader.setPosition(curOffset);

                if (saveInMem) {
                    compressedBytes = returnBytes;
                }
            } else if (image != null) {
                compressImage();
                returnBytes = compressedBytes;
                if (!saveInMem) {
                    compressedBytes = null;
                }
            }
            return returnBytes;
        }
        return compressedBytes;
    }

    private static BufferedImage deepClone(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = src.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    // Override --------------------------------------------------------------------------------------------------------
    @Override
    public void writeValue(BinaryWriter writer) {
        throw new RuntimeException("WzPngProperty writeValue不能单独调用");
    }

    @Override
    public WzPngProperty deepClone(WzObject parent) {
        WzPngProperty clone = new WzPngProperty(name, parent, null);
        clone.width = width;
        clone.height = height;
        clone.format = format;
        clone.scale = scale;
        clone.listWzUsed = false;
        // clone.compressedBytes = Arrays.copyOf(compressedBytes, compressedBytes.length); // 这个需要用新密钥重新压缩
        clone.image = deepClone(getImage(false));

        return clone;
    }
}
