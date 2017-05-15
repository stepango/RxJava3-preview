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

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.extensions.HasUpstreamPublisher;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableReduceTest {
    Subscriber<Object> observer;

    @Before
    public void before() {
        observer = TestHelper.mockSubscriber();
    }

    Function2<Integer, Integer, Integer> sum = new Function2<Integer, Integer, Integer>() {
        @Override
        public Integer invoke(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    @Test
    public void testAggregateAsIntSumFlowable() {

        Flowable<Integer> result = Flowable.just(1, 2, 3, 4, 5).reduce(0, sum)
                .map(new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer v) {
                        return v;
                    }
                });

        result.subscribe(observer);

        verify(observer).onNext(1 + 2 + 3 + 4 + 5);
        verify(observer).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAggregateAsIntSumSourceThrowsFlowable() {
        Flowable<Integer> result = Flowable.concat(Flowable.just(1, 2, 3, 4, 5),
                Flowable.<Integer> error(new TestException()))
                .reduce(0, sum).map(new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer v) {
                        return v;
                    }
                });

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumAccumulatorThrowsFlowable() {
        Function2<Integer, Integer, Integer> sumErr = new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        Flowable<Integer> result = Flowable.just(1, 2, 3, 4, 5)
                .reduce(0, sumErr).map(new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer v) {
                        return v;
                    }
                });

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumResultSelectorThrowsFlowable() {

        Function1<Integer, Integer> error = new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t1) {
                throw new TestException();
            }
        };

        Flowable<Integer> result = Flowable.just(1, 2, 3, 4, 5)
                .reduce(0, sum).map(error);

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testBackpressureWithInitialValueFlowable() throws InterruptedException {
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Flowable<Integer> reduced = source.reduce(0, sum);

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
    }

    @Test
    public void reducerCrashSuppressOnError() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Flowable.<Integer>fromPublisher(new Publisher<Integer>() {
                @Override
                public void subscribe(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(1);
                    s.onError(new TestException("Source"));
                    s.onComplete();
                }
            })
            .reduce(new Function2<Integer, Integer, Integer>() {
                @Override
                public Integer invoke(Integer a, Integer b) {
                    throw new TestException("Reducer");
                }
            })

            .test()
            .assertFailureAndMessage(TestException.class, "Reducer");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Source");
        } finally {
            RxJavaCommonPlugins.reset();
        }

    }

    @Test
    public void cancel() {

        TestSubscriber<Integer> ts = Flowable.just(1)
        .concatWith(Flowable.<Integer>never())
        .reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return a + b;
            }
        })
        .test();

        ts.assertEmpty();

        ts.cancel();

        ts.assertEmpty();

    }

    @Test
    public void testBackpressureWithNoInitialValueObservable() throws InterruptedException {
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Flowable<Integer> reduced = source.reduce(sum);

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
    }

    @Test
    public void source() {
        Flowable<Integer> source = Flowable.just(1);

        assertSame(source, (((HasUpstreamPublisher<?>)source.reduce(sum))).source());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.range(1, 2).reduce(sum));
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new TestException())
        .reduce(sum)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorFlowable() {
        Flowable.<Integer>error(new TestException())
        .reduce(sum)

        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .reduce(sum)
        .test()
        .assertResult();
    }

    @Test
    public void emptyFlowable() {
        Flowable.<Integer>empty()
        .reduce(sum)

        .test()
        .assertResult();
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Integer>, Object>() {
            @Override
            public Object invoke(Flowable<Integer> f) {
                return f.reduce(sum);
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void badSourceFlowable() {
        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Integer>, Object>() {
            @Override
            public Object invoke(Flowable<Integer> f) {
                return f.reduce(sum);
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void reducerThrows() {
        Flowable.just(1, 2)
        .reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    /**
     * https://gist.github.com/jurna/353a2bd8ff83f0b24f0b5bc772077d61
     */
    @Test
    public void shouldReduceTo10Events() {
        final AtomicInteger count = new AtomicInteger();

        Flowable.range(0, 10).flatMap(new Function1<Integer, Publisher<String>>() {
            @Override
            public Publisher<String> invoke(final Integer x) {
                return Flowable.range(0, 2)
                        .map(new Function1<Integer, String>() {
                    @Override
                    public String invoke(Integer y) {
                        return blockingOp(x, y);
                    }
                }).subscribeOn(Schedulers.io())
                .reduce(new Function2<String, String, String>() {
                    @Override
                    public String invoke(String l, String r) {
                        return l + "_" + r;
                    }
                })
                        .doOnNext(new Function1<String, kotlin.Unit>() {
                    @Override
                    public Unit invoke(String s) {
                        count.incrementAndGet();
                        System.out.println("Completed with " + s);
                        return Unit.INSTANCE;
                    }
                })
                ;
            }
        }
        ).blockingLast();

        assertEquals(10, count.get());
    }

    /**
     * https://gist.github.com/jurna/353a2bd8ff83f0b24f0b5bc772077d61
     */
    @Test
    public void shouldReduceTo10EventsFlowable() {
        final AtomicInteger count = new AtomicInteger();

        Flowable.range(0, 10).flatMap(new Function1<Integer, Publisher<String>>() {
            @Override
            public Publisher<String> invoke(final Integer x) {
                return Flowable.range(0, 2)
                        .map(new Function1<Integer, String>() {
                    @Override
                    public String invoke(Integer y) {
                        return blockingOp(x, y);
                    }
                }).subscribeOn(Schedulers.io())
                .reduce(new Function2<String, String, String>() {
                    @Override
                    public String invoke(String l, String r) {
                        return l + "_" + r;
                    }
                })

                        .doOnNext(new Function1<String, kotlin.Unit>() {
                    @Override
                    public Unit invoke(String s) {
                        count.incrementAndGet();
                        System.out.println("Completed with " + s);
                        return Unit.INSTANCE;
                    }
                })
                ;
            }
        }
        ).blockingLast();

        assertEquals(10, count.get());
    }

    static String blockingOp(Integer x, Integer y) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "x" + x + "y" + y;
    }
}
