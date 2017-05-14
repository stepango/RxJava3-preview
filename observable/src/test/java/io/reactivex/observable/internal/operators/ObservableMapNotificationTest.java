/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.observable.internal.operators;


import org.junit.Test;

import java.util.concurrent.Callable;

import io.reactivex.common.Disposables;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.internal.operators.ObservableMapNotification.MapNotificationObserver;
import io.reactivex.observable.observers.TestObserver;
import kotlin.jvm.functions.Function1;

public class ObservableMapNotificationTest {
    @Test
    public void testJust() {
        TestObserver<Object> ts = new TestObserver<Object>();
        Observable.just(1)
        .flatMap(
                new Function1<Integer, Observable<Object>>() {
                    @Override
                    public Observable<Object> invoke(Integer item) {
                        return Observable.just((Object)(item + 1));
                    }
                },
                new Function1<Throwable, Observable<Object>>() {
                    @Override
                    public Observable<Object> invoke(Throwable e) {
                        return Observable.error(e);
                    }
                },
                new Callable<Observable<Object>>() {
                    @Override
                    public Observable<Object> call() {
                        return Observable.never();
                    }
                }
        ).subscribe(ts);

        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValue(2);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(new Observable<Integer>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                MapNotificationObserver mn = new MapNotificationObserver(
                        observer,
                        Functions.justFunction(Observable.just(1)),
                        Functions.justFunction(Observable.just(2)),
                        Functions.justCallable(Observable.just(3))
                );
                mn.onSubscribe(Disposables.empty());
            }
        });
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Observable<Object> o) {
                return o.flatMap(
                        Functions.justFunction(Observable.just(1)),
                        Functions.justFunction(Observable.just(2)),
                        Functions.justCallable(Observable.just(3))
                );
            }
        });
    }
}
