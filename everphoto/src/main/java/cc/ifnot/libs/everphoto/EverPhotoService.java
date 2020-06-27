package cc.ifnot.libs.everphoto;

import java.util.Map;

import cc.ifnot.libs.everphoto.bean.res.Media;
import cc.ifnot.libs.everphoto.bean.res.MediaInfo;
import cc.ifnot.libs.everphoto.bean.res.URITemp;
import cc.ifnot.libs.everphoto.bean.res.User;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * author: dp
 * created on: 2020/6/25 2:44 PM
 * description:
 */
interface EverPhotoService {

    @FormUrlEncoded
    @POST("auth")
    Observable<User> login(@Field("mobile") String mobile,
                           @Field("password") String password);

    //    https://api.everphoto.cn/application/settings
    @GET("application/settings")
    Observable<URITemp> settings();

    @GET("users/self/updates")
    Observable<Media> updates(@QueryMap Map<String, String> queries);

    //    https://media.everphoto.cn/origin/6840599129748406797?media_token=0baafbe63d53a96f93ef99704b1f5898
    @Streaming
    @GET
    Call<ResponseBody> download(@Url String url);

    @GET("media/{id}/info")
    Call<MediaInfo> info(@Header("authorization") String auth, @Path("id") long id);
}
