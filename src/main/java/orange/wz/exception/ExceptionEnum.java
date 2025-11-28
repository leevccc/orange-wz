package orange.wz.exception;

public enum ExceptionEnum implements BaseErrorInfoInterface {
    SUCCESS(20000, "成功!"),

    NOT_FOUND(40000, "未找到该资源!"),
    METHOD_ERROR(40001, "当前请求方法不支持！"),
    BODY_NOT_MATCH(40002, "请求的数据格式不符!"),
    ACCESS_DENIED(40003, "拒绝访问！"),
    NOT_FOUND_ACCOUNT(40100, "账号不存在！"),
    NOT_FOUND_WZ_KEY(40101, "找不到密钥！"),
    NOT_FOUND_FILE_OR_FOLDER(40102, "文件或目录不存在！"),
    CLIPBOARD_IS_EMPTY(40103, "没有复制东西！"),

    INTERNAL_SERVER_ERROR(50000, "服务器内部错误!"),
    REPEAT(50001, "资源已存在!"),
    INVALID_ACCOUNT_OR_PASSWORD(50002, "账号或密码格式不符合要求!"),
    ACCOUNT_IS_CONNECTED(50003, "账号正在游戏中!"),
    INVALID_OLD_PASSWORD(50004, "旧密码不匹配!"),
    WZ_FILE_TYPE_ERROR(50100, "文件类型错误!"),
    WZ_FILE_NO_VERSION_OR_KEY(50101, "未指定版本或者密钥!"),
    IS_NOT_FILE_OR_FOLDER(50102, "操作对象不是文件或文件夹!"),
    DISABLE_TO_DELETE_SYSTEM_CONFIG(50103, "禁止删除系统关键数据!"),
    DISABLE_TO_UPDATE_SYSTEM_CONFIG(50104, "禁止修改系统关键数据!"),
    REPEAT_NAME(50105, "已有同名数据!"),
    IS_OPENED(50106, "不要重复打开相同的文件!"),
    ONLY_WZ_IMAGE(50107, "WzImage 只能粘贴 WzImageProperty"),
    ONLY_WZ_DIR_OR_IMAGE(50108, "Wz 只能粘贴 WzDirectory 或 WzImage"),
    ;

    private final int resultCode;
    private final String resultMsg;

    ExceptionEnum(int resultCode, String resultMsg) {
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
    }

    @Override
    public Integer getResultCode() {
        return resultCode;
    }

    @Override
    public String getResultMsg() {
        return resultMsg;
    }
}
