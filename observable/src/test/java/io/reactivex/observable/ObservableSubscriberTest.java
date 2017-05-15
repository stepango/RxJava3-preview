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

package io.reactivex.observable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.observable.observers.DefaultObserver;
import io.reactivex.observable.observers.SafeObserver;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObservableSubscriberTest {
    @Test
    public void testOnStartCalledOnceViaSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).take(2).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void testOnStartCalledOnceViaUnsafeSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).take(2).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void testOnStartCalledOnceViaLift() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).lift(new ObservableOperator<Integer, Integer>() {

            @Override
            public Observer<? super Integer> apply(final Observer<? super Integer> child) {
                return new DefaultObserver<Integer>() {

                    @Override
                    public void onStart() {
                        c.incrementAndGet();
                    }

                    @Override
                    public void onComplete() {
                        child.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Integer t) {
                        child.onNext(t);
                    }

                };
            }

        }).subscribe();

        assertEquals(1, c.get());
    }

    @Test
    public void subscribeConsumerConsumer() {
        final List<Integer> list = new ArrayList<Integer>();

        Observable.just(1).subscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        }, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                list.add(100);
                return Unit.INSTANCE;
            }
        });

        assertEquals(Arrays.asList(1), list);
    }

    @Test
    public void subscribeConsumerConsumerWithError() {
        final List<Integer> list = new ArrayList<Integer>();

        Observable.<Integer>error(new TestException()).subscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        }, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                list.add(100);
                return Unit.INSTANCE;
            }
        });

        assertEquals(Arrays.asList(100), list);
    }

    @Test
    public void methodTestCancelled() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.test(true);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void safeSubscriberAlreadySafe() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.just(1).safeSubscribe(new SafeObserver<Integer>(ts));

        ts.assertResult(1);
    }


    @Test
    public void methodTestNoCancel() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.test(false);

        assertTrue(ps.hasObservers());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void pluginNull() {
        RxJavaObservablePlugins.setOnObservableSubscribe(new Function2<Observable, Observer, Observer>() {
            @Override
            public Observer invoke(Observable a, Observer b) {
                return null;
            }
        });

        try {
            try {

                Observable.just(1).test();
                fail("Should have thrown");
            } catch (NullPointerException ex) {
                assertEquals("Plugin returned null Observer", ex.getMessage());
            }
        } finally {
            RxJavaObservablePlugins.reset();
        }
    }

    static final class BadObservable extends Observable<Integer> {
        @Override
        protected void subscribeActual(Observer<? super Integer> s) {
            throw new IllegalArgumentException();
        }
    }

    @Test
    public void subscribeActualThrows() {
        List<Throwable> list = TestCommonHelper.trackPluginErrors();
        try {
            try {
                new BadObservable().test();
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                if (!(ex.getCause() instanceof IllegalArgumentException)) {
                    fail(ex.toString() + ": Should be NPE(IAE)");
                }
            }

            TestCommonHelper.assertError(list, 0, IllegalArgumentException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

}
