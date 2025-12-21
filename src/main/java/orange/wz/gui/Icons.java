package orange.wz.gui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public final class Icons {
    public static final FlatSVGIcon FcFolderIcon = getSVG("FcFolder.svg", 16, 16);
    public static final FlatSVGIcon FcFolderBlueIcon = getSVG("FcFolder.svg", 16, 16, Color.BLUE);
    public static final FlatSVGIcon FcFileIcon = getSVG("FcFile.svg", 16, 16);
    public static final FlatSVGIcon AiOutlineFileWordIcon = getSVG("AiOutlineFileWord.svg", 16, 16);
    public static final FlatSVGIcon AiOutlineFileMarkdownIcon = getSVG("AiOutlineFileMarkdown.svg", 16, 16);
    public static final FlatSVGIcon ImgIcon = getSVG("IMG.svg", 16, 16);
    public static final FlatSVGIcon ListIcon = getSVG("LIST.svg", 16, 16);
    public static final FlatSVGIcon StrIcon = getSVG("STR.svg", 16, 16);
    public static final FlatSVGIcon PngIcon = getSVG("PNG.svg", 16, 16);
    public static final FlatSVGIcon IntIcon = getSVG("INT.svg", 16, 16);
    public static final FlatSVGIcon DoubleIcon = getSVG("DOUBLE.svg", 16, 16);
    public static final FlatSVGIcon FloatIcon = getSVG("FLOAT.svg", 16, 16);
    public static final FlatSVGIcon LongIcon = getSVG("LONG.svg", 16, 16);
    public static final FlatSVGIcon NullIcon = getSVG("NULL.svg", 16, 16);
    public static final FlatSVGIcon RawIcon = getSVG("RAW.svg", 16, 16);
    public static final FlatSVGIcon ShortIcon = getSVG("SHORT.svg", 16, 16);
    public static final FlatSVGIcon WavIcon = getSVG("WAV.svg", 16, 16);
    public static final FlatSVGIcon UolIcon = getSVG("UOL.svg", 16, 16);
    public static final FlatSVGIcon ConvexIcon = getSVG("CONVEX.svg", 16, 16);
    public static final FlatSVGIcon VectorIcon = getSVG("VECTOR.svg", 16, 16);
    public static final FlatSVGIcon AiOutlineSaveIcon = getSVG("AiOutlineSave.svg", 16, 16);
    public static final FlatSVGIcon AiOutlineCloseIcon = getSVG("AiOutlineClose.svg", 16, 16);
    public static final FlatSVGIcon AiOutlineReloadIcon = getSVG("AiOutlineReload.svg", 16, 16);

    private static FlatSVGIcon getSVG(String filename, int width, int height) {
        return getSVG(filename, width, height, null);
    }

    private static FlatSVGIcon getSVG(String filename, int width, int height, Color color) {
        FlatSVGIcon svg = new FlatSVGIcon("icons/" + filename, width, height);
        if (color != null) {
            svg.setColorFilter(new FlatSVGIcon.ColorFilter() {
                @Override
                public Color filter(Color color) {
                    return new Color(0x2196F3);
                }
            });
        }

        return svg;
    }

    public static Image loadImage(String name) {
        try (InputStream in = MainFrame.class.getResourceAsStream("/" + name)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
