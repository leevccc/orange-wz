package orange.wz.provider.tools;

import lombok.Getter;

public enum WzFileStatus {
    UNPARSE("文件未解析"),
    ERROR_PATH("文件路径错误"),
    ERROR_FILE_VERSION("文件版本错误"),
    ERROR_KEY("文件密钥错误"),
    ERROR_SPECIAL_ENCODE("无法解析的商业加密文件"),
    PARSE_SUCCESS("文件解析成功");

    @Getter
    private final String message;

    WzFileStatus(String message) {
        this.message = message;
    }
}
