package cc.ifnot.libs.everphoto.bean.res;

/**
 * author: dp
 * created on: 2020/6/25 5:16 PM
 * description:
 */
public class URITemp extends Base {
    private Data data;

    public Data getData() {
        return data;
    }

    public class Data {
        private URITemplate uri_template;

        public URITemplate getUri_template() {
            return uri_template;
        }
    }

    public class URITemplate {
        private String origin;
        private String video;
        private String p1080;
        private String p720;
        private String p360;
        private String s240;
        private String avatar;

        public String getOrigin() {
            return origin;
        }

        public String getVideo() {
            return video;
        }

        public String getP1080() {
            return p1080;
        }

        public String getP720() {
            return p720;
        }

        public String getP360() {
            return p360;
        }

        public String getS240() {
            return s240;
        }

        public String getAvatar() {
            return avatar;
        }
    }
//    {
//        "timestamp": 1593076510,
//            "code": 0,
//            "data": {
//        "uri_template": {
//            "origin": "https://media.everphoto.cn/origin/<media_id>",
//                    "video": "https://media.everphoto.cn/video/<media_id>",
//                    "p1080": "https://media.everphoto.cn/1080p/<media_id>.webp",
//                    "p720": "https://media.everphoto.cn/720p/<media_id>.webp",
//                    "p360": "https://media.everphoto.cn/360p/<media_id>.webp",
//                    "s240": "https://media.everphoto.cn/240x240/<media_id>.webp",
//                    "avatar": "https://media.everphoto.cn/avatar/<user_id>.webp"
//        },
}
