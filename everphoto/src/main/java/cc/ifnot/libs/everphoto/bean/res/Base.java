package cc.ifnot.libs.everphoto.bean.res;

/**
 * author: dp
 * created on: 2020/6/25 2:29 PM
 * description:
 */
public class Base {
    private long timestamp;
    private int code;
    private String message;

    public long getTimestamp() {
        return timestamp;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Base{" +
                "timestamp=" + timestamp +
                ", code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
