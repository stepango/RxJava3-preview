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
import org.reactivestreams.Subscriber;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;
import io.reactivex.interop.TestHelper;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import kotlin.jvm.functions.Function1;

import static io.reactivex.interop.RxJava3Interop.flatMapSingle;
import static io.reactivex.interop.RxJava3Interop.singleOrError;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FlowableFlatMapSingleTest {

    @Test
    public void normal() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalDelayError() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v);
            }
        }, true, Integer.MAX_VALUE)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalAsync() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v).subscribeOn(Schedulers.computation());
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueSet(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void normalAsyncMaxConcurrency() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v).subscribeOn(Schedulers.computation());
            }
        }, false, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueSet(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .assertNoErrors()
        .assertComplete();
     }

    @Test
    public void normalAsyncMaxConcurrency1() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v).subscribeOn(Schedulers.computation());
            }
        }, false, 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void mapperThrowsFlowable() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to =
                flatMapSingle(ps, new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                throw new TestException();
            }
        })
        .test();

        assertTrue(ps.hasSubscribers());

        ps.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(ps.hasSubscribers());
    }

    @Test
    public void mapperReturnsNullFlowable() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to =
                flatMapSingle(ps, new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return null;
            }
        })
        .test();

        assertTrue(ps.hasSubscribers());

        ps.onNext(1);

        to.assertFailure(NullPointerException.class);

        assertFalse(ps.hasSubscribers());
    }

    @Test
    public void normalDelayErrorAll() {
        TestSubscriber<Integer> to =
        flatMapSingle(Flowable.range(1, 10).concatWith(Flowable.<Integer>error(new TestException())),
                new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.error(new TestException());
            }
        }, true, Integer.MAX_VALUE)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 11; i++) {
            TestCommonHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalBackpressured() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v);
            }
        })
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalMaxConcurrent1Backpressured() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v);
            }
        }, false, 1)
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalMaxConcurrent2Backpressured() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v);
            }
        }, false, 2)
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void takeAsync() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v).subscribeOn(Schedulers.computation());
            }
        })
        .take(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(2)
        .assertValueSet(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void take() {
        flatMapSingle(Flowable.range(1, 10), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(v);
            }
        })
        .take(2)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void middleError() {
        flatMapSingle(Flowable.fromArray(new String[]{"1", "a", "2"}), new Function1<String, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(final String s) {
                //return Single.just(Integer.valueOf(s)); //This works
                return Single.fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws NumberFormatException {
                        return Integer.valueOf(s);
                    }
                });
            }
        })
        .test()
        .assertFailure(NumberFormatException.class, 1);
    }

    @Test
    public void asyncFlatten() {
        flatMapSingle(Flowable.range(1, 1000), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(1).subscribeOn(Schedulers.computation());
            }
        })
        .take(500)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void successError() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to =
                flatMapSingle(Flowable.range(1, 2), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                if (v == 2) {
                    return singleOrError(ps);
                }
                return Single.error(new TestException());
            }
        }, true, Integer.MAX_VALUE)
        .test();

        ps.onNext(1);
        ps.onComplete();

        to
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(
                flatMapSingle(PublishProcessor.<Integer>create(), new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.<Integer>just(1);
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function1<Flowable<Object>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Flowable<Object> f) {
                return flatMapSingle(f, Functions.justFunction(Single.just(2)));
            }
        });
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            flatMapSingle(new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onError(new TestException("First"));
                    observer.onError(new TestException("Second"));
                }
            }, Functions.justFunction(Single.just(2)))
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void badInnerSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            flatMapSingle(Flowable.just(1), Functions.justFunction(new Single<Integer>() {
                @Override
                protected void subscribeActual(SingleObserver<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new TestException("First"));
                    observer.onError(new TestException("Second"));
                }
            }))
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void emissionQueueTrigger() {
        final PublishProcessor<Integer> ps1 = PublishProcessor.create();
        final PublishProcessor<Integer> ps2 = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps2.onNext(2);
                    ps2.onComplete();
                }
            }
        };

        flatMapSingle(Flowable.just(ps1, ps2), new Function1<PublishProcessor<Integer>, SingleSource<Integer>>() {
                    @Override
                    public SingleSource<Integer> invoke(PublishProcessor<Integer> v) {
                        return singleOrError(v);
                    }
                })
        .subscribe(to);

        ps1.onNext(1);
        ps1.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void disposeInner() {
        final TestSubscriber<Object> to = new TestSubscriber<Object>();

        flatMapSingle(Flowable.just(1), new Function1<Integer, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> invoke(Integer v) {
                return new Single<Object>() {
                    @Override
                    protected void subscribeActual(SingleObserver<? super Object> observer) {
                        observer.onSubscribe(Disposables.empty());

                        assertFalse(((Disposable)observer).isDisposed());

                        to.dispose();

                        assertTrue(((Disposable)observer).isDisposed());
                    }
                };
            }
        })
        .subscribe(to);

        to
        .assertEmpty();
    }

    @Test
    public void innerSuccessCompletesAfterMain() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to =
        flatMapSingle(Flowable.just(1), Functions.justFunction(singleOrError(ps)))
        .test();

        ps.onNext(2);
        ps.onComplete();

        to
        .assertResult(2);
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts =
        flatMapSingle(Flowable.just(1), Functions.justFunction(Single.just(2)))
        .test(0L)
        .assertEmpty();

        ts.request(1);
        ts.assertResult(2);
    }

    @Test
    public void error() {
        flatMapSingle(Flowable.just(1), Functions.justFunction(Single.<Integer>error(new TestException())))
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void errorDelayed() {
        flatMapSingle(Flowable.just(1), Functions.justFunction(Single.<Integer>error(new TestException())), true, 16)
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void requestCancelRace() {
        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Integer> to =
            flatMapSingle(Flowable.just(1).concatWith(Flowable.<Integer>never()), Functions.justFunction(Single.just(2))).test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.request(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }
}
