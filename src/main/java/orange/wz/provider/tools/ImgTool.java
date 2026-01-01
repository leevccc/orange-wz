package orange.wz.provider.tools;

import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.properties.WzPngFormat;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
public final class ImgTool {

    public static class Argb32 {

        public static int[] fromBufferedImage(BufferedImage image) {
            int width = image.getWidth();
            int height = image.getHeight();

            int[] argb32 = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    argb32[y * width + x] = image.getRGB(x, y);
                }
            }
            return argb32;
        }

        /**
         * 将 argb32 的像素点放大成 scale * scale
         *
         * @param argb32    原始像素数组
         * @param rawWidth  原始宽度
         * @param rawHeight 原始高度
         * @param scale     放大倍数
         */
        public static int[] upscale(int[] argb32, int rawWidth, int rawHeight, int scale) {
            int relWidth = rawWidth * scale;
            int relHeight = rawHeight * scale;
            int[] relArgb32 = new int[relWidth * relHeight];

            for (int y = 0; y < relHeight; y++) {
                // 原始数组对应的y坐标
                int srcY = y / scale;
                for (int x = 0; x < relWidth; x++) {
                    // 原始数组对应的x坐标
                    int srcX = x / scale;
                    // 计算目标数组索引
                    int destIndex = y * relWidth + x;
                    // 计算原始数组索引
                    int srcIndex = srcY * rawWidth + srcX;
                    relArgb32[destIndex] = argb32[srcIndex];
                }
            }

            return relArgb32;
        }

        public static int[] downscale(int[] argb32, int relWidth, int relHeight, int scale, boolean average) {
            int rawWidth = relWidth / scale;
            int rawHeight = relHeight / scale;
            int[] rawArgb32 = new int[rawWidth * rawHeight];

            for (int y = 0; y < rawHeight; y++) {
                for (int x = 0; x < rawWidth; x++) {
                    int destIndex = y * rawWidth + x;

                    if (!average) {
                        // 最近邻取左上角像素
                        int srcX = x * scale;
                        int srcY = y * scale;
                        int srcIndex = srcY * relWidth + srcX;
                        rawArgb32[destIndex] = argb32[srcIndex];
                    } else {
                        // 平均采样
                        int a = 0, r = 0, g = 0, b = 0;
                        for (int dy = 0; dy < scale; dy++) {
                            for (int dx = 0; dx < scale; dx++) {
                                int srcIndex = (y * scale + dy) * relWidth + (x * scale + dx);
                                int argb = argb32[srcIndex];
                                a += (argb >>> 24) & 0xFF;
                                r += (argb >>> 16) & 0xFF;
                                g += (argb >>> 8) & 0xFF;
                                b += argb & 0xFF;
                            }
                        }
                        int area = scale * scale;
                        rawArgb32[destIndex] =
                                ((a / area) << 24) |
                                        ((r / area) << 16) |
                                        ((g / area) << 8) |
                                        (b / area);
                    }
                }
            }

            return rawArgb32;
        }

        // ARGB4444 aaaarrrr ggggbbbb -> aaaaaaaa rrrrrrrr gggggggg bbbbbbbb
        public static int fromArgb4444(short argb4444) {
            int uShort = argb4444 & 0xFFFF; // 防止符号扩展

            int a4 = (uShort >>> 12) & 0xF;
            int r4 = (uShort >>> 8) & 0xF;
            int g4 = (uShort >>> 4) & 0xF;
            int b4 = uShort & 0xF;

            int a8 = (a4 << 4) | a4;
            int r8 = (r4 << 4) | r4;
            int g8 = (g4 << 4) | g4;
            int b8 = (b4 << 4) | b4;

            return (a8 << 24) | (r8 << 16) | (g8 << 8) | b8;
        }

        public static short toArgb4444(int argb32) {
            int a = (argb32 >>> 24) & 0xFF;
            int r = (argb32 >>> 16) & 0xFF;
            int g = (argb32 >>> 8) & 0xFF;
            int b = argb32 & 0xFF;

            // +8 = +00001000 相当于十进制的四舍五入
            int a4 = (a + 8) >>> 4;
            int r4 = (r + 8) >>> 4;
            int g4 = (g + 8) >>> 4;
            int b4 = (b + 8) >>> 4;

            return (short) ((a4 << 12) | (r4 << 8) | (g4 << 4) | b4);
        }

        // ARGB1555 arrrrrgg gggbbbbb -> aaaaaaaa rrrrrrrr gggggggg bbbbbbbb
        public static int fromArgb1555(short argb1555) {
            int uShort = argb1555 & 0xFFFF;

            int a1 = (uShort >>> 15) & 0x1;
            int r5 = (uShort >>> 10) & 0x1F;
            int g5 = (uShort >>> 5) & 0x1F;
            int b5 = uShort & 0x1F;

            int a8 = a1 == 0 ? 0 : 0xFF;
            // +15 用于四舍五入
            int r8 = (r5 * 255 + 15) / 31;
            int g8 = (g5 * 255 + 15) / 31;
            int b8 = (b5 * 255 + 15) / 31;

            return (a8 << 24) | (r8 << 16) | (g8 << 8) | b8;
        }

        public static short toArgb1555(int argb32) {
            int a = (argb32 >>> 24) & 0xFF;
            int r = (argb32 >>> 16) & 0xFF;
            int g = (argb32 >>> 8) & 0xFF;
            int b = argb32 & 0xFF;

            int a1 = a >= 128 ? 1 : 0; // alpha >=128 判定为1
            // +127 用于四舍五入
            int r5 = (r * 31 + 127) / 255;
            int g5 = (g * 31 + 127) / 255;
            int b5 = (b * 31 + 127) / 255;

            return (short) ((a1 << 15) | (r5 << 10) | (g5 << 5) | b5);
        }

        // RGB565 rrrrrggg gggbbbbb -> aaaaaaaa rrrrrrrr gggggggg bbbbbbbb // alpha 默认用 0xff 即不透明
        public static int fromRgb565(short rgb565) {
            int uShort = rgb565 & 0xFFFF;

            int r5 = (uShort >>> 11) & 0x1F;
            int g6 = (uShort >>> 5) & 0x3F;
            int b5 = uShort & 0x1F;

            // +15 / +31 舍入用
            int a8 = 0xFF; // alpha = 255 不透明
            int r8 = (r5 * 255 + 15) / 31;
            int g8 = (g6 * 255 + 31) / 63;
            int b8 = (b5 * 255 + 15) / 31;

            return (a8 << 24) | (r8 << 16) | (g8 << 8) | b8;
        }

        public static short toRgb565(int argb32) {
            int r8 = (argb32 >>> 16) & 0xFF;
            int g8 = (argb32 >>> 8) & 0xFF;
            int b8 = argb32 & 0xFF;

            // +127 舍入用
            int r5 = (r8 * 31 + 127) / 255;
            int g6 = (g8 * 63 + 127) / 255;
            int b5 = (b8 * 31 + 127) / 255;

            return (short) ((r5 << 11) | (g6 << 5) | b5);
        }

        // DXT3 / DXT5
        public static int[] fromDXT3(BinaryReader reader, int width, int height) {
            byte[] alphaTable = new byte[16];
            Color[] colorTable = new Color[4];
            int[] colorIdxTable = new int[16];
            int[] argb32 = new int[width * height];

            // 以 4x4 = 16 个argb32像素点为单位
            for (int y = 0; y < height; y += 4) {
                for (int x = 0; x < width; x += 4) {
                    // 总共取 16 个 byte
                    byte[] alphaBytes = reader.getBytes(8);
                    expandAlphaTableDXT3(alphaBytes, alphaTable);
                    short color0 = reader.getShort(); // RGB565
                    short color1 = reader.getShort(); // RGB565
                    expandColorTable(color0, color1, colorTable);
                    byte[] index = reader.getBytes(4);
                    expandColorIndexTable(index, colorIdxTable);

                    // 填充 4x4 个 argb32
                    for (int j = 0; j < 4; j++) {
                        for (int i = 0; i < 4; i++) {
                            int dataIndex = j * 4 + i;
                            int uByteAlpha = alphaTable[dataIndex] & 0xFF;
                            Color color = colorTable[colorIdxTable[dataIndex]];

                            int relX = x + i;
                            int relY = y + j;
                            if (relX >= width || relY >= height) {
                                log.warn("宽高不是4的倍数 relX {} width {} relY {} height {}", relX, width, relY, height);
                                continue;
                            }
                            int argbIndex = relY * width + relX;
                            argb32[argbIndex] = uByteAlpha << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
                        }
                    }
                }
            }

            return argb32;
        }

        public static void toDXT3(BufferedImage image, BinaryWriter writer) {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            // +3 int进一舍入
            int blockWidth = (width + 3) / 4;
            int blockHeight = (height + 3) / 4;

            for (int by = 0; by < blockHeight; by++) {
                for (int bx = 0; bx < blockWidth; bx++) {
                    int[] block = new int[16];
                    // 取原图4x4个像素点存到 blockPixels
                    for (int y = 0; y < 4; y++) {
                        for (int x = 0; x < 4; x++) {
                            int px = bx * 4 + x;
                            int py = by * 4 + y;
                            int i = y * 4 + x;

                            if (px < width && py < height) {
                                block[i] = pixels[py * width + px];
                            } else {
                                block[i] = 0; // 超出边界用透明填充
                            }
                        }
                    }

                    encodeBlockDXT3(block, writer);
                }
            }
        }

        public static int[] fromDXT5(BinaryReader reader, int width, int height) {
            byte[] alphaTable = new byte[8];
            int[] alphaIdxTable = new int[16];
            Color[] colorTable = new Color[4];
            int[] colorIdxTable = new int[16];
            int[] argb32 = new int[width * height];

            // 以 4x4 = 16 个argb32像素点为单位
            for (int y = 0; y < height; y += 4) {
                for (int x = 0; x < width; x += 4) {
                    // 总共取 16 个 byte
                    byte alpha0 = reader.getByte();
                    byte alpha1 = reader.getByte();
                    expandAlphaTableDXT5(alpha0, alpha1, alphaTable);
                    byte[] alphaIndexBytes = reader.getBytes(6);
                    expandAlphaIndexTableDXT5(alphaIndexBytes, alphaIdxTable);
                    short color0 = reader.getShort(); // RGB565
                    short color1 = reader.getShort(); // RGB565
                    expandColorTable(color0, color1, colorTable);
                    byte[] index = reader.getBytes(4);
                    expandColorIndexTable(index, colorIdxTable);

                    // 填充 4x4 个 argb32
                    for (int j = 0; j < 4; j++) {
                        for (int i = 0; i < 4; i++) {
                            int dataIndex = j * 4 + i;
                            int uByteAlpha = alphaTable[alphaIdxTable[dataIndex]] & 0xFF;
                            Color color = colorTable[colorIdxTable[dataIndex]];

                            int relX = x + i;
                            int relY = y + j;
                            if (relX >= width || relY >= height) {
                                log.warn("宽高不是4的倍数 relX {} width {} relY {} height {}", relX, width, relY, height);
                                continue;
                            }
                            int argbIndex = relY * width + relX;
                            argb32[argbIndex] = uByteAlpha << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
                        }
                    }
                }
            }

            return argb32;
        }

        public static void toDXT5(BufferedImage image, BinaryWriter writer) {
            int width = image.getWidth();
            int height = image.getHeight();

            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            // +3 int进一舍入
            int blockWidth = (width + 3) / 4;
            int blockHeight = (height + 3) / 4;

            for (int by = 0; by < blockHeight; by++) {
                for (int bx = 0; bx < blockWidth; bx++) {
                    int[] block = new int[16];
                    // 取原图4x4个像素点存到 blockPixels
                    for (int y = 0; y < 4; y++) {
                        for (int x = 0; x < 4; x++) {
                            int px = bx * 4 + x;
                            int py = by * 4 + y;
                            int i = y * 4 + x;

                            if (px < width && py < height) {
                                block[i] = pixels[py * width + px];
                            } else {
                                block[i] = 0; // 超出边界用透明填充
                            }
                        }
                    }

                    encodeBlockDXT5(block, writer);
                }
            }
        }

        private static void expandAlphaTableDXT3(byte[] alpha, byte[] alphaTable) {
            int uByte;
            for (int i = 0, j = 0; i < 16; i += 2, j++) {
                uByte = alpha[j] & 0xFF;
                alphaTable[i] = (byte) (uByte & 0x0F);
                alphaTable[i + 1] = (byte) ((uByte & 0xF0) >>> 4);
            }
            for (int i = 0; i < 16; i++) {
                uByte = alphaTable[i] & 0xFF;
                alphaTable[i] = (byte) ((uByte & 0xFF) | ((uByte & 0xFF) << 4));
            }
        }

        private static void expandAlphaTableDXT5(byte alpha0, byte alpha1, byte[] alphaTable) {
            alphaTable[0] = alpha0;
            alphaTable[1] = alpha1;
            int uByteAlpha0 = alpha0 & 0xFF;
            int uByteAlpha1 = alpha1 & 0xFF;

            if (uByteAlpha0 > uByteAlpha1) {
                for (int i = 2; i < 8; i++) {
                    alphaTable[i] = (byte) (((8 - i) * uByteAlpha0 + (i - 1) * uByteAlpha1 + 3) / 7);
                }
            } else {
                for (int i = 2; i < 6; i++) {
                    alphaTable[i] = (byte) (((6 - i) * uByteAlpha0 + (i - 1) * uByteAlpha1 + 2) / 5);
                }
                alphaTable[6] = 0;
                alphaTable[7] = (byte) 255;
            }
        }

        private static void expandAlphaIndexTableDXT5(byte[] alphaIndexBytes, int[] alphaIndexTable) {
            for (int i = 0, i2 = 0; i < 16; i += 8, i2 += 3) {
                int flags = (alphaIndexBytes[i2] & 0xFF)
                        | ((alphaIndexBytes[i2 + 1] & 0xFF) << 8)
                        | ((alphaIndexBytes[i2 + 2] & 0xFF) << 16);
                for (int j = 0; j < 8; j++) {
                    int mask = 0x07 << (3 * j); // 0x07 = 0b111
                    alphaIndexTable[i + j] = (flags & mask) >>> (3 * j);
                }
            }
        }

        private static void expandColorTable(short color0, short color1, Color[] colorTable) {
            colorTable[0] = new Color(fromRgb565(color0));
            colorTable[1] = new Color(fromRgb565(color1));

            // 转成 uShort 再比较大小
            if ((color0 & 0xFFFF) > (color1 & 0xFFFF)) {
                colorTable[2] = new Color(
                        (colorTable[0].getRed() * 2 + colorTable[1].getRed() + 1) / 3,
                        (colorTable[0].getGreen() * 2 + colorTable[1].getGreen() + 1) / 3,
                        (colorTable[0].getBlue() * 2 + colorTable[1].getBlue() + 1) / 3
                );
                colorTable[3] = new Color(
                        (colorTable[0].getRed() + colorTable[1].getRed() * 2 + 1) / 3,
                        (colorTable[0].getGreen() + colorTable[1].getGreen() * 2 + 1) / 3,
                        (colorTable[0].getBlue() + colorTable[1].getBlue() * 2 + 1) / 3
                );
            } else {
                colorTable[2] = new Color(
                        (colorTable[0].getRed() + colorTable[1].getRed()) / 2,
                        (colorTable[0].getGreen() + colorTable[1].getGreen()) / 2,
                        (colorTable[0].getBlue() + colorTable[1].getBlue()) / 2
                );
                colorTable[3] = Color.BLACK;
            }
        }

        private static void expandColorIndexTable(byte[] index, int[] colorIndex) {
            int uByte;
            for (int i = 0, j = 0; i < 16; i += 4, j++) {
                uByte = index[j] & 0xFF;
                colorIndex[i] = uByte & 0x03;
                colorIndex[i + 1] = (uByte >>> 2) & 0x03;
                colorIndex[i + 2] = (uByte >>> 4) & 0x03;
                colorIndex[i + 3] = (uByte >>> 6) & 0x03;
            }
        }

        private static void encodeBlockDXT3(int[] pixels, BinaryWriter writer) {
            // 编码 alphaTable 对标 expandAlphaTableDXT3
            for (int i = 0; i < 16; i += 2) {
                // 取高4位 (alpha高4位)
                int a0 = (pixels[i] >>> 28) & 0xF;
                int a1 = (pixels[i + 1] >>> 28) & 0xF;
                writer.putByte((byte) ((a1 << 4) | a0));
            }

            // 写颜色（DXT1 算法，8字节）
            encodeBlockDXT1Colors(pixels, writer);
        }

        private static void encodeBlockDXT5(int[] pixels, BinaryWriter writer) {
            encodeAlphaDXT5(pixels, writer);     // 8 bytes alpha
            encodeBlockDXT1Colors(pixels, writer); // 8 bytes（和 DXT3 完全一致）
        }

        private static void encodeAlphaDXT5(int[] pixels, BinaryWriter writer) {
            // 生成 alpha0/alpha1
            int aMin = 255;
            int aMax = 0;

            for (int px : pixels) {
                int a = (px >>> 24) & 0xFF;
                if (a < aMin) aMin = a;
                if (a > aMax) aMax = a;
            }

            byte alpha0 = (byte) aMax;
            byte alpha1 = (byte) aMin;

            writer.putByte(alpha0);
            writer.putByte(alpha1);

            // 生成 8 个 alpha 插值表
            byte[] alphaTable = new byte[8];
            expandAlphaTableDXT5(alpha0, alpha1, alphaTable);

            // 生成 16 × 3bit 索引（48 bit/6 byte）
            long alphaBits = 0;
            int bitPos = 0;

            for (int i = 0; i < 16; i++) {
                int a = (pixels[i] >>> 24) & 0xFF;

                int best = 0;
                int bestDiff = Integer.MAX_VALUE;

                for (int j = 0; j < 8; j++) {
                    int diff = Math.abs(a - (alphaTable[j] & 0xFF));
                    if (diff < bestDiff) {
                        bestDiff = diff;
                        best = j;
                    }
                }

                alphaBits |= ((long) best) << bitPos;
                bitPos += 3;
            }

            // 写入 6 字节
            for (int i = 0; i < 6; i++) {
                writer.putByte((byte) ((alphaBits >> (8 * i)) & 0xFF));
            }
        }

        private static void encodeBlockDXT1Colors(int[] pixels, BinaryWriter writer) {
            int rMin = 255, gMin = 255, bMin = 255;
            int rMax = 0, gMax = 0, bMax = 0;

            for (int px : pixels) {
                int r = (px >> 16) & 0xFF;
                int g = (px >> 8) & 0xFF;
                int b = px & 0xFF;

                if (r < rMin) rMin = r;
                if (g < gMin) gMin = g;
                if (b < bMin) bMin = b;
                if (r > rMax) rMax = r;
                if (g > gMax) gMax = g;
                if (b > bMax) bMax = b;
            }

            // 转为 RGB565
            short color0 = toRgb565(rMax << 16 | gMax << 8 | bMax);
            short color1 = toRgb565(rMin << 16 | gMin << 8 | bMin);
            writer.putShort(color0);
            writer.putShort(color1);

            // 生成4x4索引
            Color[] colorTable = new Color[4];
            expandColorTable(color0, color1, colorTable);

            int bitIndex = 0;
            int bits = 0; // 实际只用到 1 byte = 8 bit = 4 组 2bit 索引
            for (int i = 0; i < 16; i++) { // 依次处理16个像素点
                int px = pixels[i];
                int r = (px >> 16) & 0xFF;
                int g = (px >> 8) & 0xFF;
                int b = px & 0xFF;

                int best = 0;
                int minDist = Integer.MAX_VALUE;
                for (int j = 0; j < 4; j++) { // 遍历 colorTable 查找最接近的颜色
                    Color c = colorTable[j];
                    int dr = r - c.getRed();
                    int dg = g - c.getGreen();
                    int db = b - c.getBlue();
                    int dist = dr * dr + dg * dg + db * db; // 计算 L2 平方损失来判断误差大小
                    if (dist < minDist) { // 找出误差最小的
                        minDist = dist;
                        best = j;
                    }
                }

                bits |= best << bitIndex;
                bitIndex += 2;
                if (bitIndex >= 8) {
                    writer.putByte((byte) (bits & 0xFF));
                    bits >>= 8;
                    bitIndex -= 8;
                }
            }
            if (bitIndex > 0) {
                writer.putByte((byte) (bits & 0xFF)); // 写剩余位, 正常情况下应该执行不到这一步
            }
        }

        //
    }

    public static int getRawByteSize(WzPngFormat format, int scale, int width, int height) {
        if (scale > 1) {
            if (width % scale != 0 || height % scale != 0) {
                throw new IllegalArgumentException("width 和 height 不能被 scale 整除");
            }
            width /= scale;
            height /= scale;
        }

        return getRawByteSize(format, width, height);
    }

    private static int getRawByteSize(WzPngFormat format, int width, int height) {
        int size = width * height * 4;
        return switch (format) {
            case WzPngFormat.ARGB4444, WzPngFormat.ARGB1555, WzPngFormat.RGB565 -> size / 2; // int 压缩成 short 大小减半
            case WzPngFormat.ARGB8888 -> size; // 原始数据
            case WzPngFormat.DXT3, WzPngFormat.DXT5 -> size / 4; // 特殊压缩，大小为原来的1/4
        };
    }

    public static int getBufferImageType(WzPngFormat format) {
        return switch (format) {
            case WzPngFormat.ARGB4444, ARGB8888, ARGB1555, DXT3, DXT5 -> BufferedImage.TYPE_INT_ARGB;
            case RGB565 -> BufferedImage.TYPE_USHORT_565_RGB;
        };
    }
}
