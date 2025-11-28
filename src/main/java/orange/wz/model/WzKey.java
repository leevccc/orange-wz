package orange.wz.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Base64;

public class WzKey {
    private Integer id;
    private String name;
    private byte[] iv;
    private byte[] userKey;

    // 无参构造函数
    public WzKey() {}

    // 构造函数
    public WzKey(Integer id, String name, byte[] iv, byte[] userKey) {
        this.id = id;
        this.name = name;
        this.iv = iv;
        this.userKey = userKey;
    }

    // Getter 和 Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public byte[] getIv() { return iv; }
    public void setIv(byte[] iv) { this.iv = iv; }

    public byte[] getUserKey() { return userKey; }
    public void setUserKey(byte[] userKey) { this.userKey = userKey; }

    // 为了方便JSON序列化，提供Base64编码的字符串属性
    @JsonIgnore
    public String getIvBase64() {
        return Base64.getEncoder().encodeToString(iv);
    }

    @JsonIgnore
    public String getUserKeyBase64() {
        return Base64.getEncoder().encodeToString(userKey);
    }

    public void setIvBase64(String ivBase64) {
        this.iv = Base64.getDecoder().decode(ivBase64);
    }

    public void setUserKeyBase64(String userKeyBase64) {
        this.userKey = Base64.getDecoder().decode(userKeyBase64);
    }
}