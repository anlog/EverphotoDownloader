package cc.ifnot.libs.everphoto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cc.ifnot.libs.everphoto.bean.res.Media;
import cc.ifnot.libs.everphoto.bean.res.URITemp;
import cc.ifnot.libs.everphoto.bean.res.User;
import cc.ifnot.libs.utils.Lg;
import cc.ifnot.libs.utils.MD5;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * author: dp
 * created on: 2020/6/25 2:33 PM
 * description:
 */
public enum EverPhoto {
    INSTANCE;

    private static EverPhotoService evs;

    private static String token;

    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    static {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();

//                        Host:api.everphoto.cn
//                        x-api-version:20161221
//                        user-agent:EverPhoto/2.7.4 (Android;2742;MI 8;29;wandoujia)
//                        x-device-mac:02:00:00:00:00:00
//                        application:tc.everphoto
//                        authorization:Bearer -ULF8SGLsaWeXmQFWlenVBjt
//                        x-locked:1
//                        x-device-id:1046385102960840
//                        device_id:1046385102960840
//                        x-install-id:1503781941153038
//                        x-device-uuid:3d9327b9-7fec-4e1c-a963-4730b5a31a68
//                        x-request-uuid:85d82fb1-8190-417f-a87e-23e6b51659ac
//                        x-uid:6840573484490294286
//                        x-timestamp-ms:1592697574626
                        // Request customization: add request headers
//                        authorization:Bearer -ULF8SGLsaWeXmQFWlenVBjt
                        Map<String, String> ch = new HashMap<>();
                        ch.put("Host", original.url().host().contains("media") ?
                                "media.everphoto.cn" : "api.everphoto.cn");
                        ch.put("x-api-version", "20161221");
                        ch.put("user-agen", "EverPhoto/2.7.4 (Android;2742;MI 8;29;wandoujia)");
                        ch.put("x-device-mac", "02:00:00:00:00:00");
                        ch.put("application", "tc.everphoto");
                        ch.put("x-device-id", "1046385102960840");
                        ch.put("device_id", "1046385102960840");
                        ch.put("x-device-uuid", "3d9327b9-7fec-4e1c-a963-4730b5a31a68");
                        ch.put("x-request-uuid", UUID.randomUUID().toString());
                        if (!original.url().encodedPath().contains("auth")) {
                            ch.put("authorization", "Bearer " + token);
                        }
                        Request.Builder requestBuilder = original.newBuilder()
                                .headers(Headers.of(ch));

                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    }
                })
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.everphoto.cn")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        evs = retrofit.create(EverPhotoService.class);
    }

    String mobile;
    String password;
    String smsCode;
    String out;
    boolean verbose;

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    void doDownload() {
        @NonNull final Disposable disposable = evs.login("+86" + mobile.replace("+86", ""), MD5.toHexString(MD5.md5(
                ("tc.everphoto." + password).getBytes())))
                .flatMap(new Function<User, Observable<URITemp>>() {
                    @Override
                    public Observable<URITemp> apply(User user) throws Throwable {
                        Schedulers.io().scheduleDirect(new Runnable() {
                            @Override
                            public void run() {
                                // todo save token
                            }
                        });
                        Lg.d("login success");
                        token = user.getData().getToken();

                        Lg.d("get settings");
                        return evs.settings();
                    }
                }).flatMap(new Function<URITemp, ObservableSource<Map<Media, URITemp>>>() {
                    @Override
                    public ObservableSource<Map<Media, URITemp>> apply(final URITemp uriTemp) throws Throwable {
                        final HashMap<String, String> queries = new HashMap<>();
                        queries.put("count", "200");
                        return evs.updates(queries).map(new Function<Media, Map<Media, URITemp>>() {
                            @Override
                            public Map<Media, URITemp> apply(Media media) throws Throwable {
                                return Collections.singletonMap(media, uriTemp);
                            }
                        });
                    }
                }).flatMap(new Function<Map<Media, URITemp>, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Map<Media, URITemp> mediaURITempMap) throws Throwable {
                        File file = new File(out);
                        if (file.exists()) {
                            file.mkdirs();
                        }
                        if (file.isFile()) {
                            if (!file.delete()) {
                                Lg.d("file %s delete failed", file.getAbsoluteFile());
                                file = new File(out +
                                        Calendar.getInstance().get(Calendar.YEAR) +
                                        Calendar.getInstance().get(Calendar.MONTH) +
                                        Calendar.getInstance().get(Calendar.DAY_OF_MONTH) +
                                        Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
                                file.mkdirs();
                                Lg.d("use %s as out", file.getAbsoluteFile());
                            }
                        }
                        for (Map.Entry<Media, URITemp> e : mediaURITempMap.entrySet()) {
                            final Media media = e.getKey();
                            final URITemp uri = e.getValue();
                            final String origin = uri.getData().getUri_template().getOrigin();
                            final List<Media.MediaList> media_list = media.getData().getMedia_list();
                            for (final Media.MediaList i : media_list) {
                                final String taken = i.getTaken();
                                final Date date = df.parse(taken);
                                final String dir = String.format("%02d/%02d", date.getYear() + 1900,
                                        date.getMonth() + 1);
                                final File file1 = new File(file, dir);
                                if (!file1.exists()) {
                                    file1.mkdirs();
                                }

                                final boolean isVideo = "video".equalsIgnoreCase(i.getFormat());
                                final String fileName = String.format("%s_%02d%02d%02d_%02d%02d%02d.%s", isVideo ? "VID" : "IMG",
                                        date.getYear() + 1900, date.getMonth() + 1, date.getDate(),
                                        date.getHours(), date.getMinutes(), date.getSeconds(),
                                        isVideo ? "mp4" : "jpg");
                                final File f = new File(out, dir + "/" + fileName);
                                if (f.exists()) {
                                    final MessageDigest md5 = MessageDigest.getInstance("md5");
                                    try (FileInputStream fis = new FileInputStream(f)) {
                                        byte[] buf = new byte[2048];
                                        int read = 0;
                                        while ((read = fis.read(buf)) > 0) {
                                            md5.update(buf);
                                        }

                                        final String md5s = MD5.toHexString(md5.digest());
                                        if (md5s.equalsIgnoreCase(i.getMd5())) {
                                            Lg.d("md5 match, skip download");
                                        }
                                        return Observable.just(1);
                                    }
                                }

                                Schedulers.io().scheduleDirect(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            retrofit2.Response<ResponseBody> res = evs.download(origin.replace("<media_id>",
                                                    String.valueOf(i.getId())) + "?media_token=" + i.getToken()).execute();
                                            assert res.body() != null;
                                            final InputStream inputStream = res.body().byteStream();
                                            FileOutputStream fos = new FileOutputStream(f);
                                            try {
                                                byte[] buf = new byte[2048];
                                                int read = 0;
                                                Lg.d("start writing...");
                                                while ((read = inputStream.read(buf)) > 0) {
                                                    fos.write(buf, 0, read);
                                                }

                                            } finally {
                                                Lg.d("download sucess at %s", f.getAbsolutePath());
                                                inputStream.close();
                                                fos.close();
                                            }

                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                });

                            }

                        }
                        return Observable.just(1);
                    }
                }).subscribeOn(Schedulers.computation()).subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Throwable {
                        Lg.d("%s - ", o);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        Lg.d(throwable);
                    }
                });
        while (!disposable.isDisposed()) {
        }
    }
}
