package org.voltdb;

import java.util.concurrent.Callable;

import rx.Observable;
import rx.Subscriber;

/**
 * Convenience class for creating Observable
 *
 *
 */
public class ObservableUtils {

    private ObservableUtils() {
    }

    static <T> Observable<T> createObservable(final Callable<T> callable) {
        return Observable.create(new Observable.OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> subscriber) {
                try {
                    subscriber.onNext(callable.call());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}