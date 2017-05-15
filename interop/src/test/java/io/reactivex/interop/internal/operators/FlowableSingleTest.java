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

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.RxJavaCommonPlugins;
import kotlin.jvm.functions.Function2;
import io.reactivex.flowable.Flowable;
import io.reactivex.interop.TestHelper;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static io.reactivex.interop.RxJava3Interop.reduce;
import static io.reactivex.interop.RxJava3Interop.single;
import static io.reactivex.interop.RxJava3Interop.singleElement;
import static io.reactivex.interop.RxJava3Interop.singleOrError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class FlowableSingleTest {

    @Test
    public void testSingle() {
        Maybe<Integer> observable = singleElement(Flowable.just(1));

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElements() {
        Maybe<Integer> observable = singleElement(Flowable.just(1, 2));

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmpty() {
        Maybe<Integer> observable = singleElement(Flowable.<Integer> empty());

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicate() {
        Maybe<Integer> observable = singleElement(Flowable.just(1, 2)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                );

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElements() {
        Maybe<Integer> observable = singleElement(Flowable.just(1, 2, 3, 4)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                );

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmpty() {
        Maybe<Integer> observable = singleElement(Flowable.just(1)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                );

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefault() {
        Single<Integer> observable = single(Flowable.just(1), 2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElements() {
        Single<Integer> observable = single(Flowable.just(1, 2), 3);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmpty() {
        Single<Integer> observable = single(Flowable.<Integer> empty()
                , 1);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicate() {
        Single<Integer> observable = single(Flowable.just(1, 2)
                        .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                , 4);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElements() {
        Single<Integer> observable = single(Flowable.just(1, 2, 3, 4)
                        .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                , 6);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmpty() {
        Single<Integer> observable = single(Flowable.just(1)
                        .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                , 2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 30000)
    public void testIssue1527() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Maybe<Integer> reduced = reduce(source, new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer i1, Integer i2) {
                return i1 + i2;
            }
        });

        Integer r = reduced.blockingGet();
        assertEquals(21, r.intValue());
    }

    @Test
    public void singleOrErrorNoElement() {
        singleOrError(Flowable.empty()
            )
            .test()
            .assertNoValues()
            .assertError(NoSuchElementException.class);
    }

    @Test
    public void singleOrErrorOneElement() {
        singleOrError(Flowable.just(1)
            )
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void singleOrErrorMultipleElements() {
        singleOrError(Flowable.just(1, 2, 3)
            )
            .test()
            .assertNoValues()
            .assertError(IllegalArgumentException.class);
    }

    @Test
    public void singleOrErrorError() {
        singleOrError(Flowable.error(new RuntimeException("error"))
            )
            .test()
            .assertNoValues()
            .assertErrorMessage("error")
            .assertError(RuntimeException.class);
    }

    @Test
    public void singleElementOperatorDoNotSwallowExceptionWhenDone() {
        final Throwable exception = new RuntimeException("some error");
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        try {
            RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
                @Override
                public Unit invoke(final Throwable throwable) {
                    error.set(throwable);
                    return Unit.INSTANCE;
                }
            });

            singleElement(Flowable.unsafeCreate(new Publisher<Integer>() {
                @Override public void subscribe(final Subscriber<? super Integer> observer) {
                    observer.onComplete();
                    observer.onError(exception);
                }
            })).test().assertComplete();

            assertSame(exception, error.get().getCause());
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Object>, Object>() {
            @Override
            public Object invoke(Flowable<Object> o) {
                return singleElement(o);
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Object>, Object>() {
            @Override
            public Object invoke(Flowable<Object> o) {
                return singleElement(o);
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Object>, Object>() {
            @Override
            public Object invoke(Flowable<Object> o) {
                return singleOrError(o);
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowableToSingle(new Function1<Flowable<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> invoke(Flowable<Object> o) {
                return singleOrError(o);
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowableToMaybe(new Function1<Flowable<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> invoke(Flowable<Object> o) {
                return singleElement(o);
            }
        });
    }
}
