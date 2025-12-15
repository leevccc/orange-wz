package orange.wz.provider.tools;

import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
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
import java.io.File;
import java.nio.file.Path;

@Slf4j
public final class XmlExport {
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

    public static void export(WzImage image, Path filePath, boolean indent, boolean media) {
        try {
            // 创建 Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // 插入数据
            Element root = doc.createElement("imgdir");
            doc.setXmlStandalone(true);
            root.setAttribute("name", image.getName());
            doc.appendChild(root);
            image.getChildren().forEach(prop -> {
                writeProperties(doc, root, prop, media);
            });


            // 写入文件
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            if (indent) {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath.toString()));
            transformer.transform(source, result);
        } catch (ParserConfigurationException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeProperties(Document doc, Element parent, WzImageProperty property, boolean media) {
        Element e;
        switch (property) {
            case WzCanvasProperty prop -> {
                e = doc.createElement("canvas");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("width", String.valueOf(prop.getPng().getWidth()));
                e.setAttribute("height", String.valueOf(prop.getPng().getHeight()));

                if (media) e.setAttribute("png", prop.getPng().getBase64());

                prop.getChildren().forEach(subProperty -> writeProperties(doc, e, subProperty, media));
            }
            case WzConvexProperty prop -> {
                e = doc.createElement("extended");
                e.setAttribute("name", escapeText(prop.getName()));
                prop.getChildren().forEach(subProperty -> writeProperties(doc, e, subProperty, media));
            }
            case WzDoubleProperty prop -> {
                e = doc.createElement("double");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("value", String.valueOf(prop.getValue()));
            }
            case WzFloatProperty prop -> {
                e = doc.createElement("float");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("value", String.valueOf(prop.getValue()));
            }
            case WzIntProperty prop -> {
                e = doc.createElement("int");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("value", String.valueOf(prop.getValue()));
            }
            case WzListProperty prop -> {
                e = doc.createElement("imgdir");
                e.setAttribute("name", escapeText(prop.getName()));
                prop.getChildren().forEach(subProperty -> writeProperties(doc, e, subProperty, media));
            }
            case WzLongProperty prop -> {
                e = doc.createElement("long");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("value", String.valueOf(prop.getValue()));
            }
            case WzNullProperty prop -> {
                e = doc.createElement("null");
                e.setAttribute("name", escapeText(prop.getName()));
            }
            case WzShortProperty prop -> {
                e = doc.createElement("short");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("value", String.valueOf(prop.getValue()));
            }
            case WzSoundProperty prop -> {
                e = doc.createElement("sound");
                e.setAttribute("name", escapeText(prop.getName()));
                if (media) {
                    e.setAttribute("length", String.valueOf(prop.getLenMs()));
                    e.setAttribute("header", prop.getHeaderBase64());
                    e.setAttribute("mp3", prop.getBase64());
                }
            }
            case WzStringProperty prop -> {
                e = doc.createElement("string");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("value", escapeText(prop.getValue()));
            }
            case WzUOLProperty prop -> {
                e = doc.createElement("uol");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("value", escapeText(prop.getValue()));
            }
            case WzVectorProperty prop -> {
                e = doc.createElement("vector");
                e.setAttribute("name", escapeText(prop.getName()));
                e.setAttribute("x", String.valueOf(prop.getX()));
                e.setAttribute("y", String.valueOf(prop.getY()));
            }
            case null, default -> e = null;
        }

        if (e != null) {
            parent.appendChild(e);
        } else {
            log.error("未知的节点类型: {}", property.getName());
        }
    }
}
