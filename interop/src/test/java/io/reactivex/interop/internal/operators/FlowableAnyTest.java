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

package io.reactivex.interop.internal.operators;

import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.interop.RxJava3Interop;
import io.reactivex.interop.TestHelper;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Flowable<Integer> w = Flowable.just(1, 2);
        Single<Boolean> observable = RxJava3Interop.any(w, new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Flowable<Integer> w = Flowable.just(1, 2);
        Single<Boolean> observable = RxJava3Interop.isEmpty(w);

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithOneItem() {
        Flowable<Integer> w = Flowable.just(1);
        Single<Boolean> observable = RxJava3Interop.any(w, new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Flowable<Integer> w = Flowable.just(1);
        Single<Boolean> observable = RxJava3Interop.isEmpty(w);

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithEmpty() {
        Flowable<Integer> w = Flowable.empty();
        Single<Boolean> observable = RxJava3Interop.any(w, new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Flowable<Integer> w = Flowable.empty();
        Single<Boolean> observable = RxJava3Interop.isEmpty(w);

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithPredicate1() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Single<Boolean> observable = RxJava3Interop.any(w, new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testExists1() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Single<Boolean> observable = RxJava3Interop.any(w, new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithPredicate2() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Single<Boolean> observable = RxJava3Interop.any(w, new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 1;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Flowable<Integer> w = Flowable.empty();
        Single<Boolean> observable = RxJava3Interop.any(w, new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWithFollowingFirst() {
        Flowable<Integer> o = Flowable.fromArray(1, 3, 5, 6);
        Single<Boolean> anyEven = RxJava3Interop.any(o, new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer i) {
                return i % 2 == 0;
            }
        });

        assertTrue(anyEven.blockingGet());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Flowable<Integer> source =
            RxJava3Interop.flatMapPublisher(RxJava3Interop.isEmpty(Flowable.just(1)), new Function<Boolean, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Boolean t1) {
                    return Flowable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    @Ignore("Single doesn't do backpressure")
    public void testBackpressureIfNoneRequestedNoneShouldBeDelivered() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();

        RxJava3Interop.any(Flowable.just(1), new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t) {
                return true;
            }
        })
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDelivered() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();
        RxJava3Interop.any(Flowable.just(1), new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }).subscribe(ts);

        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValue(true);
    }

    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();
        final IllegalArgumentException ex = new IllegalArgumentException();

        RxJava3Interop.any(Flowable.just("Boo!"), new kotlin.jvm.functions.Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String v) {
                throw ex;
            }
        }).subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(RxJava3Interop.any(Flowable.just(1), Functions.alwaysTrue()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowableToSingle(new Function<Flowable<Object>, Single<Boolean>>() {
            @Override
            public Single<Boolean> apply(Flowable<Object> o) throws Exception {
                return RxJava3Interop.any(o, Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void badSourceSingle() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            RxJava3Interop.any(new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onError(new TestException("First"));

                    observer.onNext(1);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }, Functions.alwaysTrue())
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
