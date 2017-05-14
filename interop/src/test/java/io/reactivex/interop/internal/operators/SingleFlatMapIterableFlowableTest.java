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
import org.reactivestreams.Subscription;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.utils.CrashingIterable;
import io.reactivex.flowable.subscribers.SubscriberFusion;
import io.reactivex.flowable.subscribers.TestSubscriber;
import io.reactivex.observable.Single;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static io.reactivex.interop.RxJava3Interop.flattenAsFlowable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleFlatMapIterableFlowableTest {

    @Test
    public void normal() {

        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void emptyIterable() {

        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Collections.<Integer>emptyList();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void error() {

        flattenAsFlowable(Single.<Integer>error(new TestException()), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void backpressure() {

        TestSubscriber<Integer> ts = flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .test(0);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void take() {
        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void fused() {
        TestSubscriber<Integer> to = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(to);

        to.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(FusedQueueSubscription.ASYNC))
        .assertResult(1, 2);
        ;
    }

    @Test
    public void fusedNoSync() {
        TestSubscriber<Integer> to = SubscriberFusion.newTest(FusedQueueSubscription.SYNC);

        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(to);

        to.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(FusedQueueSubscription.NONE))
        .assertResult(1, 2);
        ;
    }

    @Test
    public void iteratorCrash() {

        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return new CrashingIterable(1, 100, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNextCrash() {

        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return new CrashingIterable(100, 1, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextCrash() {

        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return new CrashingIterable(100, 100, 1);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void hasNextCrash2() {

        flattenAsFlowable(Single.just(1), new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return new CrashingIterable(100, 2, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void async1() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .hide()
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async2() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async3() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .take(500 * 1000)
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async4() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .observeOn(Schedulers.single())
        .take(500 * 1000)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void fusedEmptyCheck() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        return Arrays.asList(1, 2, 3);
                    }
        }).subscribe(new RelaxedSubscriber<Integer>() {
            FusedQueueSubscription<Integer> qd;
            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Subscription d) {
                qd = (FusedQueueSubscription<Integer>)d;

                assertEquals(FusedQueueSubscription.ASYNC, qd.requestFusion(FusedQueueSubscription.ANY));
            }

            @Override
            public void onNext(Integer value) {
                assertFalse(qd.isEmpty());

                qd.clear();

                assertTrue(qd.isEmpty());

                qd.cancel();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void hasNextThrowsUnbounded() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        return new CrashingIterable(100, 2, 100);
                    }
                })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void nextThrowsUnbounded() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        return new CrashingIterable(100, 100, 1);
                    }
                })
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void hasNextThrows() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        return new CrashingIterable(100, 2, 100);
                    }
                })
        .test(2L)
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void nextThrows() {
        flattenAsFlowable(Single.just(1)
                , new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        return new CrashingIterable(100, 100, 1);
                    }
                })
        .test(2L)
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void requestBefore() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            flattenAsFlowable(ps.singleElement(),
                    new Function1<Integer, Iterable<Integer>>() {
                @Override
                public Iterable<Integer> invoke(Integer v) {
                    return Arrays.asList(1, 2, 3);
                }
            })
            .test(5L)
            .assertEmpty();
        }
    }

    @Test
    public void requestCreateInnerRace() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.onNext(1);

            final TestSubscriber<Integer> ts = flattenAsFlowable(ps.singleElement(),
                    new Function1<Integer, Iterable<Integer>>() {
                @Override
                public Iterable<Integer> invoke(Integer v) {
                    return Arrays.asList(a);
                }
            })
            .test(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                    for (int i = 0; i < 500; i++) {
                        ts.request(1);
                    }
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 500; i++) {
                        ts.request(1);
                    }
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void cancelCreateInnerRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.onNext(1);

            final TestSubscriber<Integer> ts = flattenAsFlowable(ps.singleElement(),
                    new Function1<Integer, Iterable<Integer>>() {
                @Override
                public Iterable<Integer> invoke(Integer v) {
                    return Arrays.asList(1, 2, 3);
                }
            })
            .test(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void slowPathCancelAfterHasNext() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        flattenAsFlowable(Single.just(1)
                , new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return new Iterable<Integer>() {
                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<Integer>() {
                            int count;
                            @Override
                            public boolean hasNext() {
                                if (count++ == 2) {
                                    ts.cancel();
                                }
                                return true;
                            }

                            @Override
                            public Integer next() {
                                return 1;
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        })
        .subscribe(ts);

        ts.request(3);
        ts.assertValues(1, 1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void fastPathCancelAfterHasNext() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        flattenAsFlowable(Single.just(1)
                , new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return new Iterable<Integer>() {
                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<Integer>() {
                            int count;
                            @Override
                            public boolean hasNext() {
                                if (count++ == 2) {
                                    ts.cancel();
                                }
                                return true;
                            }

                            @Override
                            public Integer next() {
                                return 1;
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        })
        .subscribe(ts);

        ts.request(Long.MAX_VALUE);
        ts.assertValues(1, 1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void requestIteratorRace() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestSubscriber<Integer> ts = flattenAsFlowable(ps.singleOrError(), new Function1<Integer, Iterable<Integer>>() {
                @Override
                public Iterable<Integer> invoke(Integer v) {
                    return Arrays.asList(a);
                }
            }).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        ts.request(1);
                    }
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                    ps.onComplete();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }
}
