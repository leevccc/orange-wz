package orange.wz.provider.tools;

import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzXmlFile;
import orange.wz.provider.properties.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
public final class XmlExport {
    private final WzImage image;
    private final int indent;
    private final boolean linux;
    private final MediaExportType meType;

    private BufferedWriter writer;
    private Path mediaFolder;
    private int curIndent = 0;

    public XmlExport(WzImage image, int indent, boolean linux, MediaExportType meType) {
        this.image = image;
        this.indent = indent;
        this.linux = linux;
        this.meType = meType;
    }

    private void writeLineSeparator() throws IOException {
        if (indent <= 0) return;
        writer.write(linux ? "\n" : "\r\n");
    }

    private void writeIndent() throws IOException {
        if (indent <= 0 || curIndent <= 0) return;
        int spaces = indent * curIndent;
        char[] buffer = new char[spaces];
        Arrays.fill(buffer, ' ');
        writer.write(buffer);
    }

    public boolean export(Path filePath) {
        String imgName = image instanceof WzXmlFile xml ? xml.getImgName() : image.getName();
        mediaFolder = filePath.getParent().resolve("media").resolve(imgName);
        if (meType == MediaExportType.FILE) {
            try {
                FileTool.deleteDirectory(mediaFolder);
                FileTool.createDirectory(mediaFolder);
            } catch (Exception e) {
                log.error("media 目录被占用: {} {}", mediaFolder, e.getMessage());
                return false;
            }
        }

        try {
            FileTool.createDirectory(filePath.getParent());
        } catch (Exception e) {
            log.error("创建目录失败: {} {}", filePath.getParent(), e.getMessage());
            return false;
        }

        try (FileOutputStream fos = new FileOutputStream(filePath.toString())) {

            writer = new BufferedWriter(
                    new OutputStreamWriter(fos, StandardCharsets.UTF_8),
                    64 * 1024 // 64KB buffer（可以调大）
            );

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            writeLineSeparator();


            writer.write("<imgdir name=\"" + imgName + "\" indent=\"" + indent + "\" media=\"" + meType.name() + "\">");
            writeLineSeparator();
            curIndent++;
            image.getChildren().forEach(prop -> writeProp(prop, ""));
            curIndent--;
            writer.write("</imgdir>");
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private void writeProp(WzImageProperty property, String mediaFilename) {
        try {
            writeIndent();
            switch (property) {
                case WzCanvasProperty prop -> {
                    String etName = escapeText(prop.getName());
                    String format = String.valueOf(prop.getFormat().getValue());
                    String scale = String.valueOf(prop.getScale());
                    String context = "<canvas name=\"" + etName + "\" format=\"" + format + "\" scale=\"" + scale + "\"";

                    if (meType == MediaExportType.BASE64)
                        context = context + " basedata=\"" + Base64Tool.coverBytesToBase64(prop.getImageBytes(false)) + "\"";
                    else if (meType == MediaExportType.FILE) {
                        String filename = FileTool.safeFileName(mediaFilename + prop.getName() + ".png");
                        Path p = mediaFolder.resolve(filename);
                        FileTool.saveFile(p, prop.getImageBytes(false));
                    }

                    List<WzImageProperty> children = prop.getChildren();
                    if (children.isEmpty()) {
                        context += "/>";
                        writer.write(context);
                        writeLineSeparator();
                    } else {
                        context += ">";
                        writer.write(context);
                        writeLineSeparator();
                        curIndent++;
                        children.forEach(subProperty -> writeProp(subProperty, mediaFilename + prop.getName() + "."));
                        curIndent--;
                        writeIndent();
                        writer.write("</canvas>");
                        writeLineSeparator();
                    }
                }
                case WzConvexProperty prop -> {
                    writer.write("<extended name=\"" + escapeText(prop.getName()) + "\"");
                    List<WzImageProperty> children = prop.getChildren();
                    if (children.isEmpty()) {
                        writer.write("/>");
                        writeLineSeparator();
                    } else {
                        writer.write(">");
                        writeLineSeparator();
                        curIndent++;
                        children.forEach(subProperty -> writeProp(subProperty, mediaFilename + prop.getName() + "."));
                        curIndent--;
                        writeIndent();
                        writer.write("</extended>");
                        writeLineSeparator();
                    }
                }
                case WzDoubleProperty prop -> {
                    writer.write("<double name=\"" + escapeText(prop.getName()) + "\" value=\"" + prop.getValue() + "\"/>");
                    writeLineSeparator();
                }
                case WzFloatProperty prop -> {
                    writer.write("<float name=\"" + escapeText(prop.getName()) + "\" value=\"" + prop.getValue() + "\"/>");
                    writeLineSeparator();
                }
                case WzIntProperty prop -> {
                    writer.write("<int name=\"" + escapeText(prop.getName()) + "\" value=\"" + prop.getValue() + "\"/>");
                    writeLineSeparator();
                }
                case WzListProperty prop -> {
                    writer.write("<imgdir name=\"" + escapeText(prop.getName()) + "\"");
                    List<WzImageProperty> children = prop.getChildren();
                    if (children.isEmpty()) {
                        writer.write("/>");
                        writeLineSeparator();
                    } else {
                        writer.write(">");
                        writeLineSeparator();
                        curIndent++;
                        children.forEach(subProperty -> writeProp(subProperty, mediaFilename + prop.getName() + "."));
                        curIndent--;
                        writeIndent();
                        writer.write("</imgdir>");
                        writeLineSeparator();
                    }
                }
                case WzLongProperty prop -> {
                    writer.write("<long name=\"" + escapeText(prop.getName()) + "\" value=\"" + prop.getValue() + "\"/>");
                    writeLineSeparator();
                }
                case WzNullProperty prop -> {
                    writer.write("<null name=\"" + escapeText(prop.getName()) + "\"/>");
                    writeLineSeparator();
                }
                case WzShortProperty prop -> {
                    writer.write("<short name=\"" + escapeText(prop.getName()) + "\" value=\"" + prop.getValue() + "\"/>");
                    writeLineSeparator();
                }
                case WzSoundProperty prop -> {
                    String context = "<sound name=\"" + escapeText(prop.getName()) + "\"";

                    if (meType == MediaExportType.BASE64) {
                        String basehead = Base64Tool.coverBytesToBase64(prop.getHeader());
                        String basedata = Base64Tool.coverBytesToBase64(prop.getSoundBytes(false));
                        context = context + " length=\"" + prop.getLenMs() + "\" basehead=\"" + basehead + "\" basedata=\"" + basedata + "\"/>";
                    } else if (meType == MediaExportType.FILE) {
                        String basehead = Base64Tool.coverBytesToBase64(prop.getHeader());
                        context = context + " length=\"" + prop.getLenMs() + "\" basehead=\"" + basehead + "\"/>";

                        String filename = FileTool.safeFileName(mediaFilename + prop.getName() + ".mp3");
                        Path p = mediaFolder.resolve(filename);
                        FileTool.saveFile(p, prop.getSoundBytes(false));
                    } else {
                        context = context + "/>";
                    }

                    writer.write(context);
                    writeLineSeparator();
                }
                case WzStringProperty prop -> {
                    writer.write("<string name=\"" + escapeText(prop.getName()) + "\" value=\"" + escapeText(prop.getValue()) + "\"/>");
                    writeLineSeparator();
                }
                case WzUOLProperty prop -> {
                    writer.write("<uol name=\"" + escapeText(prop.getName()) + "\" value=\"" + escapeText(prop.getValue()) + "\"/>");
                    writeLineSeparator();
                }
                case WzVectorProperty prop -> {
                    writer.write("<vector name=\"" + escapeText(prop.getName()) + "\" x=\"" + prop.getX() + "\" y=\"" + prop.getY() + "\"/>");
                    writeLineSeparator();
                }
                case null, default -> log.error("未知的节点类型: {}", property.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String escapeText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 10);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }

}
