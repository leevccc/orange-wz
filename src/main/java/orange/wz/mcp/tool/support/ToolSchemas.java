package orange.wz.mcp.tool.support;

import java.util.List;
import java.util.Map;

public final class ToolSchemas {
    private ToolSchemas() {
    }

    public static Map<String, Object> emptyObjectSchema() {
        return objectSchema(Map.of(), List.of());
    }

    public static Map<String, Object> keySchema() {
        return objectSchema(
                Map.of(
                        "name", stringSchema(),
                        "ivBase64", stringSchema(),
                        "userKeyBase64", stringSchema()
                ),
                List.of("name", "ivBase64", "userKeyBase64")
        );
    }

    public static Map<String, Object> nodeReferenceSchema() {
        return objectSchema(nodeReferenceProperties(), List.of("rootPath"));
    }

    public static Map<String, Object> nodeReferenceWithAutoParseSchema() {
        return objectSchema(
                Map.of(
                        "rootPath", stringSchema(),
                        "nodePath", stringSchema(),
                        "autoParse", booleanSchema()
                ),
                List.of("rootPath")
        );
    }

    public static Map<String, Object> nodeReferenceWithReadOptionsSchema() {
        return objectSchema(
                Map.of(
                        "rootPath", stringSchema(),
                        "nodePath", stringSchema(),
                        "autoParse", booleanSchema(),
                        "maxDepth", numberSchema()
                ),
                List.of("rootPath")
        );
    }

    public static Map<String, Object> nodeReferenceProperties() {
        return Map.of(
                "rootPath", stringSchema(),
                "nodePath", stringSchema()
        );
    }

    public static Map<String, Object> queryOperationSchema() {
        return objectSchema(
                Map.of(
                        "op", stringSchema(),
                        "rootPath", stringSchema(),
                        "nodePath", stringSchema(),
                        "keyword", stringSchema(),
                        "searchIn", stringSchema(),
                        "type", stringSchema(),
                        "includeTree", booleanSchema(),
                        "maxDepth", numberSchema(),
                        "autoParse", booleanSchema()
                ),
                List.of()
        );
    }

    public static Map<String, Object> updateOperationSchema() {
        return objectSchema(
                Map.ofEntries(
                        Map.entry("rootPath", stringSchema()),
                        Map.entry("nodePath", stringSchema()),
                        Map.entry("op", stringSchema()),
                        Map.entry("autoParse", booleanSchema()),
                        Map.entry("type", stringSchema()),
                        Map.entry("name", stringSchema()),
                        Map.entry("value", stringSchema()),
                        Map.entry("x", numberSchema()),
                        Map.entry("y", numberSchema()),
                        Map.entry("base64Png", stringSchema()),
                        Map.entry("base64Mp3", stringSchema()),
                        Map.entry("pngFormat", stringSchema()),
                        Map.entry("filePath", stringSchema())
                ),
                List.of("rootPath")
        );
    }

    public static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", required,
                "additionalProperties", true
        );
    }

    public static Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    public static Map<String, Object> numberSchema() {
        return Map.of("type", "number");
    }

    public static Map<String, Object> booleanSchema() {
        return Map.of("type", "boolean");
    }

    public static Map<String, Object> arraySchema(Map<String, Object> itemSchema) {
        return Map.of(
                "type", "array",
                "items", itemSchema
        );
    }
}
