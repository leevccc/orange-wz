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

            // + 127 用于舍入
            int r4 = (r * 15 + 127) / 255;
            int g4 = (g * 15 + 127) / 255;
            int b4 = (b * 15 + 127) / 255;
            int a4 = (a * 15 + 127) / 255;

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
                writer.putByte((byte) ((alphaBits >>> (8 * i)) & 0xFF));
            }
        }

        private static void encodeBlockDXT1Colors(int[] pixels, BinaryWriter writer) {
            int rMin = 255, gMin = 255, bMin = 255;
            int rMax = 0, gMax = 0, bMax = 0;

            for (int px : pixels) {
                int r = (px >>> 16) & 0xFF;
                int g = (px >>> 8) & 0xFF;
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
                int r = (px >>> 16) & 0xFF;
                int g = (px >>> 8) & 0xFF;
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
                    bits >>>= 8;
                    bitIndex -= 8;
                }
            }
            if (bitIndex > 0) {
                writer.putByte((byte) (bits & 0xFF)); // 写剩余位, 正常情况下应该执行不到这一步
            }
        }

        // BC7 https://github.com/richgel999/bc7enc_rdo/blob/master/bc7decomp.cpp
        public static int[] fromBC7(BinaryReader reader, int width, int height) {
            int[] argb32 = new int[width * height];
            ColorRGBA[] pixels = new ColorRGBA[16];
            byte[] block;

            for (int y = 0; y < height; y += 4) {
                for (int x = 0; x < width; x += 4) {
                    // 2 个 long 转成 16 个 int argb
                    block = reader.getBytes(16);
                    decodeBC7(block, pixels);
                    for (int i = 0; i < 16; i++) {
                        int row = i / 4;
                        int col = i % 4;
                        int index = (y + row) * width + x + col;
                        argb32[index] = pixels[i].getARGB();
                    }
                }
            }

            return argb32;
        }

        private static final class ColorRGBA {
            public int red;
            public int green;
            public int blue;
            public int alpha;

            public void set(int index, long val) {
                val &= 0xFF;
                switch (index) {
                    case 0:
                        red = (int) val;
                        break;
                    case 1:
                        green = (int) val;
                        break;
                    case 2:
                        blue = (int) val;
                        break;
                    case 3:
                        alpha = (int) val;
                        break;
                }
            }

            public int get(int index) {
                return switch (index) {
                    case 0 -> red;
                    case 1 -> green;
                    case 2 -> blue;
                    case 3 -> alpha;
                    default -> throw new IndexOutOfBoundsException();
                };
            }

            public int getARGB() {
                return (alpha << 24) | (red << 16) | (green << 8) | blue;
            }

            public ColorRGBA copy() {
                ColorRGBA color = new ColorRGBA();
                color.red = red;
                color.green = green;
                color.blue = blue;
                color.alpha = alpha;
                return color;
            }
        }

        private static final byte[] gBC7FirstByteToMode = { // 256
                8, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                7, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
                4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        };

        private static final int[] gBC7Weights2 = {0, 21, 43, 64};
        private static final int[] gBC7Weights3 = {0, 9, 18, 27, 37, 46, 55, 64};
        private static final int[] gBC7Weights4 = {0, 4, 9, 13, 17, 21, 26, 30, 34, 38, 43, 47, 51, 55, 60, 64};

        // @formatter:off
        private static final byte[] gBC7TableAnchorIndexSecondSubset = { // 64
                15,15,15,15,15,15,15,15,                15,15,15,15,15,15,15,15,                15, 2, 8, 2, 2, 8, 8,15,                2, 8, 2, 2, 8, 8, 2, 2,                 15,15, 6, 8, 2, 8,15,15,                2, 8, 2, 2, 2,15,15, 6,                 6, 2, 6, 8,15,15, 2, 2,                 15,15,15,15,15, 2, 2,15
        };

        private static final byte[] gBC7TableAnchorIndexThirdSubset_1 = { // 64
                3, 3,15,15, 8, 3,15,15,                 8, 8, 6, 6, 6, 5, 3, 3,                 3, 3, 8,15, 3, 3, 6,10,                 5, 8, 8, 6, 8, 5,15,15,                 8,15, 3, 5, 6,10, 8,15,                 15, 3,15, 5,15,15,15,15,                3,15, 5, 5, 5, 8, 5,10,                 5,10, 8,13,15,12, 3, 3
        };

        private static final byte[] gBC7TableAnchorIndexThirdSubset_2 = { // 64
                15, 8, 8, 3,15,15, 3, 8,                15,15,15,15,15,15,15, 8,                15, 8,15, 3,15, 8,15, 8,                3,15, 6,10,15,15,10, 8,                 15, 3,15,10,10, 8, 9,10,                6,15, 8,15, 3, 6, 6, 8,                 15, 3,15,15,15,15,15,15,                15,15,15,15, 3,15,15, 8
        };
        
        private static final byte[] gBC7Partition2 = { //64 x 16
                0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,1,        0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,1,        0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,        0,0,0,1,0,0,1,1,0,0,1,1,0,1,1,1,        0,0,0,0,0,0,0,1,0,0,0,1,0,0,1,1,        0,0,1,1,0,1,1,1,0,1,1,1,1,1,1,1,        0,0,0,1,0,0,1,1,0,1,1,1,1,1,1,1,        0,0,0,0,0,0,0,1,0,0,1,1,0,1,1,1,
                0,0,0,0,0,0,0,0,0,0,0,1,0,0,1,1,        0,0,1,1,0,1,1,1,1,1,1,1,1,1,1,1,        0,0,0,0,0,0,0,1,0,1,1,1,1,1,1,1,        0,0,0,0,0,0,0,0,0,0,0,1,0,1,1,1,        0,0,0,1,0,1,1,1,1,1,1,1,1,1,1,1,        0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,        0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,        0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,
                0,0,0,0,1,0,0,0,1,1,1,0,1,1,1,1,        0,1,1,1,0,0,0,1,0,0,0,0,0,0,0,0,        0,0,0,0,0,0,0,0,1,0,0,0,1,1,1,0,        0,1,1,1,0,0,1,1,0,0,0,1,0,0,0,0,        0,0,1,1,0,0,0,1,0,0,0,0,0,0,0,0,        0,0,0,0,1,0,0,0,1,1,0,0,1,1,1,0,        0,0,0,0,0,0,0,0,1,0,0,0,1,1,0,0,        0,1,1,1,0,0,1,1,0,0,1,1,0,0,0,1,
                0,0,1,1,0,0,0,1,0,0,0,1,0,0,0,0,        0,0,0,0,1,0,0,0,1,0,0,0,1,1,0,0,        0,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,        0,0,1,1,0,1,1,0,0,1,1,0,1,1,0,0,        0,0,0,1,0,1,1,1,1,1,1,0,1,0,0,0,        0,0,0,0,1,1,1,1,1,1,1,1,0,0,0,0,        0,1,1,1,0,0,0,1,1,0,0,0,1,1,1,0,        0,0,1,1,1,0,0,1,1,0,0,1,1,1,0,0,
                0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,        0,0,0,0,1,1,1,1,0,0,0,0,1,1,1,1,        0,1,0,1,1,0,1,0,0,1,0,1,1,0,1,0,        0,0,1,1,0,0,1,1,1,1,0,0,1,1,0,0,        0,0,1,1,1,1,0,0,0,0,1,1,1,1,0,0,        0,1,0,1,0,1,0,1,1,0,1,0,1,0,1,0,        0,1,1,0,1,0,0,1,0,1,1,0,1,0,0,1,        0,1,0,1,1,0,1,0,1,0,1,0,0,1,0,1,
                0,1,1,1,0,0,1,1,1,1,0,0,1,1,1,0,        0,0,0,1,0,0,1,1,1,1,0,0,1,0,0,0,        0,0,1,1,0,0,1,0,0,1,0,0,1,1,0,0,        0,0,1,1,1,0,1,1,1,1,0,1,1,1,0,0,        0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,        0,0,1,1,1,1,0,0,1,1,0,0,0,0,1,1,        0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,        0,0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,
                0,1,0,0,1,1,1,0,0,1,0,0,0,0,0,0,        0,0,1,0,0,1,1,1,0,0,1,0,0,0,0,0,        0,0,0,0,0,0,1,0,0,1,1,1,0,0,1,0,        0,0,0,0,0,1,0,0,1,1,1,0,0,1,0,0,        0,1,1,0,1,1,0,0,1,0,0,1,0,0,1,1,        0,0,1,1,0,1,1,0,1,1,0,0,1,0,0,1,        0,1,1,0,0,0,1,1,1,0,0,1,1,1,0,0,        0,0,1,1,1,0,0,1,1,1,0,0,0,1,1,0,
                0,1,1,0,1,1,0,0,1,1,0,0,1,0,0,1,        0,1,1,0,0,0,1,1,0,0,1,1,1,0,0,1,        0,1,1,1,1,1,1,0,1,0,0,0,0,0,0,1,        0,0,0,1,1,0,0,0,1,1,1,0,0,1,1,1,        0,0,0,0,1,1,1,1,0,0,1,1,0,0,1,1,        0,0,1,1,0,0,1,1,1,1,1,1,0,0,0,0,        0,0,1,0,0,0,1,0,1,1,1,0,1,1,1,0,        0,1,0,0,0,1,0,0,0,1,1,1,0,1,1,1
        };

        private static final byte[] gBC7Partition3 = { //64 x 16
                0,0,1,1,0,0,1,1,0,2,2,1,2,2,2,2,        0,0,0,1,0,0,1,1,2,2,1,1,2,2,2,1,        0,0,0,0,2,0,0,1,2,2,1,1,2,2,1,1,        0,2,2,2,0,0,2,2,0,0,1,1,0,1,1,1,        0,0,0,0,0,0,0,0,1,1,2,2,1,1,2,2,        0,0,1,1,0,0,1,1,0,0,2,2,0,0,2,2,        0,0,2,2,0,0,2,2,1,1,1,1,1,1,1,1,        0,0,1,1,0,0,1,1,2,2,1,1,2,2,1,1,
                0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,        0,0,0,0,1,1,1,1,1,1,1,1,2,2,2,2,        0,0,0,0,1,1,1,1,2,2,2,2,2,2,2,2,        0,0,1,2,0,0,1,2,0,0,1,2,0,0,1,2,        0,1,1,2,0,1,1,2,0,1,1,2,0,1,1,2,        0,1,2,2,0,1,2,2,0,1,2,2,0,1,2,2,        0,0,1,1,0,1,1,2,1,1,2,2,1,2,2,2,        0,0,1,1,2,0,0,1,2,2,0,0,2,2,2,0,
                0,0,0,1,0,0,1,1,0,1,1,2,1,1,2,2,        0,1,1,1,0,0,1,1,2,0,0,1,2,2,0,0,        0,0,0,0,1,1,2,2,1,1,2,2,1,1,2,2,        0,0,2,2,0,0,2,2,0,0,2,2,1,1,1,1,        0,1,1,1,0,1,1,1,0,2,2,2,0,2,2,2,        0,0,0,1,0,0,0,1,2,2,2,1,2,2,2,1,        0,0,0,0,0,0,1,1,0,1,2,2,0,1,2,2,        0,0,0,0,1,1,0,0,2,2,1,0,2,2,1,0,
                0,1,2,2,0,1,2,2,0,0,1,1,0,0,0,0,        0,0,1,2,0,0,1,2,1,1,2,2,2,2,2,2,        0,1,1,0,1,2,2,1,1,2,2,1,0,1,1,0,        0,0,0,0,0,1,1,0,1,2,2,1,1,2,2,1,        0,0,2,2,1,1,0,2,1,1,0,2,0,0,2,2,        0,1,1,0,0,1,1,0,2,0,0,2,2,2,2,2,        0,0,1,1,0,1,2,2,0,1,2,2,0,0,1,1,        0,0,0,0,2,0,0,0,2,2,1,1,2,2,2,1,
                0,0,0,0,0,0,0,2,1,1,2,2,1,2,2,2,        0,2,2,2,0,0,2,2,0,0,1,2,0,0,1,1,        0,0,1,1,0,0,1,2,0,0,2,2,0,2,2,2,        0,1,2,0,0,1,2,0,0,1,2,0,0,1,2,0,        0,0,0,0,1,1,1,1,2,2,2,2,0,0,0,0,        0,1,2,0,1,2,0,1,2,0,1,2,0,1,2,0,        0,1,2,0,2,0,1,2,1,2,0,1,0,1,2,0,        0,0,1,1,2,2,0,0,1,1,2,2,0,0,1,1,
                0,0,1,1,1,1,2,2,2,2,0,0,0,0,1,1,        0,1,0,1,0,1,0,1,2,2,2,2,2,2,2,2,        0,0,0,0,0,0,0,0,2,1,2,1,2,1,2,1,        0,0,2,2,1,1,2,2,0,0,2,2,1,1,2,2,        0,0,2,2,0,0,1,1,0,0,2,2,0,0,1,1,        0,2,2,0,1,2,2,1,0,2,2,0,1,2,2,1,        0,1,0,1,2,2,2,2,2,2,2,2,0,1,0,1,        0,0,0,0,2,1,2,1,2,1,2,1,2,1,2,1,
                0,1,0,1,0,1,0,1,0,1,0,1,2,2,2,2,        0,2,2,2,0,1,1,1,0,2,2,2,0,1,1,1,        0,0,0,2,1,1,1,2,0,0,0,2,1,1,1,2,        0,0,0,0,2,1,1,2,2,1,1,2,2,1,1,2,        0,2,2,2,0,1,1,1,0,1,1,1,0,2,2,2,        0,0,0,2,1,1,1,2,1,1,1,2,0,0,0,2,        0,1,1,0,0,1,1,0,0,1,1,0,2,2,2,2,        0,0,0,0,0,0,0,0,2,1,1,2,2,1,1,2,
                0,1,1,0,0,1,1,0,2,2,2,2,2,2,2,2,        0,0,2,2,0,0,1,1,0,0,1,1,0,0,2,2,        0,0,2,2,1,1,2,2,1,1,2,2,0,0,2,2,        0,0,0,0,0,0,0,0,0,0,0,0,2,1,1,2,        0,0,0,2,0,0,0,1,0,0,0,2,0,0,0,1,        0,2,2,2,1,2,2,2,0,2,2,2,1,2,2,2,        0,1,0,1,2,2,2,2,2,2,2,2,2,2,2,2,        0,1,1,1,2,0,1,1,2,2,0,1,2,2,2,0,
        };
        // @formatter:on

        private static void decodeBC7(byte[] blockBytes, ColorRGBA[] pixels) {
            long[] dataChunks = new long[2];
            BinaryReader reader = new BinaryReader(blockBytes);
            dataChunks[0] = reader.getLong();
            dataChunks[1] = reader.getLong();

            int mode = gBC7FirstByteToMode[blockBytes[0] & 0xFF];
            switch (mode) {
                case 0:
                case 2:
                    unpackBC7Mode0_2(mode, dataChunks, pixels);
                    break;
                case 1:
                case 3:
                case 7:
                    unpackBC7Mode1_3_7(mode, dataChunks, pixels);
                    break;
                case 4:
                case 5:
                    unpackBC7Mode4_5(mode, dataChunks, pixels);
                    break;
                case 6:
                    unpackBC7Mode6(dataChunks, pixels);
                    break;
            }
        }

        private static void unpackBC7Mode0_2(int mode, long[] dataChunks, ColorRGBA[] pixels) {
            final int ENDPOINTS = 6;
            final int WEIGHT_BITS = (mode == 0) ? 3 : 2;
            final int WEIGHT_MASK = (1 << WEIGHT_BITS) - 1;
            final int ENDPOINT_BITS = (mode == 0) ? 4 : 5;
            final int ENDPOINT_MASK = (1 << ENDPOINT_BITS) - 1;
            final int PBITS = (mode == 0) ? 6 : 0;
            final int WEIGHT_VALS = 1 << WEIGHT_BITS;
            final int PART_BITS = (mode == 0) ? 4 : 6;
            final int PART_MASK = (1 << PART_BITS) - 1;

            final long chunkLow = dataChunks[0];
            final long chunkHigh = dataChunks[1];

            final int part = (int) (chunkLow >>> (mode + 1)) & PART_MASK;

            long[] channelReaChunks = new long[3];

            if (mode == 0) {
                channelReaChunks[0] = chunkLow >>> 5;
                channelReaChunks[1] = chunkHigh >>> 29;
                channelReaChunks[2] = ((chunkLow >>> 53) | (chunkHigh << 11));
            } else {
                channelReaChunks[0] = chunkLow >>> 9;
                channelReaChunks[1] = ((chunkLow >>> 39) | (chunkHigh << 25));
                channelReaChunks[2] = chunkHigh >>> 5;
            }

            ColorRGBA[] endpoints = new ColorRGBA[ENDPOINTS];
            for (int e = 0; e < ENDPOINTS; e++) {
                endpoints[e] = new ColorRGBA();
            }
            for (int c = 0; c < 3; c++) {
                long channelReadChunk = channelReaChunks[c];
                for (int e = 0; e < ENDPOINTS; e++) {
                    endpoints[e].set(c, channelReadChunk & ENDPOINT_MASK);
                    channelReadChunk >>>= ENDPOINT_BITS;
                }
            }

            int[] bits = new int[6];
            if (mode == 0) {
                byte bitsChunk = (byte) ((chunkHigh >>> 13) & 0xFF);
                for (int p = 0; p < PBITS; p++) {
                    bits[p] = (bitsChunk >>> p) & 1;
                }
            }

            long weightsReadChunk = chunkHigh >>> (67 - 16 * WEIGHT_BITS);
            weightsReadChunk = insertWeightZero(weightsReadChunk, WEIGHT_BITS, 0);
            weightsReadChunk = insertWeightZero(weightsReadChunk, WEIGHT_BITS, Math.min(gBC7TableAnchorIndexThirdSubset_1[part], gBC7TableAnchorIndexThirdSubset_2[part]));
            weightsReadChunk = insertWeightZero(weightsReadChunk, WEIGHT_BITS, Math.max(gBC7TableAnchorIndexThirdSubset_1[part], gBC7TableAnchorIndexThirdSubset_2[part]));

            int[] weights = new int[16];
            for (int i = 0; i < 16; i++) {
                weights[i] = (int) weightsReadChunk & WEIGHT_MASK;
                weightsReadChunk >>>= WEIGHT_BITS;
            }

            for (int e = 0; e < ENDPOINTS; e++) {
                for (int c = 0; c < 4; c++) {
                    endpoints[e].set(c, c == 3 ? 255 : (PBITS != 0 ? bc7DeQuant(endpoints[e].get(c), bits[e], ENDPOINT_BITS) : bc7DeQuant(endpoints[e].get(c), ENDPOINT_BITS)));
                }
            }

            ColorRGBA[][] blockColors = new ColorRGBA[3][8];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 8; j++) {
                    blockColors[i][j] = new ColorRGBA();
                }
            }
            for (int s = 0; s < 3; s++) {
                for (int i = 0; i < WEIGHT_VALS; i++) {
                    for (int c = 0; c < 3; c++) {
                        blockColors[s][i].set(c, bc7Interp(endpoints[s * 2 + 0].get(c), endpoints[s * 2 + 1].get(c), i, WEIGHT_BITS));
                    }
                    blockColors[s][i].set(3, 255);
                }
            }

            for (int i = 0; i < 16; i++) {
                pixels[i] = blockColors[gBC7Partition3[part * 16 + i]][weights[i]].copy();
            }
        }

        private static void unpackBC7Mode1_3_7(int mode, long[] dataChunks, ColorRGBA[] pixels) {
            final int ENDPOINTS = 4;
            final int COMPS = (mode == 7) ? 4 : 3;
            final int WEIGHT_BITS = (mode == 1) ? 3 : 2;
            final int WEIGHT_MASK = (1 << WEIGHT_BITS) - 1;
            final int ENDPOINT_BITS = (mode == 7) ? 5 : ((mode == 1) ? 6 : 7);
            final int ENDPOINT_MASK = (1 << ENDPOINT_BITS) - 1;
            final int PBITS = (mode == 1) ? 2 : 4;
            final boolean SHARED_PBITS = mode == 1;
            final int WEIGHT_VALS = 1 << WEIGHT_BITS;

            final long low_chunk = dataChunks[0];
            final long high_chunk = dataChunks[1];

            final int part = (int) ((low_chunk >>> (mode + 1)) & 0x3F);

            ColorRGBA[] endpoints = new ColorRGBA[ENDPOINTS];
            long[] channelReadChunks = new long[4];
            long readChunk = 0;
            channelReadChunks[0] = (low_chunk >>> (mode + 7));
            long weightsReadChunk = 0;

            switch (mode) {
                case 1:
                    channelReadChunks[1] = low_chunk >>> 32;
                    channelReadChunks[2] = (low_chunk >>> 56) | (high_chunk << 8);
                    readChunk = high_chunk >>> 16;
                    weightsReadChunk = high_chunk >>> 18;
                    break;
                case 3:
                    channelReadChunks[1] = (low_chunk >>> 38) | (high_chunk << 26);
                    channelReadChunks[2] = high_chunk >>> 2;
                    readChunk = high_chunk >>> 30;
                    weightsReadChunk = high_chunk >>> 34;
                    break;
                case 7:
                    channelReadChunks[1] = low_chunk >>> 34;
                    channelReadChunks[2] = (low_chunk >>> 54) | (high_chunk << 10);
                    channelReadChunks[3] = high_chunk >>> 10;
                    readChunk = high_chunk >>> 30;
                    weightsReadChunk = high_chunk >>> 34;
                    break;
            }

            for (int e = 0; e < ENDPOINTS; e++) {
                endpoints[e] = new ColorRGBA();
            }
            for (int c = 0; c < COMPS; c++) {
                long channelReadChunk = channelReadChunks[c];
                for (int e = 0; e < ENDPOINTS; e++) {
                    endpoints[e].set(c, channelReadChunk & ENDPOINT_MASK);
                    channelReadChunk >>>= ENDPOINT_BITS;
                }
            }

            int[] bits = new int[4];
            for (int p = 0; p < PBITS; p++) {
                bits[p] = (int) ((readChunk >>> p) & 1);
            }

            weightsReadChunk = insertWeightZero(weightsReadChunk, WEIGHT_BITS, 0);
            weightsReadChunk = insertWeightZero(weightsReadChunk, WEIGHT_BITS, gBC7TableAnchorIndexSecondSubset[part]);

            int[] weights = new int[16];
            for (int i = 0; i < 16; i++) {
                weights[i] = (int) (weightsReadChunk & WEIGHT_MASK);
                weightsReadChunk >>>= WEIGHT_BITS;
            }

            for (int e = 0; e < ENDPOINTS; e++) {
                for (int c = 0; c < 4; c++) {
                    endpoints[e].set(c, (mode != 7 && c == 3) ? 255 : bc7DeQuant(endpoints[e].get(c), bits[SHARED_PBITS ? (e >>> 1) : e], ENDPOINT_BITS));
                }
            }

            ColorRGBA[][] blockColors = new ColorRGBA[2][8];
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 8; j++) {
                    blockColors[i][j] = new ColorRGBA();
                }
            }
            for (int s = 0; s < 2; s++) {
                for (int i = 0; i < WEIGHT_VALS; i++) {
                    for (int c = 0; c < COMPS; c++) {
                        blockColors[s][i].set(c, bc7Interp(endpoints[s * 2 + 0].get(c), endpoints[s * 2 + 1].get(c), i, WEIGHT_BITS));
                    }
                    if (COMPS == 3) {
                        blockColors[s][i].set(3, 255);
                    }
                }
            }

            for (int i = 0; i < 16; i++) {
                pixels[i] = blockColors[gBC7Partition2[part * 16 + i]][weights[i]].copy();
            }
        }

        private static void unpackBC7Mode4_5(int mode, long[] dataChunks, ColorRGBA[] pixels) {
            final int ENDPOINTS = 2;
            final int WEIGHT_BITS = 2;
            final int WEIGHT_MASK = (1 << WEIGHT_BITS) - 1;
            final int A_WEIGHT_BITS = (mode == 4) ? 3 : 2;
            final int A_WEIGHT_MASK = (1 << A_WEIGHT_BITS) - 1;
            final int ENDPOINT_BITS = (mode == 4) ? 5 : 7;
            final int ENDPOINT_MASK = (1 << ENDPOINT_BITS) - 1;
            final int A_ENDPOINT_BITS = (mode == 4) ? 6 : 8;
            final int A_ENDPOINT_MASK = (1 << A_ENDPOINT_BITS) - 1;

            final long low_chunk = dataChunks[0];
            final long high_chunk = dataChunks[1];

            final int comp_rot = (int) ((low_chunk >>> (mode + 1)) & 0x3);
            final int index_mode = (mode == 4) ? (int) ((low_chunk >>> 7) & 1) : 0;
            final boolean indexModeBool = index_mode != 0;

            long colorReadBits = low_chunk >>> 8;

            ColorRGBA[] endpoints = new ColorRGBA[ENDPOINTS];
            for (int e = 0; e < ENDPOINTS; e++) {
                endpoints[e] = new ColorRGBA();
            }
            for (int c = 0; c < 3; c++) {
                for (int e = 0; e < ENDPOINTS; e++) {
                    endpoints[e].set(c, colorReadBits & ENDPOINT_MASK);
                    colorReadBits >>>= ENDPOINT_BITS;
                }
            }

            endpoints[0].set(3, colorReadBits & ENDPOINT_MASK);

            long rgbWeightChunk = 0;
            long aWeightChunk = 0;
            if (mode == 4) {
                endpoints[0].set(3, colorReadBits & A_ENDPOINT_MASK);
                endpoints[1].set(3, (colorReadBits >>> A_ENDPOINT_BITS) & A_ENDPOINT_MASK);
                rgbWeightChunk = (low_chunk >>> 50) | (high_chunk << 14);
                aWeightChunk = high_chunk >>> 17;
            } else if (mode == 5) {
                endpoints[0].set(3, colorReadBits & A_ENDPOINT_MASK);
                endpoints[1].set(3, ((low_chunk >>> 58) | (high_chunk << 6)) & A_ENDPOINT_MASK);
                rgbWeightChunk = high_chunk >>> 2;
                aWeightChunk = high_chunk >>> 33;
            }

            rgbWeightChunk = insertWeightZero(rgbWeightChunk, WEIGHT_BITS, 0);
            aWeightChunk = insertWeightZero(aWeightChunk, A_WEIGHT_BITS, 0);

            final int[] weight_bits = {
                    indexModeBool ? A_WEIGHT_BITS : WEIGHT_BITS,
                    indexModeBool ? WEIGHT_BITS : A_WEIGHT_BITS
            };
            final int[] weight_mask = {
                    indexModeBool ? A_WEIGHT_MASK : WEIGHT_MASK,
                    indexModeBool ? WEIGHT_MASK : A_WEIGHT_MASK
            };

            int[] weights = new int[16];
            int[] aWeights = new int[16];

            if (indexModeBool) {
                long temp = rgbWeightChunk;
                rgbWeightChunk = aWeightChunk;
                aWeightChunk = temp;
            }

            for (int i = 0; i < 16; i++) {
                weights[i] = (int) (rgbWeightChunk & weight_mask[0]);
                rgbWeightChunk >>>= weight_bits[0];
            }

            for (int i = 0; i < 16; i++) {
                aWeights[i] = (int) (aWeightChunk & weight_mask[1]);
                aWeightChunk >>>= weight_bits[1];
            }

            for (int e = 0; e < ENDPOINTS; e++) {
                for (int c = 0; c < 4; c++) {
                    endpoints[e].set(c, bc7DeQuant(endpoints[e].get(c), (c == 3) ? A_ENDPOINT_BITS : ENDPOINT_BITS));
                }
            }

            ColorRGBA[] blockColors = new ColorRGBA[8];
            for (int i = 0; i < 8; i++) {
                blockColors[i] = new ColorRGBA();
            }
            for (int i = 0; i < 1 << weight_bits[0]; i++) {
                for (int c = 0; c < 3; c++) {
                    blockColors[i].set(c, bc7Interp(endpoints[0].get(c), endpoints[1].get(c), i, weight_bits[0]));
                }
            }

            for (int i = 0; i < 1 << weight_bits[1]; i++) {
                blockColors[i].set(3, bc7Interp(endpoints[0].get(3), endpoints[1].get(3), i, weight_bits[1]));
            }

            for (int i = 0; i < 16; i++) {
                pixels[i] = blockColors[weights[i]].copy();
                pixels[i].alpha = blockColors[aWeights[i]].alpha;
                if (comp_rot >= 1) {
                    int temp = pixels[i].alpha;
                    pixels[i].alpha = pixels[i].get(comp_rot - 1);
                    pixels[i].set(comp_rot - 1, temp);

                }
            }
        }

        private static final class BC7Mode6 {

            public final long m_lo;
            public final long m_hi;

            public BC7Mode6(long lo, long hi) {
                this.m_lo = lo;
                this.m_hi = hi;
            }

            // ---- m_lo fields ----

            public long m_mode() {
                return m_lo & 0x7FL;
            }

            public long m_r0() {
                return (m_lo >>> 7) & 0x7FL;
            }

            public long m_r1() {
                return (m_lo >>> 14) & 0x7FL;
            }

            public long m_g0() {
                return (m_lo >>> 21) & 0x7FL;
            }

            public long m_g1() {
                return (m_lo >>> 28) & 0x7FL;
            }

            public long m_b0() {
                return (m_lo >>> 35) & 0x7FL;
            }

            public long m_b1() {
                return (m_lo >>> 42) & 0x7FL;
            }

            public long m_a0() {
                return (m_lo >>> 49) & 0x7FL;
            }

            public long m_a1() {
                return (m_lo >>> 56) & 0x7FL;
            }

            public long m_p0() {
                return (m_lo >>> 63) & 0x7FL;
            }

            // ---- m_hi fields ----

            public long m_p1() {
                return m_hi & 0x01L;
            }

            public long m_s00() {
                return (m_hi >>> 1) & 0x07L;
            }

            public long m_s10() {
                return (m_hi >>> 4) & 0x0FL;
            }

            public long m_s20() {
                return (m_hi >>> 8) & 0x0FL;
            }

            public long m_s30() {
                return (m_hi >>> 12) & 0x0FL;
            }

            public long m_s01() {
                return (m_hi >>> 16) & 0x0FL;
            }

            public long m_s11() {
                return (m_hi >>> 20) & 0x0FL;
            }

            public long m_s21() {
                return (m_hi >>> 24) & 0x0FL;
            }

            public long m_s31() {
                return (m_hi >>> 28) & 0x0FL;
            }

            public long m_s02() {
                return (m_hi >>> 32) & 0x0FL;
            }

            public long m_s12() {
                return (m_hi >>> 36) & 0x0FL;
            }

            public long m_s22() {
                return (m_hi >>> 40) & 0x0FL;
            }

            public long m_s32() {
                return (m_hi >>> 44) & 0x0FL;
            }

            public long m_s03() {
                return (m_hi >>> 48) & 0x0FL;
            }

            public long m_s13() {
                return (m_hi >>> 52) & 0x0FL;
            }

            public long m_s23() {
                return (m_hi >>> 56) & 0x0FL;
            }

            public long m_s33() {
                return (m_hi >>> 60) & 0x0FL;
            }
        }

        private static void unpackBC7Mode6(long[] dataChunks, ColorRGBA[] pixels) {
            BC7Mode6 block = new BC7Mode6(dataChunks[0], dataChunks[1]);

            if (block.m_mode() != (1 << 6)) {
                throw new IllegalArgumentException("Invalid BC7Mode6 block");
            }

            final long r0 = ((block.m_r0() << 1) | block.m_p0());
            final long g0 = ((block.m_g0() << 1) | block.m_p0());
            final long b0 = ((block.m_b0() << 1) | block.m_p0());
            final long a0 = ((block.m_a0() << 1) | block.m_p0());
            final long r1 = ((block.m_r1() << 1) | block.m_p1());
            final long g1 = ((block.m_g1() << 1) | block.m_p1());
            final long b1 = ((block.m_b1() << 1) | block.m_p1());
            final long a1 = ((block.m_a1() << 1) | block.m_p1());

            ColorRGBA[] vals = new ColorRGBA[16];
            for (int i = 0; i < 16; i++) {
                vals[i] = new ColorRGBA();
            }
            for (int i = 0; i < 16; i++) {
                final long w = gBC7Weights4[i];
                final long iw = 64 - w;
                vals[i].set(0, (r0 * iw + r1 * w + 32) >> 6);
                vals[i].set(1, (g0 * iw + g1 * w + 32) >> 6);
                vals[i].set(2, (b0 * iw + b1 * w + 32) >> 6);
                vals[i].set(3, (a0 * iw + a1 * w + 32) >> 6);
            }

            pixels[0] = vals[(int) block.m_s00()].copy();
            pixels[1] = vals[(int) block.m_s10()].copy();
            pixels[2] = vals[(int) block.m_s20()].copy();
            pixels[3] = vals[(int) block.m_s30()].copy();

            pixels[4] = vals[(int) block.m_s01()].copy();
            pixels[5] = vals[(int) block.m_s11()].copy();
            pixels[6] = vals[(int) block.m_s21()].copy();
            pixels[7] = vals[(int) block.m_s31()].copy();

            pixels[8] = vals[(int) block.m_s02()].copy();
            pixels[9] = vals[(int) block.m_s12()].copy();
            pixels[10] = vals[(int) block.m_s22()].copy();
            pixels[11] = vals[(int) block.m_s32()].copy();

            pixels[12] = vals[(int) block.m_s03()].copy();
            pixels[13] = vals[(int) block.m_s13()].copy();
            pixels[14] = vals[(int) block.m_s23()].copy();
            pixels[15] = vals[(int) block.m_s33()].copy();
        }

        private static long insertWeightZero(long indexBits, int bitsPerIndex, int offset) {
            long LOW_BIT_MASK = (1L << ((bitsPerIndex * (offset + 1)) - 1)) - 1;
            long HIGH_BIT_MASK = ~LOW_BIT_MASK;

            indexBits = ((indexBits & HIGH_BIT_MASK) << 1) | (indexBits & LOW_BIT_MASK);

            return indexBits;
        }

        private static int bc7DeQuant(int val, int pbit, int val_bits) {
            assert (val < (1 << val_bits));
            assert (pbit < 2);
            assert (val_bits >= 4 && val_bits <= 8);
            final int total_bits = val_bits + 1;
            val = (val << 1) | pbit;
            val <<= (8 - total_bits);
            val |= (val >>> total_bits);
            assert (val <= 255);
            return val;
        }

        private static int bc7DeQuant(int val, int val_bits) {
            assert (val < (1 << val_bits));
            assert (val_bits >= 4 && val_bits <= 8);
            val <<= (8 - val_bits);
            val |= (val >>> val_bits);
            assert (val <= 255);
            return val;
        }

        private static int bc7Interp2(int l, int h, int w) {
            assert (w < 4);
            return (l * (64 - gBC7Weights2[w]) + h * gBC7Weights2[w] + 32) >>> 6;
        }

        private static int bc7Interp3(int l, int h, int w) {
            assert (w < 8);
            return (l * (64 - gBC7Weights3[w]) + h * gBC7Weights3[w] + 32) >>> 6;
        }

        private static int bc7Interp4(int l, int h, int w) {
            assert (w < 16);
            return (l * (64 - gBC7Weights4[w]) + h * gBC7Weights4[w] + 32) >>> 6;
        }

        private static int bc7Interp(int l, int h, int w, int bits) {
            assert (l <= 255 && h <= 255);
            return switch (bits) {
                case 2 -> bc7Interp2(l, h, w);
                case 3 -> bc7Interp3(l, h, w);
                case 4 -> bc7Interp4(l, h, w);
                default -> 0;
            };
        }
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
            case BC7 -> (width & ~3) * (height & ~3); // 宽度高度不总是4的倍数，NX会额外添加行数来补齐
        };
    }

    public static int getBufferImageType(WzPngFormat format) {
        return switch (format) {
            case WzPngFormat.ARGB4444, ARGB8888, ARGB1555, DXT3, DXT5, BC7 -> BufferedImage.TYPE_INT_ARGB;
            case RGB565 -> BufferedImage.TYPE_USHORT_565_RGB;
        };
    }
}
