package orange.wz.provider.tools;

import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.WzXmlFile;
import orange.wz.provider.properties.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

@Slf4j
public final class XmlImport {
    public static String unescapeText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&') {
                if (input.startsWith("&quot;", i)) {
                    sb.append('"');
                    i += 5;
                } else if (input.startsWith("&apos;", i)) {
                    sb.append('\'');
                    i += 5;
                } else if (input.startsWith("&amp;", i)) {
                    sb.append('&');
                    i += 4;
                } else if (input.startsWith("&lt;", i)) {
                    sb.append('<');
                    i += 3;
                } else if (input.startsWith("&gt;", i)) {
                    sb.append('>');
                    i += 3;
                } else {
                    sb.append('&'); // 不认识的实体，原样保留
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static WzXmlFile importXml(Path filePath, String keyBoxName, byte[] iv, byte[] key) {
        String filename = filePath.getFileName().toString();
        String filePathStr = filePath.toString();
        WzXmlFile image = new WzXmlFile(filename, filePathStr, keyBoxName, iv, key);
        if (importXml(image, filePath)) {
            return image;
        }
        return null;
    }

    public static boolean importXml(WzXmlFile wzXmlFile, Path filePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(filePath.toFile());

            Element root = doc.getDocumentElement();
            if (!"imgdir".equals(root.getNodeName())) {
                log.error("根节点必须是imgdir");
                return false;
            }

            if (root.hasAttribute("indent")) {
                wzXmlFile.setIndent(Integer.parseInt(root.getAttribute("indent")));
            }
            if (root.hasAttribute("media")) {
                wzXmlFile.setMeType(MediaExportType.valueOf(root.getAttribute("media")));
            }

            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node instanceof Element element) {
                    WzImageProperty prop = readProperty(element, wzXmlFile, wzXmlFile, "");
                    if (prop != null) {
                        wzXmlFile.addChild(prop, true);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Xml 导入失败 文件: {} 错误信息: {}", wzXmlFile.getName(), e.getMessage());
            return false;
        }
    }

    private static WzImageProperty readProperty(Element e, WzObject parent, WzXmlFile image, String mediaFileName) {
        String name = unescapeText(e.getAttribute("name"));

        return switch (e.getNodeName()) {
            case "imgdir" -> {
                WzListProperty list = new WzListProperty(name, parent, image);
                readChildren(e, list, image, mediaFileName + name + ".");
                yield list;
            }

            case "canvas" -> {
                int width = 0;
                int height = 0;
                int format = 2;
                int scale = 0;
                try {
                    width = Integer.parseInt(e.getAttribute("width"));
                    height = Integer.parseInt(e.getAttribute("height"));
                    format = Integer.parseInt(e.getAttribute("format"));
                    scale = Integer.parseInt(e.getAttribute("scale"));
                } catch (Exception ex) {
                    log.warn("Image: {} CanvasNode: {} Error: {}", image.getName(), name, ex.getMessage());
                }

                byte[] imageBytes = null;
                if (image.getMeType() == MediaExportType.BASE64 || e.hasAttribute("basedata")) {
                    imageBytes = Base64Tool.coverBase64ToBytes(e.getAttribute("basedata"));
                } else if (image.getMeType() == MediaExportType.FILE) {
                    Path mediaPath = Path.of(image.getFilePath()).getParent().resolve("media").resolve(image.getImgName()).resolve(FileTool.safeFileName(mediaFileName + name + ".png"));
                    imageBytes = FileTool.readFile(mediaPath);
                }

                WzCanvasProperty canvas = new WzCanvasProperty(name, width, height, format, scale, imageBytes, parent, image);
                readChildren(e, canvas, image, mediaFileName + name + ".");
                yield canvas;
            }

            case "extended" -> {
                WzConvexProperty convex = new WzConvexProperty(name, parent, image);
                readChildren(e, convex, image, mediaFileName + name + ".");
                yield convex;
            }

            case "int" -> new WzIntProperty(name, Integer.parseInt(e.getAttribute("value")), parent, image);

            case "short" -> new WzShortProperty(name, Short.parseShort(e.getAttribute("value")), parent, image);

            case "long" -> new WzLongProperty(name, Long.parseLong(e.getAttribute("value")), parent, image);

            case "float" -> new WzFloatProperty(name, Float.parseFloat(e.getAttribute("value")), parent, image);

            case "double" -> new WzDoubleProperty(name, Double.parseDouble(e.getAttribute("value")), parent, image);

            case "string" -> new WzStringProperty(name, unescapeText(e.getAttribute("value")), parent, image);

            case "uol" -> new WzUOLProperty(name, unescapeText(e.getAttribute("value")), parent, image);

            case "vector" -> {
                int x = Integer.parseInt(e.getAttribute("x"));
                int y = Integer.parseInt(e.getAttribute("y"));
                yield new WzVectorProperty(name, x, y, parent, image);
            }

            case "sound" -> {
                int length = 0;
                byte[] header = null;
                byte[] mp3 = null;
                try {
                    if (image.getMeType() == MediaExportType.BASE64 || e.hasAttribute("basedata")) {
                        length = Integer.parseInt(e.getAttribute("length"));
                        header = Base64Tool.coverBase64ToBytes(e.getAttribute("basehead"));
                        mp3 = Base64Tool.coverBase64ToBytes(e.getAttribute("basedata"));
                    } else if (image.getMeType() == MediaExportType.FILE) {
                        length = Integer.parseInt(e.getAttribute("length"));
                        header = Base64Tool.coverBase64ToBytes(e.getAttribute("basehead"));

                        Path mediaPath = Path.of(image.getFilePath()).getParent().resolve("media").resolve(image.getImgName()).resolve(FileTool.safeFileName(mediaFileName + name + ".mp3"));
                        mp3 = FileTool.readFile(mediaPath);
                    }
                } catch (Exception ex) {
                    log.warn("Image: {} SoundNode: {} Error: {}", image.getName(), name, ex.getMessage());
                }

                yield new WzSoundProperty(name, length, header, mp3, parent, image);
            }

            case "null" -> new WzNullProperty(name, parent, image);

            default -> {
                log.error("未知节点类型: {}", e.getNodeName());
                yield null;
            }
        };
    }

    private static void readChildren(Element parentXml, WzImageProperty parentProp, WzXmlFile image, String mediaFileName) {
        NodeList children = parentXml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element) {
                WzImageProperty child = readProperty(element, parentProp, image, mediaFileName);
                if (child != null) {
                    parentProp.addChild(child, true);
                }
            }
        }
    }

}
