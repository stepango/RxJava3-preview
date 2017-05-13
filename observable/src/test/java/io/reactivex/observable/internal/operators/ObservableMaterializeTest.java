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
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import io.reactivex.common.Disposables;
import io.reactivex.common.Notification;
import io.reactivex.common.functions.Function;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.DefaultObserver;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObservableMaterializeTest {

    @Test
    public void testMaterialize1() {
        // null will cause onError to be triggered before "three" can be
        // returned
        final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", null,
                "three");

        TestLocalObserver observer = new TestLocalObserver();
        Observable<Notification<String>> m = Observable.unsafeCreate(o1).materialize();
        m.subscribe(observer);

        try {
            o1.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertFalse(observer.onError);
        assertTrue(observer.onComplete);
        assertEquals(3, observer.notifications.size());

        assertTrue(observer.notifications.get(0).isOnNext());
        assertEquals("one", observer.notifications.get(0).getValue());

        assertTrue(observer.notifications.get(1).isOnNext());
        assertEquals("two", observer.notifications.get(1).getValue());

        assertTrue(observer.notifications.get(2).isOnError());
        assertEquals(NullPointerException.class, observer.notifications.get(2).getError().getClass());
    }

    @Test
    public void testMaterialize2() {
        final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", "three");

        TestLocalObserver observer = new TestLocalObserver();
        Observable<Notification<String>> m = Observable.unsafeCreate(o1).materialize();
        m.subscribe(observer);

        try {
            o1.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertFalse(observer.onError);
        assertTrue(observer.onComplete);
        assertEquals(4, observer.notifications.size());
        assertTrue(observer.notifications.get(0).isOnNext());
        assertEquals("one", observer.notifications.get(0).getValue());

        assertTrue(observer.notifications.get(1).isOnNext());
        assertEquals("two", observer.notifications.get(1).getValue());

        assertTrue(observer.notifications.get(2).isOnNext());
        assertEquals("three", observer.notifications.get(2).getValue());

        assertTrue(observer.notifications.get(3).isOnComplete());
    }

    @Test
    public void testMultipleSubscribes() throws InterruptedException, ExecutionException {
        final TestAsyncErrorObservable o = new TestAsyncErrorObservable("one", "two", null, "three");

        Observable<Notification<String>> m = Observable.unsafeCreate(o).materialize();

        assertEquals(3, m.toList().toFuture().get().size());
        assertEquals(3, m.toList().toFuture().get().size());
    }

    @Test
    public void testWithCompletionCausingError() {
        TestObserver<Notification<Integer>> ts = new TestObserver<Notification<Integer>>();
        final RuntimeException ex = new RuntimeException("boo");
        Observable.<Integer>empty().materialize().doOnNext(new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object t) {
                throw ex;
            }
        }).subscribe(ts);
        ts.assertError(ex);
        ts.assertNoValues();
        ts.assertTerminated();
    }

    private static class TestLocalObserver extends DefaultObserver<Notification<String>> {

        boolean onComplete;
        boolean onError;
        List<Notification<String>> notifications = new Vector<Notification<String>>();

        @Override
        public void onComplete() {
            this.onComplete = true;
        }

        @Override
        public void onError(Throwable e) {
            this.onError = true;
        }

        @Override
        public void onNext(Notification<String> value) {
            this.notifications.add(value);
        }

    }

    private static class TestAsyncErrorObservable implements ObservableSource<String> {

        String[] valuesToReturn;

        TestAsyncErrorObservable(String... values) {
            valuesToReturn = values;
        }

        volatile Thread t;

        @Override
        public void subscribe(final Observer<? super String> observer) {
            observer.onSubscribe(Disposables.empty());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    for (String s : valuesToReturn) {
                        if (s == null) {
                            System.out.println("throwing exception");
                            try {
                                Thread.sleep(100);
                            } catch (Throwable e) {

                            }
                            observer.onError(new NullPointerException());
                            return;
                        } else {
                            observer.onNext(s);
                        }
                    }
                    System.out.println("subscription complete");
                    observer.onComplete();
                }

            });
            t.start();
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).materialize());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Notification<Object>>>() {
            @Override
            public ObservableSource<Notification<Object>> apply(Observable<Object> o) throws Exception {
                return o.materialize();
            }
        });
    }
}
