package cc.ifnot.libs.everphoto.bean.res;

/**
 * author: dp
 * created on: 2020/6/25 2:28 PM
 * description:
 */
public class User extends Base {

    private UserData data;

    public User(String tk) {
        this.data = new UserData(tk);
    }

    public UserData getData() {
        return data;
    }

    @Override
    public String toString() {
        return "User{" +
                "data=" + data +
                '}';
    }

    public static class UserData {

        private String token;

        public UserData(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
