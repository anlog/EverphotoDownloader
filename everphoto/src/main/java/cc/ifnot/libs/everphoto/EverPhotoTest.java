package cc.ifnot.libs.everphoto;

import java.util.UUID;

import cc.ifnot.libs.everphoto.bean.res.User;
import cc.ifnot.libs.utils.Lg;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * author: dp
 * created on: 2020/6/25 3:25 PM
 * description:
 */
public class EverPhotoTest {

    private static EverPhotoService evs;

    public static void main(String[] args) {

        EverPhoto.INSTANCE.setMobile("17610677575");
        EverPhoto.INSTANCE.setOut("out");
        EverPhoto.INSTANCE.setPassword("lee00.123");
        EverPhoto.INSTANCE.setVerbose(true);

        EverPhoto.INSTANCE.doDownload();
//
        Lg.d(UUID.randomUUID().toString());
        if (true) return;

        OkHttpClient client = new OkHttpClient.Builder()
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
        @NonNull final Disposable disposable = evs.login("aa", "bb")
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        Lg.d(throwable);
                    }
                })
                .subscribe(new Consumer<User>() {
                    @Override
                    public void accept(User user) throws Throwable {
                        Lg.d("user: %s", user.toString());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        Lg.d("error");
                    }
                });

        while (!disposable.isDisposed()) {

        }

    }
}
