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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Notification;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestScheduler;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.DisposableSubscriber;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableDelayTest {
    private Subscriber<Long> observer;
    private Subscriber<Long> observer2;

    private TestScheduler scheduler;

    @Before
    public void before() {
        observer = TestHelper.mockSubscriber();
        observer2 = TestHelper.mockSubscriber();

        scheduler = new TestScheduler();
    }

    @Test
    public void testDelay() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Flowable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testLongDelay() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Flowable<Long> delayed = source.delay(5L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(5999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(6000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        scheduler.advanceTimeTo(6999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(7000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        scheduler.advanceTimeTo(7999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(8000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithError() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler)
                .map(new Function1<Long, Long>() {
            @Override
            public Long invoke(Long value) {
                if (value == 1L) {
                    throw new RuntimeException("error!");
                }
                return value;
            }
        });
        Flowable<Long> delayed = source.delay(1L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(1999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();

        scheduler.advanceTimeTo(5000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void testDelayWithMultipleSubscriptions() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Flowable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);
        delayed.subscribe(observer2);

        InOrder inOrder = inOrder(observer);
        InOrder inOrder2 = inOrder(observer2);

        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder2.verify(observer2, times(1)).onNext(0L);

        scheduler.advanceTimeTo(2499L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder2.verify(observer2, times(1)).onNext(1L);

        verify(observer, never()).onComplete();
        verify(observer2, never()).onComplete();

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder2.verify(observer2, times(1)).onNext(2L);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());
        inOrder.verify(observer, times(1)).onComplete();
        inOrder2.verify(observer2, times(1)).onComplete();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelaySubscription() {
        Flowable<Integer> result = Flowable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        inOrder.verify(o, never()).onNext(any());
        inOrder.verify(o, never()).onComplete();

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        inOrder.verify(o, times(1)).onNext(1);
        inOrder.verify(o, times(1)).onNext(2);
        inOrder.verify(o, times(1)).onNext(3);
        inOrder.verify(o, times(1)).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelaySubscriptionCancelBeforeTime() {
        Flowable<Integer> result = Flowable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        TestSubscriber<Object> ts = new TestSubscriber<Object>(o);

        result.subscribe(ts);
        ts.dispose();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithFlowableNormal1() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final List<PublishProcessor<Integer>> delays = new ArrayList<PublishProcessor<Integer>>();
        final int n = 10;
        for (int i = 0; i < n; i++) {
            PublishProcessor<Integer> delay = PublishProcessor.create();
            delays.add(delay);
        }

        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return delays.get(t1);
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
            delays.get(i).onNext(i);
            inOrder.verify(o).onNext(i);
        }
        source.onComplete();

        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithFlowableSingleSend1() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();

        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return delay;
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);
        delay.onNext(2);

        inOrder.verify(o).onNext(1);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithFlowableSourceThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();

        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return delay;
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        source.onError(new TestException());
        delay.onNext(1);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithFlowableDelayFunctionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                throw new TestException();
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithFlowableDelayThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();

        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return delay;
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        delay.onError(new TestException());

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithFlowableSubscriptionNormal() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();
        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return delay;
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delay, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithFlowableSubscriptionFunctionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();
        Callable<Flowable<Integer>> subFunc = new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                throw new TestException();
            }
        };
        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return delay;
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(Flowable.defer(subFunc), delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithFlowableSubscriptionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();
        Callable<Flowable<Integer>> subFunc = new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                return delay;
            }
        };
        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return delay;
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(Flowable.defer(subFunc), delayFunc).subscribe(o);

        source.onNext(1);
        delay.onError(new TestException());

        source.onNext(2);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithFlowableEmptyDelayer() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return Flowable.empty();
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        source.onComplete();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithFlowableSubscriptionRunCompletion() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> sdelay = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();
        Callable<Flowable<Integer>> subFunc = new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                return sdelay;
            }
        };
        Function1<Integer, Flowable<Integer>> delayFunc = new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return delay;
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(Flowable.defer(subFunc), delayFunc).subscribe(o);

        source.onNext(1);
        sdelay.onComplete();

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithFlowableAsTimed() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);

        final Flowable<Long> delayer = Flowable.timer(500L, TimeUnit.MILLISECONDS, scheduler);

        Function1<Long, Flowable<Long>> delayFunc = new Function1<Long, Flowable<Long>>() {
            @Override
            public Flowable<Long> invoke(Long t1) {
                return delayer;
            }
        };

        Flowable<Long> delayed = source.delay(delayFunc);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithFlowableReorder() {
        int n = 3;

        PublishProcessor<Integer> source = PublishProcessor.create();
        final List<PublishProcessor<Integer>> subjects = new ArrayList<PublishProcessor<Integer>>();
        for (int i = 0; i < n; i++) {
            subjects.add(PublishProcessor.<Integer> create());
        }

        Flowable<Integer> result = source.delay(new Function1<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> invoke(Integer t1) {
                return subjects.get(t1);
            }
        });

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
        }
        source.onComplete();

        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o, never()).onComplete();

        for (int i = n - 1; i >= 0; i--) {
            subjects.get(i).onComplete();
            inOrder.verify(o).onNext(i);
        }

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayEmitsEverything() {
        Flowable<Integer> source = Flowable.range(1, 5);
        Flowable<Integer> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed = delayed.doOnEach(new Function1<Notification<Integer>, kotlin.Unit>() {

            @Override
            public Unit invoke(Notification<Integer> t1) {
                System.out.println(t1);
                return Unit.INSTANCE;
            }

        });
        TestSubscriber<Integer> observer = new TestSubscriber<Integer>();
        delayed.subscribe(observer);
        // all will be delivered after 500ms since range does not delay between them
        scheduler.advanceTimeBy(500L, TimeUnit.MILLISECONDS);
        observer.assertValues(1, 2, 3, 4, 5);
    }

    @Test
    public void testBackpressureWithTimedDelay() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, Flowable.bufferSize() * 2)
                .delay(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function1<Integer, Integer>() {

                    int c;

                    @Override
                    public Integer invoke(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
    }

    @Test
    public void testBackpressureWithSubscriptionTimedDelay() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, Flowable.bufferSize() * 2)
                .delaySubscription(100, TimeUnit.MILLISECONDS)
                .delay(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function1<Integer, Integer>() {

                    int c;

                    @Override
                    public Integer invoke(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
    }

    @Test
    public void testBackpressureWithSelectorDelay() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, Flowable.bufferSize() * 2)
                .delay(new Function1<Integer, Flowable<Long>>() {

                    @Override
                    public Flowable<Long> invoke(Integer i) {
                        return Flowable.timer(100, TimeUnit.MILLISECONDS);
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Function1<Integer, Integer>() {

                    int c;

                    @Override
                    public Integer invoke(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
    }

    @Test
    public void testBackpressureWithSelectorDelayAndSubscriptionDelay() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, Flowable.bufferSize() * 2)
                .delay(Flowable.defer(new Callable<Flowable<Long>>() {

                    @Override
                    public Flowable<Long> call() {
                        return Flowable.timer(500, TimeUnit.MILLISECONDS);
                    }
                }), new Function1<Integer, Flowable<Long>>() {

                    @Override
                    public Flowable<Long> invoke(Integer i) {
                        return Flowable.timer(100, TimeUnit.MILLISECONDS);
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Function1<Integer, Integer>() {

                    int c;

                    @Override
                    public Integer invoke(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
    }

    @Test
    public void testErrorRunsBeforeOnNext() {
        TestScheduler test = new TestScheduler();

        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        ps.delay(1, TimeUnit.SECONDS, test).subscribe(ts);

        ps.onNext(1);

        test.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        ps.onError(new TestException());

        test.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void testDelaySupplierSimple() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        Flowable<Integer> source = Flowable.range(1, 5);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.delaySubscription(Flowable.defer(new Callable<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> call() {
                return ps;
            }
        })).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ps.onNext(1);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void testDelaySupplierCompletes() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        Flowable<Integer> source = Flowable.range(1, 5);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.delaySubscription(Flowable.defer(new Callable<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> call() {
                return ps;
            }
        })).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        // FIXME should this complete the source instead of consuming it?
        ps.onComplete();

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void testDelaySupplierErrors() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        Flowable<Integer> source = Flowable.range(1, 5);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.delaySubscription(Flowable.defer(new Callable<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> call() {
                return ps;
            }
        })).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ps.onError(new TestException());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void delayAndTakeUntilNeverSubscribeToSource() {
        PublishProcessor<Integer> delayUntil = PublishProcessor.create();
        PublishProcessor<Integer> interrupt = PublishProcessor.create();
        final AtomicBoolean subscribed = new AtomicBoolean(false);

        Flowable.just(1)
                .doOnSubscribe(new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object o) {
                subscribed.set(true);
                return Unit.INSTANCE;
            }
        })
        .delaySubscription(delayUntil)
        .takeUntil(interrupt)
        .subscribe();

        interrupt.onNext(9000);
        delayUntil.onNext(1);

        Assert.assertFalse(subscribed.get());
    }

    @Test
    public void delayWithTimeDelayError() throws Exception {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .delay(100, TimeUnit.MILLISECONDS, true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void testDelaySubscriptionDisposeBeforeTime() {
        Flowable<Integer> result = Flowable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        TestSubscriber<Object> ts = new TestSubscriber<Object>(o);

        result.subscribe(ts);
        ts.dispose();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testOnErrorCalledOnScheduler() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> thread = new AtomicReference<Thread>();

        Flowable.<String>error(new Exception())
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .doOnError(new Function1<Throwable, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Throwable throwable) {
                        thread.set(Thread.currentThread());
                        latch.countDown();
                        return Unit.INSTANCE;
                    }
                })
                .onErrorResumeNext(Flowable.<String>empty())
                .subscribe();

        latch.await();

        assertNotEquals(Thread.currentThread(), thread.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().delay(1, TimeUnit.SECONDS));

        TestHelper.checkDisposed(PublishProcessor.create().delay(Functions.justFunction(Flowable.never())));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function1<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> invoke(Flowable<Object> o) {
                return o.delay(1, TimeUnit.SECONDS);
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function1<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> invoke(Flowable<Object> o) {
                return o.delay(Functions.justFunction(Flowable.never()));
            }
        });
    }

    @Test
    public void onCompleteFinal() {
        TestScheduler scheduler = new TestScheduler();

        Flowable.empty()
        .delay(1, TimeUnit.MILLISECONDS, scheduler)
        .subscribe(new DisposableSubscriber<Object>() {
            @Override
            public void onNext(Object value) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
                throw new TestException();
            }
        });

        try {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void onErrorFinal() {
        TestScheduler scheduler = new TestScheduler();

        Flowable.error(new TestException())
        .delay(1, TimeUnit.MILLISECONDS, scheduler)
        .subscribe(new DisposableSubscriber<Object>() {
            @Override
            public void onNext(Object value) {
            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }

            @Override
            public void onComplete() {
            }
        });

        try {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }
    }

}
