package cc.ifnot.libs.everphoto;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cc.ifnot.libs.everphoto.bean.res.Base;
import cc.ifnot.libs.everphoto.bean.res.Media;
import cc.ifnot.libs.everphoto.bean.res.URITemp;
import cc.ifnot.libs.everphoto.bean.res.User;
import cc.ifnot.libs.utils.Lg;
import cc.ifnot.libs.utils.MD5;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
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

    private static final ThreadLocal<SimpleDateFormat> threadLocal = new
            ThreadLocal<SimpleDateFormat>();
    private static final int BUF_SIZE = 1024 * 1024;
    private static EverPhotoService evs;
    private static String token;
    private static OkHttpClient okHttpClient;

    static {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.MINUTES)
                .writeTimeout(3, TimeUnit.MINUTES)
//                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();

                        Map<String, String> ch = new HashMap<>();
//                        ch.put("Host", original.url().host().contains("media") ?
//                                "media.everphoto.cn" : "api.everphoto.cn");
                        ch.put("x-api-version", "20161221");
                        ch.put("user-agent", "EverPhoto/2.7.4 (Android;2742;MI 8;29;wandoujia)");
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
                }).addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.NONE))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.everphoto.cn")
                .client(okHttpClient)
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

    AtomicInteger all = new AtomicInteger(0);
    AtomicInteger index = new AtomicInteger(0);
    Map<Long, AtomicInteger> refs = Collections.synchronizedMap(new HashMap<>());
    private @NonNull Scheduler downloadSchedulers = Schedulers.from(
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1));
    private FileOutputStream dfs;
    private FileOutputStream errfs;

    public EverPhoto setMobile(String mobile) {
        this.mobile = mobile;
        return this;
    }

    public EverPhoto setPassword(String password) {
        this.password = password;
        return this;
    }

    public EverPhoto setSmsCode(String smsCode) {
        this.smsCode = smsCode;
        return this;
    }

    public EverPhoto setOut(String out) {
        this.out = out;
        return this;
    }

    public EverPhoto setThreadCount(int threads) {
        downloadSchedulers = Schedulers.from(Executors.newFixedThreadPool(threads));
        return this;
    }

    public EverPhoto setVerbose(boolean verbose) {
        this.verbose = verbose;
        if (verbose) {
            Lg.level(Lg.MORE);
            for (Interceptor i : okHttpClient.interceptors()) {
                if (i instanceof HttpLoggingInterceptor) {
                    ((HttpLoggingInterceptor) i).setLevel(HttpLoggingInterceptor.Level.BODY);
                }
            }
        }
        return this;
    }

    public Observable<String> doDownload() {
        return Observable.fromCallable(new Callable<User>() {
            @Override
            public User call() throws Exception {

                if (out != null && out.length() > 0) {
                    final File f = new File(out);
                    if (!f.isDirectory()) {
                        f.delete();
                    }
                    if (!f.exists()) {
                        f.mkdirs();
                    }

                    final File download = new File(f, ".download");
                    if (download.isDirectory()) {
                        download.delete();
                    }
                    if (download.exists()) {
                        download.renameTo(new File(f, ".download.old"));
                    }
                    dfs = new FileOutputStream(download);
                    dfs.write(("## " + new Date().toString()).getBytes());
                    dfs.write('\n');

                    final File errf = new File(f, ".err");
                    if (errf.isDirectory()) {
                        errf.delete();
                    }
                    if (errf.exists()) {
                        errf.renameTo(new File(f, ".err.old"));
                    }
                    errfs = new FileOutputStream(errf);

                    final File tk = new File(f, ".token");
                    if (tk.exists() && tk.length() > 0) {
                        byte[] tkbuf = new byte[1024];
                        try (final FileInputStream fis = new FileInputStream(tk)) {
                            final int read = fis.read(tkbuf);
                            token = new String(tkbuf, 0, read);
                            Lg.w("read cached token from %s", tk.getAbsolutePath());
                            return new User(token);
                        }
                    }
                }

                Lg.d("do login");
                @NonNull final User user = evs.login("+86" + mobile.replace("+86", ""), MD5.toHexString(MD5.md5(
                        ("tc.everphoto." + password).getBytes()))).blockingSingle();
                token = user.getData().getToken();
                if (token != null && token.length() > 0) {
                    if (out != null && out.length() > 0) {
                        final File f = new File(out);
                        if (!f.isDirectory()) {
                            f.delete();
                        }
                        if (!f.exists()) {
                            f.mkdirs();
                        }
                        final File tk = new File(f, ".token");
                        if (tk.exists()) {
                            tk.delete();
                        }
                        byte[] tkbuf = new byte[1024];
                        try (final FileOutputStream fos = new FileOutputStream(tk)) {
                            fos.write(token.getBytes(), 0, token.length());
                            Lg.w("write token to %s", tk.getAbsolutePath());
                        }

                    }
                }
                return user;
            }
        }).observeOn(Schedulers.io())
                .flatMap(new Function<User, Observable<URITemp>>() {
                    @Override
                    public Observable<URITemp> apply(User user) throws Throwable {
                        token = user.getData().getToken();

                        Lg.d("get settings");
                        return evs.settings();
                    }
                }).observeOn(Schedulers.io()).flatMap(new Function<URITemp, ObservableSource<Map.Entry<@NonNull Media, URITemp>>>() {
                    @Override
                    public ObservableSource<Map.Entry<@NonNull Media, URITemp>> apply(final URITemp uriTemp) throws Throwable {
                        Lg.d("get counts..");
                        return new ObservableSource<Map.Entry<@NonNull Media, URITemp>>() {
                            @Override
                            public void subscribe(@NonNull Observer<? super Map.Entry<@NonNull Media, URITemp>> observer) {
                                boolean more = true;
                                String prev = null;
                                while (more) {
                                    final HashMap<String, String> queries = new HashMap<>();
                                    queries.put("count", "200");
                                    if (prev != null && prev.length() > 0) {
                                        queries.put("p", prev);
                                    }
                                    Lg.d("get count (prev: %s) -> %s", prev, queries.toString());
                                    @NonNull final Media media = evs.updates(queries).subscribeOn(Schedulers.computation())
                                            .blockingSingle();
                                    more = media.getPagination().isHas_more();
                                    prev = media.getPagination().getPrev();
                                    Lg.d("get count end -> %s - %s", more, prev);
                                    final Map.Entry<@NonNull Media, URITemp> entry = new HashMap.SimpleEntry<>(media, uriTemp);
                                    observer.onNext(entry);
                                }
//                                Lg.d("onComplete");
//                                observer.onComplete();
//                                okHttpClient.dispatcher().cancelAll();
//                                okHttpClient.connectionPool().evictAll();
//                                okHttpClient.dispatcher().executorService().shutdown();
//                                Lg.d(okHttpClient.connectionPool().connectionCount() + "" +
//                                        " connectionPool " + okHttpClient.connectionPool().idleConnectionCount());
//                                Lg.d(okHttpClient.dispatcher().queuedCallsCount()
//                                        + " == " + okHttpClient.dispatcher().runningCallsCount());
                            }
                        };
                    }
                }).observeOn(Schedulers.io()).flatMap(new Function<Map.Entry<@NonNull Media, URITemp>, ObservableSource<DownloadBena>>() {
                    @Override
                    public ObservableSource<DownloadBena> apply(Map.Entry<@NonNull Media, URITemp> entry) throws Throwable {
                        Lg.d("parse media in");
                        return new ObservableSource<DownloadBena>() {
                            @Override
                            public void subscribe(@NonNull Observer<? super DownloadBena> observer) {
                                Lg.d("parse media subscribe in");
                                File file = new File(out);
                                if (!file.exists()) {
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
                                final Media media = entry.getKey();
                                final URITemp uri = entry.getValue();
                                final String origin = uri.getData().getUri_template().getOrigin();
                                final List<Media.MediaList> media_list = media.getData().getMedia_list();
                                for (final Media.MediaList i : media_list) {
                                    if (i.isDeleted()) {
                                        Lg.w("media %d has been deleted", i.getId());
                                    } else {
                                        Lg.d("send media %s (all: %s) to download", i.getId(), all.incrementAndGet());
                                        observer.onNext(new DownloadBena(file, i, origin));
                                    }
                                }
                            }
                        };
                    }
                }).observeOn(Schedulers.io()).flatMap(new Function<DownloadBena, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(DownloadBena downloadBena) throws Throwable {

                        return Observable.just(downloadBena).subscribeOn(downloadSchedulers)
                                .map(new Function<DownloadBena, String>() {
                                    @Override
                                    public String apply(DownloadBena downloadBena) throws Throwable {
                                        final File file = downloadBena.getFile();
                                        final Media.MediaList i = downloadBena.getMedia();
                                        final String origin = downloadBena.getOrigin();
                                        Lg.d("media_list start: %s", i.getId());
                                        String taken = i.getTaken();
                                        final Date date;
                                        File f = null;
                                        try {
                                            if (taken == null || taken.length() == 0) {
                                                taken = i.getCreated_at();
                                                Lg.w("taken time is null, use Created_at");
                                            }
                                            Lg.d("parse date: %s", taken);

                                            SimpleDateFormat df = threadLocal.get();
                                            if (df == null) {
                                                threadLocal.set(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
                                                df = threadLocal.get();
                                            }

                                            date = df.parse(taken);
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
                                            f = new File(out, dir + "/" + fileName);
                                            final String url = origin.replace("<media_id>",
                                                    String.valueOf(i.getId())) + "?media_token=" + i.getToken();

                                            Lg.d("out file is %s", f.getAbsolutePath());
                                            if (f.exists()) {
                                                Lg.w("f - %s exists", f.getAbsolutePath());
                                                final MessageDigest md5 = MessageDigest.getInstance("md5");
                                                try (FileInputStream fis = new FileInputStream(f)) {
                                                    byte[] buf = new byte[BUF_SIZE];
                                                    int read = 0, count = 0;
                                                    while ((read = fis.read(buf)) != -1) {
                                                        count += read;
                                                        md5.update(buf, 0, read);
                                                    }

                                                    final String md5s = MD5.toHexString(md5.digest());
                                                    if (md5s.equalsIgnoreCase(i.getMd5())) {
                                                        dfs.write(String.format("%s;%s;%s;%s;%s\n", f.getName(), count,
                                                                md5s, i.getId(), i.getSource_path()).getBytes());
                                                        Lg.w("download task: %s md5 match  , skip download", all.get());
                                                        return String.format("%s(md5: %s) of %s(md5: %s) is already downloaded",
                                                                f.getAbsolutePath(), md5s, url, i.getMd5());
                                                    } else {
                                                        // in case same date for different photo
                                                        int index = 0;
                                                        if (count == i.getSize()) {
                                                            if (refs.containsKey(i.getId())) {
                                                                index = refs.get(i.getId()).incrementAndGet();
                                                            } else {
                                                                refs.put(i.getId(), new AtomicInteger(index++));
                                                            }
                                                            f = new File(f.getParent(),
                                                                    f.getName().replace(".", "_" + index + "."));
                                                            Lg.w("will sava as %s", f.getName());
                                                        } else {
                                                            Lg.w("md5 does not match %s, incomplete file; delete it", f.getName());
                                                        }
                                                    }
                                                }
                                            }
                                            retrofit2.Response<ResponseBody> res = evs.download(url).execute();
                                            if (res.code() == 200) {
                                                assert res.body() != null;
                                                final InputStream fis = res.body().byteStream();
                                                try (FileOutputStream fos = new FileOutputStream(f)) {
//                                            bs.getBuffer().copyTo(fos);
//                                            fos.flush();
                                                    byte[] bf = new byte[BUF_SIZE];
                                                    int rd = 0, count = 0;
                                                    Lg.w("downloading <%s/%s> file[%s] start", index.incrementAndGet(),
                                                            all.get(), f.getName());
                                                    while ((rd = fis.read(bf)) != -1) {
//                                                Lg.d("write %s bytes", rd);
                                                        count += rd;
                                                        fos.write(bf, 0, rd);
                                                    }
                                                    fos.flush();
                                                    dfs.write(String.format("%s;%s;%s;%s;%s\n", f.getName(), count,
                                                            i.getMd5(), i.getId(), i.getSource_path()).getBytes());
                                                    Lg.w("download <%s/%s> (%s bytes) file[%s] done",
                                                            index.get(), all.get(), count, f.getName());
                                                }
                                            } else {
                                                Lg.w("download failed <%s/%s>: server : %s - %s",
                                                        index.get(), all.get(), res.code(),
                                                        new Gson().fromJson(Objects.requireNonNull(res.body()).string(),
                                                                Base.class).toString());
                                                errfs.write(String.format("%s;%s;%s;%s;%s\n",
                                                        "server:" + res.code(), f.getName(),
                                                        i.getMd5(), i.getId(), i.getSource_path()).getBytes());
                                            }
                                            return String.format("%s(md5: %s) of %s(md5: %s) download success",
                                                    f.getAbsolutePath(), i.getMd5(), url, i.getMd5());

                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            if (f != null) {
                                                Lg.w("download <%s/%s> df parse error: %s for %s; delete it",
                                                        index.get(), all.get(), taken, f.getAbsolutePath());
                                                f.delete();
                                            } else {
                                                Lg.w("download <%s/%s> df parse error: %s file is null",
                                                        index.get(), all.get(), taken);
                                            }
                                            errfs.write(String.format("%s;%s;%s;%s;%s\n",
                                                    "df parse", f == null ? "null" : f.getName(),
                                                    i.getMd5(), i.getId(), i.getSource_path()).getBytes());
                                            return e.getMessage() == null ? e.getMessage() : "exception throw";
                                        } catch (NoSuchAlgorithmException | IOException e) {
                                            e.printStackTrace();
                                            Lg.w("download <%s/%s> io error when download %s; delete it",
                                                    index.get(), all.get(), f.getAbsolutePath());
                                            f.delete();
                                            errfs.write(String.format("%s;%s;%s;%s;%s\n",
                                                    "io error", f.getName(),
                                                    i.getMd5(), i.getId(), i.getSource_path()).getBytes());
                                            return e.getMessage() == null ? e.getMessage() : "exception throw";
                                        }
                                    }
                                });
                    }
                }).observeOn(Schedulers.io());
    }


    private static class DownloadBena {

        private File file;
        private Media.MediaList media;
        private String origin;

        public DownloadBena(File file, Media.MediaList media, String origin) {
            this.file = file;
            this.media = media;
            this.origin = origin;
        }

        public File getFile() {
            return file;
        }

        public Media.MediaList getMedia() {
            return media;
        }

        public String getOrigin() {
            return origin;
        }
    }
}

