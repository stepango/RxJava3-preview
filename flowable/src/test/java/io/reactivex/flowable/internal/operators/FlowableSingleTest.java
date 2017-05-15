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

package io.reactivex.flowable.internal.operators;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.RxJavaCommonPlugins;
import kotlin.jvm.functions.Function2;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.subscribers.DefaultSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

public class FlowableSingleTest {

    @Test
    public void testSingleFlowable() {
        Flowable<Integer> observable = Flowable.just(1).singleElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElementsFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2).singleElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmptyFlowable() {
        Flowable<Integer> observable = Flowable.<Integer> empty().singleElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsIf1Then2RequestedFlowable() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable.just(1)
        //
                .doOnRequest(new Function1<Long, Unit>() {
                    @Override
                    public Unit invoke(Long n) {
                        requests.add(n);
                        return Unit.INSTANCE;
                    }
                })
                //
                .singleElement()
                //

                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(2);
                    }
                });
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsIf3RequestedFlowable() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable.just(1)
        //
                .doOnRequest(new Function1<Long, Unit>() {
                    @Override
                    public Unit invoke(Long n) {
                        requests.add(n);
                        return Unit.INSTANCE;
                    }
                })
                //
                .singleElement()
                //

                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
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
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }

    @Test
    public void testSingleRequestsExactlyWhatItNeedsIf1RequestedFlowable() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable.just(1)
        //
                .doOnRequest(new Function1<Long, Unit>() {
                    @Override
                    public Unit invoke(Long n) {
                        requests.add(n);
                        return Unit.INSTANCE;
                    }
                })
                //
                .singleElement()
                //

                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
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
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }


    @Test
    public void testSingleWithPredicateFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElementsFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3, 4)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmptyFlowable() {
        Flowable<Integer> observable = Flowable.just(1)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();
        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultFlowable() {
        Flowable<Integer> observable = Flowable.just(1).single(2);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElementsFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2).single(3);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmptyFlowable() {
        Flowable<Integer> observable = Flowable.<Integer> empty()
                .single(1);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElementsFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3, 4)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmptyFlowable() {
        Flowable<Integer> observable = Flowable.just(1)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithBackpressureFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2).singleElement();

        Subscriber<Integer> subscriber = spy(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });
        observable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitItem() {
        final AtomicLong request = new AtomicLong();
        Flowable.just(1).doOnRequest(new Function1<Long, Unit>() {
            @Override
            public Unit invoke(Long n) {
                request.addAndGet(n);
                return Unit.INSTANCE;
            }
        }).blockingSingle();
        // FIXME single now triggers fast-path
        assertEquals(Long.MAX_VALUE, request.get());
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitErrorFromEmpty() {
        final AtomicLong request = new AtomicLong();
        try {
            Flowable.empty().doOnRequest(new Function1<Long, Unit>() {
                @Override
                public Unit invoke(Long n) {
                    request.addAndGet(n);
                    return Unit.INSTANCE;
                }
            }).blockingSingle();
        } catch (NoSuchElementException e) {
            // FIXME single now triggers fast-path
            assertEquals(Long.MAX_VALUE, request.get());
        }
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitErrorFromMoreThanOne() {
        final AtomicLong request = new AtomicLong();
        try {
            Flowable.just(1, 2).doOnRequest(new Function1<Long, Unit>() {
                @Override
                public Unit invoke(Long n) {
                    request.addAndGet(n);
                    return Unit.INSTANCE;
                }
            }).blockingSingle();
        } catch (IllegalArgumentException e) {
            // FIXME single now triggers fast-path
            assertEquals(Long.MAX_VALUE, request.get());
        }
    }

    @Test
    public void singleOrErrorNoElement() {
        Flowable.empty()
            .singleOrError()
            .test()
            .assertNoValues()
            .assertError(NoSuchElementException.class);
    }

    @Test
    public void singleOrErrorOneElement() {
        Flowable.just(1)
            .singleOrError()
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void singleOrErrorMultipleElements() {
        Flowable.just(1, 2, 3)
            .singleOrError()
            .test()
            .assertNoValues()
            .assertError(IllegalArgumentException.class);
    }

    @Test
    public void singleOrErrorError() {
        Flowable.error(new RuntimeException("error"))
            .singleOrError()
            .test()
            .assertNoValues()
            .assertErrorMessage("error")
            .assertError(RuntimeException.class);
    }

    @Test(timeout = 30000)
    public void testIssue1527Flowable() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Flowable<Integer> reduced = source.reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer i1, Integer i2) {
                return i1 + i2;
            }
        });

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
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

            Flowable.unsafeCreate(new Publisher<Integer>() {
                @Override public void subscribe(final Subscriber<? super Integer> observer) {
                    observer.onComplete();
                    observer.onError(exception);
                }
            }).singleElement().test().assertComplete();

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
                return o.singleOrError();
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Object>, Object>() {
            @Override
            public Object invoke(Flowable<Object> o) {
                return o.singleElement();
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Object>, Object>() {
            @Override
            public Object invoke(Flowable<Object> o) {
                return o.singleOrError();
            }
        }, false, 1, 1, 1);
    }
}
