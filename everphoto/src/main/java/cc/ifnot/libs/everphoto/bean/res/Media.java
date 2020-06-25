package cc.ifnot.libs.everphoto.bean.res;

import java.util.List;

/**
 * author: dp
 * created on: 2020/6/25 5:54 PM
 * description:
 */
public class Media extends Base {
    //      "data": {
//        "media_list": [


    private Data data;
    private Pagination pagination;

    public Data getData() {
        return data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public static class Data {
        private List<MediaList> media_list;

        public List<MediaList> getMedia_list() {
            return media_list;
        }
    }

    public static class MediaList {
        private long id;
        private long status;
        private String created_at;
        private String taken;
        private String token;
        private String source_path;
        private String size;
        private String md5;
        private String format; // jpg or video

        public long getId() {
            return id;
        }

        public String getTaken() {
            return taken;
        }

        public String getToken() {
            return token;
        }

        public long getStatus() {
            return status;
        }

        public String getCreated_at() {
            return created_at;
        }

        public String getSource_path() {
            return source_path;
        }

        public String getSize() {
            return size;
        }

        public String getMd5() {
            return md5;
        }

        public String getFormat() {
            return format;
        }
    }

    public static class Pagination {
        private boolean has_more;
        private String prev;

        public boolean isHas_more() {
            return has_more;
        }

        public String getPrev() {
            return prev;
        }
    }
//          "pagination": {
//        "has_more": false,
//                "prev": "prev_6841115150057996301"
}
