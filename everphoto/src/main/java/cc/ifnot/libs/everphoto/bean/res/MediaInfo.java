package cc.ifnot.libs.everphoto.bean.res;

/**
 * author: dp
 * created on: 2020/6/27 3:19 PM
 * description:
 */
public class MediaInfo extends Base {
    private Media.MediaList data;

    public Media.MediaList getData() {
        return data;
    }

    @Override
    public String toString() {
        return "MediaInfo{" +
                "data=" + data +
                '}';
    }
}
