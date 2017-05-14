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

import java.util.List;

import io.reactivex.common.Disposables;
import io.reactivex.common.Notification;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import kotlin.jvm.functions.Function1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableDematerializeTest {

    @Test
    public void testDematerialize1() {
        Observable<Notification<Integer>> notifications = Observable.just(1, 2).materialize();
        Observable<Integer> dematerialize = notifications.dematerialize();

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDematerialize2() {
        Throwable exception = new Throwable("test");
        Observable<Integer> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.materialize().dematerialize();

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testDematerialize3() {
        Exception exception = new Exception("test");
        Observable<Integer> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.materialize().dematerialize();

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testErrorPassThru() {
        Exception exception = new Exception("test");
        Observable<Integer> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.dematerialize();

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testCompletePassThru() {
        Observable<Integer> o = Observable.empty();
        Observable<Integer> dematerialize = o.dematerialize();

        Observer<Integer> observer = TestHelper.mockObserver();

        TestObserver<Integer> ts = new TestObserver<Integer>(observer);
        dematerialize.subscribe(ts);

        System.out.println(ts.errors());

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testHonorsContractWhenCompleted() {
        Observable<Integer> source = Observable.just(1);

        Observable<Integer> result = source.materialize().dematerialize();

        Observer<Integer> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testHonorsContractWhenThrows() {
        Observable<Integer> source = Observable.error(new TestException());

        Observable<Integer> result = source.materialize().dematerialize();

        Observer<Integer> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(Notification.createOnComplete()).dematerialize());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Object> o) {
                return o.dematerialize();
            }
        });
    }

    @Test
    public void eventsAfterDematerializedTerminal() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Object>() {
                @Override
                protected void subscribeActual(Observer<? super Object> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onNext(Notification.createOnComplete());
                    observer.onNext(Notification.createOnNext(1));
                    observer.onNext(Notification.createOnError(new TestException("First")));
                    observer.onError(new TestException("Second"));
                }
            }
            .dematerialize()
            .test()
            .assertResult();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "First");
            TestCommonHelper.assertUndeliverable(errors, 1, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
