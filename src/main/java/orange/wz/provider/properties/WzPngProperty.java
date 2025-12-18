package orange.wz.provider.properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzMutableKey;
import orange.wz.provider.tools.WzType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@Getter
@Slf4j
public class WzPngProperty extends WzImageProperty {
    private int width;
    private int height;
    private int format;
    private int format2;
    private int offset;
    private byte[] compressedBytes;
    private boolean listWzUsed;
    private BufferedImage png;

    public WzPngProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.PNG_PROPERTY, parent, wzImage);
    }

    public void setData(BinaryReader reader) {
        width = reader.readCompressedInt();
        height = reader.readCompressedInt();
        format = reader.readCompressedInt();
        format2 = reader.getByte();
        reader.skip(4); // 跳过4个字节
        offset = reader.getPosition();
        int len = reader.getInt() - 1;
        reader.skip(1); // 跳过1个字节
        if (len > 0) {
            compressedBytes = reader.getBytes(len);
            parse(reader.getWzMutableKey());
        }
    }

    private void parse(WzMutableKey wzMutableKey) {
        byte[] rawBytes = getRawImage(wzMutableKey); // 注意是小端序的 b g r a
        if (rawBytes.length == 0) {
            png = null;
            return;
        }

        WzPngFormat pngFormat = WzPngFormat.getByValue(format + format2);

        byte[] argbByteArr;
        int[] argbIntArr;
        int imageType = getImageType(pngFormat);
        BufferedImage bmp = new BufferedImage(width, height, imageType);

        switch (pngFormat) {
            case WzPngFormat.Format1: // 16 bit argb4444
                int b, g;
                argbByteArr = new byte[width * height * 4];
                for (int i = 0; i < rawBytes.length; i++) {
                    b = rawBytes[i] & 0x0F;
                    b |= (b << 4);
                    argbByteArr[i * 2] = (byte) b;
                    g = rawBytes[i] & 0xF0;
                    g |= (g >> 4);
                    argbByteArr[i * 2 + 1] = (byte) g;
                }

                argbIntArr = getIntArgbFromByteArgb(argbByteArr);
                bmp.setRGB(0, 0, width, height, argbIntArr, 0, width);
                break;
            case WzPngFormat.Format2: // 32 bit argb8888
                // UI.wz/UIWindow2.img/MonsterKilling/Count/keyBackgrd/ing
                argbIntArr = getIntArgbFromByteArgb(rawBytes);
                bmp.setRGB(0, 0, width, height, argbIntArr, 0, width);
                break;
            case WzPngFormat.Format3: // 黑白缩略图
                // FullPath = "Map.wz\\Back\\blackHeaven.img\\back\\98"
                // 感谢 https://forum.ragezone.com/threads/new-wz-png-format-decode-code.1114978/ 下面的 257 1026 2050 同样来自于此
                int[] argb2 = new int[width * height];
                int index;
                int index2;
                int p;
                int w = ((int) Math.ceil(width / 4.0));
                int h = ((int) Math.ceil(height / 4.0));
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        index = (x + y * w) * 2; // 原像素索引
                        index2 = x * 4 + y * width * 4; // 目标像素索引
                        p = (rawBytes[index] & 0x0F) | ((rawBytes[index] & 0x0F) << 4);
                        p |= ((rawBytes[index] & 0xF0) | ((rawBytes[index] & 0xF0) >> 4)) << 8;
                        p |= ((rawBytes[index + 1] & 0x0F) | ((rawBytes[index + 1] & 0x0F) << 4)) << 16;
                        p |= ((rawBytes[index + 1] & 0xF0) | ((rawBytes[index] & 0xF0) >> 4)) << 24;

                        for (int i = 0; i < 4; i++) {
                            if (x * 4 + i < width) {
                                argb2[index2 + i] = p;
                            } else {
                                break;
                            }
                        }
                    }
                    // 复制行
                    index2 = y * width * 4;
                    for (int j = 1; j < 4; j++) {
                        if (y * 4 + j < height) {
                            System.arraycopy(argb2, index2, argb2, index2 + j * width, width);
                        } else {
                            break;
                        }
                    }
                }
                bmp.setRGB(0, 0, width, height, argb2, 0, width);
                break;
            case WzPngFormat.Format257:
                // "Npc.wz\\2570101.img\\info\\illustration2\\face\\0"
                // 2570107 is a decent example. Used KMS 353
                // PixelFormat.Format16bppArgb1555 在 BufferImage 中原本并不存在，要转换成 IntArray 实现
                int sourceStride = width * 2;
                ByteBuffer sourceBuffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN);
                for (int y = 0; y < height; y++) {
                    int sourcePos = y * sourceStride;
                    sourceBuffer.position(sourcePos);

                    for (int x = 0; x < width; x++) {
                        short argb1555 = sourceBuffer.getShort();
                        int iArgb = convertARGB1555ToARGB(argb1555);
                        bmp.setRGB(x, y, iArgb);
                    }
                }
                break;
            case WzPngFormat.Format513: // 16 bit rgb565
                // UI.wz/Logo.img v95
                argbIntArr = getIntArgbFromUShortRgb565(rawBytes);
                bmp.setRGB(0, 0, width, height, argbIntArr, 0, width);
                break;
            case WzPngFormat.Format517: // 16 bit rgb565 缩略图
                // FullPath = "Map.wz\\Back\\midForest.img\\back\\0"
                byte[] decompress = new byte[width * height * 2];

                int lineIndex = 0;
                for (int j0 = 0, j1 = height / 16; j0 < j1; j0++) {
                    var dstIndex = lineIndex;
                    for (int i0 = 0, i1 = width / 16; i0 < i1; i0++) {
                        int idx = (i0 + j0 * i1) * 2;
                        byte b0 = rawBytes[idx];
                        byte b1 = rawBytes[idx + 1];
                        for (int k = 0; k < 16; k++) {
                            decompress[dstIndex++] = b0;
                            decompress[dstIndex++] = b1;
                        }
                    }
                    for (int k = 1; k < 16; k++) {
                        System.arraycopy(decompress, lineIndex, decompress, dstIndex, width * 2);
                        dstIndex += width * 2;
                    }

                    lineIndex += width * 32;
                }

                argbIntArr = getIntArgbFromUShortRgb565(decompress);
                bmp.setRGB(0, 0, width, height, argbIntArr, 0, width);
                break;
            case WzPngFormat.Format1026:
                // Familiar_000.wz\9960688.img\attack\info\hit\0
                // Effect_004.wz\Direction17.img\effect\ark\noise\800\0\24
                // Effect_017.wz\EliteMobEff.img\eliteMonster\0\0
                argbByteArr = getPixelDataDXT3(rawBytes, width, height);
                argbIntArr = getIntArgbFromByteArgb(argbByteArr);
                bmp.setRGB(0, 0, width, height, argbIntArr, 0, width);
                break;
            case WzPngFormat.Format2050:
                // Skill_022.wz/40002.img/skill/400021006/effect
                argbByteArr = getPixelDataDXT5(rawBytes, width, height);
                argbIntArr = getIntArgbFromByteArgb(argbByteArr);
                bmp.setRGB(0, 0, width, height, argbIntArr, 0, width);
                break;
        }

        png = bmp;
    }

    private int getImageType(WzPngFormat pngFormat) {
        return switch (pngFormat) {
            case WzPngFormat.Format1, Format2, Format3, Format257, Format1026, Format2050 ->
                    BufferedImage.TYPE_INT_ARGB;
            case Format513, Format517 -> BufferedImage.TYPE_USHORT_565_RGB;
        };
    }

    private byte[] getRawImage(WzMutableKey wzMutableKey) {
        int decompressSize = getDecompressSize();
        byte[] decBuf = new byte[decompressSize]; // decompress byte

        // 使用 try-with-resources 确保资源正确关闭
        try (InflaterInputStream zlib = createZlibStream(compressedBytes, wzMutableKey)) {
            // zlib.read(decBuf, 0, uncompressedSize); 可能一次读不完全部数据，要循环确认，所以有了这个方法
            int totalRead = 0;
            int bytesRead;
            while (totalRead < decompressSize && (bytesRead = zlib.read(decBuf, totalRead, decompressSize - totalRead)) != -1) {
                totalRead += bytesRead;
            }

            return decBuf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getDecompressSize() {
        WzPngFormat pngFormat = WzPngFormat.getByValue(format + format2);
        return switch (pngFormat) {
            case WzPngFormat.Format1, Format257, Format513 -> width * height * 2;
            case Format2, Format3 -> width * height * 4;
            case Format517 -> width * height / 128;
            case Format1026, Format2050 -> width * height;
        };
    }

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

    /**
     * 小端序 b g r a byte[] 转 32bit argb int[]
     */
    private static int[] getIntArgbFromByteArgb(byte[] argb) {
        // 检查输入数组的长度是否为 4 的倍数
        if (argb.length % 4 != 0) {
            throw new IllegalArgumentException("输入的 byte[] 长度必须是 4 的倍数");
        }

        // 计算像素的数量
        int pixelCount = argb.length / 4;
        int[] iargb = new int[pixelCount];

        // 遍历 byte[]，每 4 个字节转换为一个 int
        for (int i = 0; i < pixelCount; i++) {
            int offset = i * 4; // 每个像素的起始位置
            int alpha = (argb[offset + 3] & 0xFF) << 24;   // Alpha 通道
            int red = (argb[offset + 2] & 0xFF) << 16; // 红色通道
            int green = (argb[offset + 1] & 0xFF) << 8; // 绿色通道
            int blue = argb[offset] & 0xFF;        // 蓝色通道

            // 将四个通道组合成一个 int
            iargb[i] = alpha | red | green | blue;
        }

        return iargb;
    }

    /**
     * 小端序 b g r a byte[] 转 16bit rgb565 int[]
     *
     */
    private static int[] getIntArgbFromUShortRgb565(byte[] rgb) {
        if (rgb.length % 2 != 0) {
            throw new IllegalArgumentException("输入的 byte[] 长度必须是 2 的倍数");
        }

        int pixelCount = rgb.length / 2;
        int[] argb = new int[pixelCount];

        for (int i = 0; i < pixelCount; i++) {
            int offset = i * 2;

            // 读取 16 位像素值，根据字节顺序处理
            short pixel;
            // 小端序：低位字节在前
            pixel = (short) (((rgb[offset + 1] & 0xFF) << 8) | (rgb[offset] & 0xFF));

            // 从 RGB565 提取各分量
            int r = (pixel >> 11) & 0x1F;  // 高5位：红色
            int g = (pixel >> 5) & 0x3F;   // 中间6位：绿色
            int b = pixel & 0x1F;          // 低5位：蓝色

            // 扩展到 8位（保持比例）
            r = (r * 255) / 31;    // 5位到8位
            g = (g * 255) / 63;    // 6位到8位
            b = (b * 255) / 31;    // 5位到8位

            // 组合成 ARGB（Alpha = 255，完全不透明）
            argb[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        return argb;
    }

    private static int convertARGB1555ToARGB(short argb1555) {
        // 提取各个分量（ARGB1555是小端序存储）
        int alpha = (argb1555 & 0x8000) != 0 ? 0xFF : 0x00;  // 1位Alpha -> 8位Alpha
        int red = ((argb1555 & 0x7C00) >>> 10) * 255 / 31; // 5位红色 -> 8位红色
        int green = ((argb1555 & 0x03E0) >>> 5) * 255 / 31; // 5位绿色 -> 8位绿色
        int blue = (argb1555 & 0x001F) * 255 / 31; // 5位蓝色 -> 8位蓝色

        // 组合成32位ARGB值
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static byte[] getPixelDataDXT3(byte[] rawData, int width, int height) {
        byte[] pixel = new byte[width * height * 4];

        Color[] colorTable = new Color[4];
        int[] colorIdxTable = new int[16];
        byte[] alphaTable = new byte[16];
        for (int y = 0; y < height; y += 4) {
            for (int x = 0; x < width; x += 4) {
                int off = x * 4 + y * width;
                expandAlphaTableDXT3(alphaTable, rawData, off);
                int u0 = ((rawData[off + 8] & 0xFF) | ((rawData[off + 9] & 0xFF) << 8)); // 小端序读取 short
                int u1 = ((rawData[off + 10] & 0xFF) | ((rawData[off + 11] & 0xFF) << 8)); // 小端序读取 short
                expandColorTable(colorTable, u0, u1);
                expandColorIndexTable(colorIdxTable, rawData, off + 12);

                for (int j = 0; j < 4; j++) {
                    for (int i = 0; i < 4; i++) {
                        setPixel(pixel,
                                x + i,
                                y + j,
                                width,
                                colorTable[colorIdxTable[j * 4 + i]],
                                alphaTable[j * 4 + i]);
                    }
                }
            }
        }

        return pixel;
    }

    private static void expandAlphaTableDXT3(byte[] alpha, byte[] rawData, int offset) {
        for (int i = 0; i < 16; i += 2, offset++) {
            alpha[i] = (byte) (rawData[offset] & 0x0F);
            alpha[i + 1] = (byte) ((rawData[offset] & 0xF0) >> 4);
        }
        for (int i = 0; i < 16; i++) {
            alpha[i] = (byte) ((alpha[i] & 0xFF) | ((alpha[i] & 0xFF) << 4));
        }
    }

    private static void expandColorTable(Color[] color, int c0, int c1) {
        color[0] = rgb565ToColor(c0);
        color[1] = rgb565ToColor(c1);

        if (c0 > c1) {
            color[2] = new Color(
                    (color[0].getRed() * 2 + color[1].getRed() + 1) / 3,
                    (color[0].getGreen() * 2 + color[1].getGreen() + 1) / 3,
                    (color[0].getBlue() * 2 + color[1].getBlue() + 1) / 3
            );
            color[3] = new Color(
                    (color[0].getRed() + color[1].getRed() * 2 + 1) / 3,
                    (color[0].getGreen() + color[1].getGreen() * 2 + 1) / 3,
                    (color[0].getBlue() + color[1].getBlue() * 2 + 1) / 3
            );
        } else {
            color[2] = new Color(
                    (color[0].getRed() + color[1].getRed()) / 2,
                    (color[0].getGreen() + color[1].getGreen()) / 2,
                    (color[0].getBlue() + color[1].getBlue()) / 2
            );
            color[3] = Color.BLACK;
        }
    }

    private static void expandColorIndexTable(int[] colorIndex, byte[] rawData, int offset) {
        for (int i = 0; i < 16; i += 4, offset++) {
            colorIndex[i] = (rawData[offset] & 0x03);
            colorIndex[i + 1] = (rawData[offset] & 0x0c) >> 2;
            colorIndex[i + 2] = (rawData[offset] & 0x30) >> 4;
            colorIndex[i + 3] = (rawData[offset] & 0xc0) >> 6;
        }
    }

    private static void setPixel(byte[] pixelData, int x, int y, int width, Color color, byte alpha) {
        int offset = (y * width + x) * 4;
        pixelData[offset] = (byte) color.getBlue();
        pixelData[offset + 1] = (byte) color.getGreen();
        pixelData[offset + 2] = (byte) color.getRed();
        pixelData[offset + 3] = alpha;
    }

    private static Color rgb565ToColor(int val) {
        final int rgb565_mask_r = 0xf800;
        final int rgb565_mask_g = 0x07e0;
        final int rgb565_mask_b = 0x001f;

        int r = (val & rgb565_mask_r) >> 11;
        int g = (val & rgb565_mask_g) >> 5;
        int b = (val & rgb565_mask_b);

        return new Color(
                (r << 3) | (r >> 2),
                (g << 2) | (g >> 4),
                (b << 3) | (b >> 2)
        );
    }

    private static byte[] getPixelDataDXT5(byte[] rawData, int width, int height) {
        byte[] pixel = new byte[width * height * 4];

        Color[] colorTable = new Color[4];
        int[] colorIdxTable = new int[16];
        byte[] alphaTable = new byte[8];
        int[] alphaIdxTable = new int[16];
        for (int y = 0; y < height; y += 4) {
            for (int x = 0; x < width; x += 4) {
                int off = x * 4 + y * width;
                expandAlphaTableDXT5(alphaTable, rawData[off], rawData[off + 1]);
                expandAlphaIndexTableDXT5(alphaIdxTable, rawData, off + 2);
                int u0 = ((rawData[off + 8] & 0xFF) | ((rawData[off + 9] & 0xFF) << 8)); // 小端序读取 short
                int u1 = ((rawData[off + 10] & 0xFF) | ((rawData[off + 11] & 0xFF) << 8)); // 小端序读取 short
                expandColorTable(colorTable, u0, u1);
                expandColorIndexTable(colorIdxTable, rawData, off + 12);

                for (int j = 0; j < 4; j++) {
                    for (int i = 0; i < 4; i++) {
                        setPixel(pixel,
                                x + i,
                                y + j,
                                width,
                                colorTable[colorIdxTable[j * 4 + i]],
                                alphaTable[alphaIdxTable[j * 4 + i]]);
                    }
                }
            }
        }

        return pixel;
    }

    private static void expandAlphaTableDXT5(byte[] alpha, byte a0, byte a1) {
        alpha[0] = a0;
        alpha[1] = a1;
        if ((a0 & 0xFF) > (a1 & 0xFF)) {
            for (int i = 2; i < 8; i++) {
                alpha[i] = (byte) (((8 - i) * (a0 & 0xFF) + (i - 1) * (a1 & 0xFF) + 3) / 7);
            }
        } else {
            for (int i = 2; i < 6; i++) {
                alpha[i] = (byte) (((6 - i) * (a0 & 0xFF) + (i - 1) * (a1 & 0xFF) + 2) / 5);
            }
            alpha[6] = 0;
            alpha[7] = (byte) 255;
        }
    }

    private static void expandAlphaIndexTableDXT5(int[] alphaIndex, byte[] rawData, int offset) {
        for (int i = 0; i < 16; i += 8, offset += 3) {
            int flags = (rawData[offset] & 0xFF)
                    | ((rawData[offset + 1] & 0xFF) << 8)
                    | ((rawData[offset + 2] & 0xFF) << 16);
            for (int j = 0; j < 8; j++) {
                int mask = 0x07 << (3 * j);
                alphaIndex[i + j] = (flags & mask) >> (3 * j);
            }
        }
    }

    public String getBase64() {
        if (png == null) return "";
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(png, "PNG", stream);
            byte[] imageBytes = stream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return "";
    }

    public WzPngFormat getPngFormat() {
        return WzPngFormat.getByValue(format + format2);
    }

    private BufferedImage readBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Base64字符串不能为空");
        }

        try {
            String cleanedBase64 = cleanBase64String(base64String);
            byte[] imageBytes = Base64.getDecoder().decode(cleanedBase64);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                BufferedImage image = ImageIO.read(bis);
                if (image == null) {
                    throw new IOException("无法解码图片数据，可能不是支持的图片格式");
                }
                return image;
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Base64格式错误: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("图片解码错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 清理Base64字符串，移除Data URL前缀
     */
    private static String cleanBase64String(String base64String) {
        if (base64String.startsWith("data:image")) {
            return base64String.split(",")[1];
        }
        return base64String;
    }

    public void setPng(String base64, WzMutableKey wzMutableKey, WzPngFormat pngFormat) {
        png = readBase64(base64);
        compressPng(wzMutableKey, pngFormat);
    }

    public void setPng(BufferedImage pngImage, WzPngFormat pngFormat) {
        png = pngImage;
        compressPng(wzImage.getReader().getWzMutableKey(), pngFormat);
    }

    public void setPng(BufferedImage pngImage, WzMutableKey wzMutableKey) {
        png = pngImage;
        compressPng(wzMutableKey, getPngFormat());
    }

    public void compressPng(WzMutableKey wzMutableKey, WzPngFormat pngFormat) {
        format = pngFormat.getValue();
        format2 = 0;
        width = png.getWidth();
        height = png.getHeight();

        compressedBytes = compress(png, WzPngFormat.getByValue(format + format2));
        compressedBytes = zlibCompress(compressedBytes);
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

        // parse(wzKey);
    }

    private byte[] zlibCompress(byte[] decompressedBuffer) {
        ByteArrayOutputStream memStream = new ByteArrayOutputStream();

        try (DeflaterOutputStream zip = new DeflaterOutputStream(memStream)) {
            zip.write(decompressedBuffer);
        } catch (IOException e) {
            throw new RuntimeException("压缩失败", e);
        }

        return memStream.toByteArray();
    }

    private byte[] compress(BufferedImage bmp, WzPngFormat format) {
        return switch (format) { // todo 513 517
            case WzPngFormat.Format1 -> compressFormat1(bmp);
            case WzPngFormat.Format3, WzPngFormat.Format1026 -> compressDXT3(bmp);
            case WzPngFormat.Format257 -> compressFormat257(bmp);
            case WzPngFormat.Format2050 -> compressFormat2050(bmp);
            case WzPngFormat.Format2, WzPngFormat.Format513, WzPngFormat.Format517 -> compressFormat2(bmp);
            case null -> compressFormat2(bmp);
        };
    }

    private byte[] compressFormat1(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] buf = new byte[width * height * 2];
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                byte b = (byte) ((pixel) & 0xFF);
                byte g = (byte) ((pixel >> 8) & 0xFF);
                byte r = (byte) ((pixel >> 16) & 0xFF);
                byte a = (byte) ((pixel >> 24) & 0xFF);

                buf[index++] = (byte) (((b >> 4) & 0x0F) | (g & 0xF0));
                buf[index++] = (byte) (((r >> 4) & 0x0F) | (a & 0xF0));
            }
        }
        return buf;
    }

    private static byte[] compressFormat2(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] buf = new byte[width * height * 4];
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                buf[index++] = (byte) ((pixel) & 0xFF);  // B
                buf[index++] = (byte) ((pixel >> 8) & 0xFF);  // G
                buf[index++] = (byte) ((pixel >> 16) & 0xFF); // R
                buf[index++] = (byte) ((pixel >> 24) & 0xFF); // A
            }
        }
        return buf;
    }

    private static byte[] compressDXT3(BufferedImage bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        int blockCountX = width / 4;
        int blockCountY = height / 4;
        byte[] buf = new byte[blockCountX * blockCountY * 16]; // 16 bytes per 4x4 block
        int bufIndex = 0;

        for (int by = 0; by < blockCountY; by++) {
            for (int bx = 0; bx < blockCountX; bx++) {
                // Extract 4x4 block pixels
                Color[] block = new Color[16];
                for (int j = 0; j < 4; j++) {
                    for (int i = 0; i < 4; i++) {
                        int x = bx * 4 + i;
                        int y = by * 4 + j;
                        if (x < width && y < height) {
                            int rgb = bmp.getRGB(x, y);
                            int a = (rgb >> 24) & 0xFF;
                            int r = (rgb >> 16) & 0xFF;
                            int g = (rgb >> 8) & 0xFF;
                            int b = rgb & 0xFF;
                            block[j * 4 + i] = new Color(r, g, b, a);
                        } else {
                            // Handle edge cases with transparent black
                            block[j * 4 + i] = new Color(0, 0, 0, 0);
                        }
                    }
                }

                // Compress alpha (4 bits per pixel, 8 bytes total)
                for (int i = 0; i < 16; i += 2) {
                    byte a0 = (byte) (block[i].getAlpha() >> 4);     // Low nibble
                    byte a1 = (byte) (block[i + 1].getAlpha() >> 4); // High nibble
                    buf[bufIndex++] = (byte) ((a1 << 4) | a0);
                }

                // Compress color (DXT1 style: 2 RGB565 colors + 4 indices)
                int[] colorsResult = compressBlockColors(block);
                int c0 = colorsResult[0];
                int c1 = colorsResult[1];
                Color[] colorTable = new Color[4];
                expandColorTable(colorTable, c0, c1);
                byte[] indices = computeColorIndices(block, colorTable);

                // Write color data (little-endian)
                buf[bufIndex++] = (byte) (c0 & 0xFF);
                buf[bufIndex++] = (byte) ((c0 >> 8) & 0xFF);
                buf[bufIndex++] = (byte) (c1 & 0xFF);
                buf[bufIndex++] = (byte) ((c1 >> 8) & 0xFF);
                for (int i = 0; i < 4; i++) {
                    buf[bufIndex++] = indices[i];
                }
            }
        }
        return buf;
    }

    private static byte[] computeColorIndices(Color[] block, Color[] colors) {
        byte[] indices = new byte[4]; // 4 bytes, one per row
        for (int j = 0; j < 4; j++) {
            byte row = 0;
            for (int i = 0; i < 4; i++) {
                Color pixel = block[j * 4 + i];
                int bestIndex = 0;
                double minDist = Double.MAX_VALUE;
                for (int k = 0; k < 4; k++) {
                    double dist = colorDistance(pixel, colors[k]);
                    if (dist < minDist) {
                        minDist = dist;
                        bestIndex = k;
                    }
                }
                row |= (byte) (bestIndex << (i * 2));
            }
            indices[j] = row;
        }
        return indices;
    }

    private static double colorDistance(Color c1, Color c2) {
        int dr = c1.getRed() - c2.getRed();
        int dg = c1.getGreen() - c2.getGreen();
        int db = c1.getBlue() - c2.getBlue();
        return dr * dr + dg * dg + db * db; // Simple Euclidean distance (RGB only)
    }

    private static int[] compressBlockColors(Color[] block) {
        // Simple min/max quantization for RGB
        int minR = 255, minG = 255, minB = 255;
        int maxR = 0, maxG = 0, maxB = 0;
        for (Color color : block) {
            minR = Math.min(minR, color.getRed());
            minG = Math.min(minG, color.getGreen());
            minB = Math.min(minB, color.getBlue());
            maxR = Math.max(maxR, color.getRed());
            maxG = Math.max(maxG, color.getGreen());
            maxB = Math.max(maxB, color.getBlue());
        }

        // Convert to RGB565
        int c0 = colorToRGB565((byte) maxR, (byte) maxG, (byte) maxB);
        int c1 = colorToRGB565((byte) minR, (byte) minG, (byte) minB);

        return new int[]{c0, c1};
    }

    private static int colorToRGB565(byte r, byte g, byte b) {
        int r5 = ((r & 0xFF) * 31) / 255;
        int g6 = ((g & 0xFF) * 63) / 255;
        int b5 = ((b & 0xFF) * 31) / 255;
        return ((r5 << 11) | (g6 << 5) | b5);
    }

    private static byte[] compressFormat257(BufferedImage bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        byte[] buf = new byte[width * height * 2];
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bmp.getRGB(x, y);
                int b = (pixel) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int a = (pixel >> 24) & 0xFF;

                int a1 = (a >= 128 ? 1 : 0); // 1-bit alpha
                int r5 = (r * 31) / 255;
                int g5 = (g * 31) / 255;
                int b5 = (b * 31) / 255;

                int argb1555 = (a1 << 15) | (r5 << 10) | (g5 << 5) | b5;
                buf[index++] = (byte) (argb1555 & 0xFF);
                buf[index++] = (byte) ((argb1555 >> 8) & 0xFF);
            }
        }
        return buf;
    }

    private static byte[] compressFormat2050(BufferedImage bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        int blockCountX = width / 4;
        int blockCountY = height / 4;
        byte[] buf = new byte[blockCountX * blockCountY * 16];
        int bufIndex = 0;

        for (int by = 0; by < blockCountY; by++) {
            for (int bx = 0; bx < blockCountX; bx++) {
                // Extract 4x4 block pixels
                Color[] block = new Color[16];
                for (int j = 0; j < 4; j++) {
                    for (int i = 0; i < 4; i++) {
                        int x = bx * 4 + i;
                        int y = by * 4 + j;
                        if (x < width && y < height) {
                            int pixel = bmp.getRGB(x, y);
                            int b = (pixel) & 0xFF;
                            int g = (pixel >> 8) & 0xFF;
                            int r = (pixel >> 16) & 0xFF;
                            int a = (pixel >> 24) & 0xFF;
                            block[j * 4 + i] = new Color(r, g, b, a);
                        } else {
                            // Handle edge cases with transparent black
                            block[j * 4 + i] = new Color(0, 0, 0, 0);
                        }
                    }
                }

                // Compress alpha
                byte[] alphaResult = compressBlockAlphaDXT5(block);
                byte a0 = alphaResult[0];
                byte a1 = alphaResult[1];
                int[] alphaIndices = new int[16];
                for (int i = 0; i < 16; i++) {
                    alphaIndices[i] = alphaResult[i + 2] & 0xFF; // Convert byte to int
                }

                buf[bufIndex++] = a0;
                buf[bufIndex++] = a1;
                long flags = 0; // 48-bit value for 16 3-bit indices
                for (int i = 0; i < 16; i++) {
                    flags |= (long) alphaIndices[i] << (i * 3);
                }
                buf[bufIndex++] = (byte) (flags & 0xFF);
                buf[bufIndex++] = (byte) ((flags >> 8) & 0xFF);
                buf[bufIndex++] = (byte) ((flags >> 16) & 0xFF);
                buf[bufIndex++] = (byte) ((flags >> 24) & 0xFF);
                buf[bufIndex++] = (byte) ((flags >> 32) & 0xFF);
                buf[bufIndex++] = (byte) ((flags >> 40) & 0xFF);

                // Compress color
                int[] colorsResult = compressBlockColors(block);
                int c0 = colorsResult[0];
                int c1 = colorsResult[1];
                Color[] colorTable = new Color[4];
                expandColorTable(colorTable, c0, c1);
                byte[] indices = computeColorIndices(block, colorTable);

                // Write color data
                buf[bufIndex++] = (byte) (c0 & 0xFF);
                buf[bufIndex++] = (byte) ((c0 >> 8) & 0xFF);
                buf[bufIndex++] = (byte) (c1 & 0xFF);
                buf[bufIndex++] = (byte) ((c1 >> 8) & 0xFF);
                for (int i = 0; i < 4; i++) {
                    buf[bufIndex++] = indices[i];
                }
            }
        }
        return buf;
    }

    private static byte[] compressBlockAlphaDXT5(Color[] block) {
        // Find min/max alpha
        int minA = 255, maxA = 0;
        for (Color color : block) {
            int alpha = color.getAlpha();
            minA = Math.min(minA, alpha);
            maxA = Math.max(maxA, alpha);
        }

        byte a0 = (byte) maxA;
        byte a1 = (byte) minA;

        byte[] alphaTable = new byte[8];
        expandAlphaTableDXT5(alphaTable, a0, a1);

        int[] indices = new int[16];
        for (int i = 0; i < 16; i++) {
            int alpha = block[i].getAlpha();
            int bestIndex = 0;
            int minDiff = Integer.MAX_VALUE;
            for (int j = 0; j < 8; j++) {
                int diff = Math.abs(alpha - (alphaTable[j] & 0xFF));
                if (diff < minDiff) {
                    minDiff = diff;
                    bestIndex = j;
                }
            }
            indices[i] = bestIndex;
        }

        // Return a0, a1 and indices in one array
        byte[] result = new byte[18]; // 2 bytes for a0, a1 + 16 ints for indices
        result[0] = a0;
        result[1] = a1;
        for (int i = 0; i < 16; i++) {
            result[i + 2] = (byte) indices[i];
        }
        return result;
    }

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
        clone.format2 = format2;
        clone.listWzUsed = false;
        clone.png = readBase64(getBase64());

        return clone;
    }
}
