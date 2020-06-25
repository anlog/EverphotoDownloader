package cc.ifnot.libs.everphoto.bean.req;

/**
 * author: dp
 * created on: 2020/6/25 2:46 PM
 * description:
 */
public class Login {
    private String mobile;
    private String password;

    public Login(String mobile, String password) {
        this.mobile = mobile;
        this.password = password;
    }
}
