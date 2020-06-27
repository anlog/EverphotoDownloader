package cc.ifnot.libs.everphoto;

import java.util.concurrent.CountDownLatch;

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

        @NonNull final Observable<Integer> just = Observable.just(1);
        Lg.d(just);
        @NonNull final Observable<Integer> a = just.subscribeOn(Schedulers.io());
        Lg.d(a);
        @NonNull final Observable<Integer> b = a.subscribeOn(Schedulers.computation());
        Lg.d(b);


        @NonNull final Disposable ds = Observable.just(1, 2, 3)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Throwable {
                        Lg.d(integer);
                        return integer;
                    }
                }).subscribeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Throwable {
                        Lg.d("map");
                        return integer * integer;
                    }
                }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.newThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Throwable {
                        Lg.d("==" + integer);
                    }
                });

        while (!ds.isDisposed()) {}

        Lg.d();

        if (true)return;

        CountDownLatch latch = new CountDownLatch(1);
        @NonNull final Disposable disposable = Observable.just(1, 3)
                .subscribeOn(Schedulers.single())
                .flatMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer integer) throws Throwable {
                        return new ObservableSource<Integer>() {
                            @Override
                            public void subscribe(@NonNull Observer<? super Integer> observer) {
                                Lg.d("flatMap = %s", integer);
                                observer.onNext(integer + 100);
                                observer.onNext(integer + 200);
                                observer.onComplete();
                            }
                        };
                    }
                }, false, Integer.MAX_VALUE)
                .flatMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer integer) throws Throwable {
                        return Observable.just(integer).subscribeOn(Schedulers.computation())
                                .map(new Function<Integer, Integer>() {
                                    @Override
                                    public Integer apply(Integer integer) throws Throwable {
                                        Lg.d("map = %s", integer);
                                        return integer * integer;
                                    }
                                });
                    }
                })
                .flatMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer integer) throws Throwable {
                        return new ObservableSource<Integer>() {
                            @Override
                            public void subscribe(@NonNull Observer<? super Integer> observer) {
                                observer.onComplete();
                            }
                        };
                    }
                })
                .observeOn(Schedulers.newThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Throwable {
                        Lg.d("onNext: %s", integer);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        Lg.d("onErr");
                    }
                }, new Action() {
                    @Override
                    public void run() throws Throwable {
                        Lg.d("onComplete");
                        latch.countDown();
                    }
                });
        while (!disposable.isDisposed()) {
            try {
                latch.await();
                Lg.d("wait");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (true) return;


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
