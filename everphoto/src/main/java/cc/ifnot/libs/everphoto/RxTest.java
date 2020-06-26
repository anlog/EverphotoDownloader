package cc.ifnot.libs.everphoto;

import cc.ifnot.libs.utils.Lg;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * author: dp
 * created on: 2020/6/25 9:33 PM
 * description:
 */
public class RxTest {

    public static void main(String[] args) {
        Lg.level(Lg.MORE);
        Lg.d("in");

        @NonNull final Disposable dis = Observable.just(2)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Throwable {
                        Lg.d("map");
                        return integer * integer;
                    }
                }).flatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(Integer integer) throws Throwable {
                        Lg.d("flatMap");
                        Lg.d("flatMap for");
                        final ObservableSource<String> stringObservableSource;
                        stringObservableSource = new ObservableSource<String>() {
                            @Override
                            public void subscribe(@NonNull Observer<? super String> observer) {
                                for (int i = 0; i < integer; i++) {
                                    observer.onNext(String.valueOf(i));
                                    Lg.d("send complete");
                                    observer.onComplete();
                                }
                            }
                        };
                        return stringObservableSource;
                    }
                }).flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String s) throws Throwable {
                        Lg.d("%s ---new", s);
                        return new ObservableSource<String>() {
                            @Override
                            public void subscribe(@NonNull Observer<? super String> observer) {
                                observer.onNext("1");
                                observer.onNext(s);
                                Lg.d("%s ---new", s);
                                observer.onComplete();
                            }
                        };
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Throwable {
                        Lg.d("get -- %s", s);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        Lg.d(throwable);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Throwable {
                        Lg.d("receive complete");
                    }
                });
        while (!dis.isDisposed()) {
            Lg.d("isDisposed: %s", dis.isDisposed());
        }
        Lg.d("isDisposed: %s", dis.isDisposed());
    }
}
