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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.List;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.flowable.BackpressureStrategy;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.FlowableEmitter;
import io.reactivex.flowable.FlowableOnSubscribe;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FlowableCreateTest {

    @Test(expected = NullPointerException.class)
    public void sourceNull() {
        Flowable.create(null, BackpressureStrategy.BUFFER);
    }

    @Test(expected = NullPointerException.class)
    public void modeNull() {
        Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> s) throws Exception { }
        }, null);
    }

    @Test
    public void basic() {
        final Disposable d = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
                e.onError(new TestException());
                e.onNext(4);
                e.onError(new TestException());
                e.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
        .test()
        .assertResult(1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithCancellable() {
        final Disposable d1 = Disposables.empty();
        final Disposable d2 = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e.setDisposable(d1);
                e.setCancellable(new Function0() {
                    @Override
                    public Unit invoke() {
                        d2.dispose();
                        return Unit.INSTANCE;
                    }
                });

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
                e.onError(new TestException());
                e.onNext(4);
                e.onError(new TestException());
                e.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
        .test()
        .assertResult(1, 2, 3);

        assertTrue(d1.isDisposed());
        assertTrue(d2.isDisposed());
    }

    @Test
    public void basicWithError() {
        final Disposable d = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onError(new TestException());
                e.onComplete();
                e.onNext(4);
                e.onError(new TestException());
            }
        }, BackpressureStrategy.BUFFER)
        .test()
        .assertFailure(TestException.class, 1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicSerialized() {
        final Disposable d = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();

                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
                e.onError(new TestException());
                e.onNext(4);
                e.onError(new TestException());
                e.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
        .test()
        .assertResult(1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithErrorSerialized() {
        final Disposable d = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();

                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onError(new TestException());
                e.onComplete();
                e.onNext(4);
                e.onError(new TestException());
            }
        }, BackpressureStrategy.BUFFER)
        .test()
        .assertFailure(TestException.class, 1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void wrap() {
        Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> observer) {
                observer.onSubscribe(new BooleanSubscription());
                observer.onNext(1);
                observer.onNext(2);
                observer.onNext(3);
                observer.onNext(4);
                observer.onNext(5);
                observer.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void unsafe() {
        Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> observer) {
                observer.onSubscribe(new BooleanSubscription());
                observer.onNext(1);
                observer.onNext(2);
                observer.onNext(3);
                observer.onNext(4);
                observer.onNext(5);
                observer.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsafeWithFlowable() {
        Flowable.unsafeCreate(Flowable.just(1));
    }

    @Test
    public void createNullValueBuffer() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.BUFFER)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueLatest() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.LATEST)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueError() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.ERROR)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueDrop() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.DROP)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueMissing() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.MISSING)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueBufferSerialized() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.BUFFER)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueLatestSerialized() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.LATEST)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueErrorSerialized() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.ERROR)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueDropSerialized() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.DROP)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void createNullValueMissingSerialized() {
        final Throwable[] error = { null };

        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();
                try {
                    e.onNext(null);
                    e.onNext(1);
                    e.onError(new TestException());
                    e.onComplete();
                } catch (Throwable ex) {
                    error[0] = ex;
                }
            }
        }, BackpressureStrategy.MISSING)
        .test()
        .assertFailure(NullPointerException.class);

        assertNull(error[0]);
    }

    @Test
    public void onErrorRace() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable<Object> source = Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    final FlowableEmitter<Object> f = e.serialize();

                    final TestException ex = new TestException();

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            f.onError(null);
                        }
                    };

                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            f.onError(ex);
                        }
                    };

                    TestCommonHelper.race(r1, r2, Schedulers.single());
                }
            }, m);

            List<Throwable> errors = TestCommonHelper.trackPluginErrors();

            try {
                for (int i = 0; i < 500; i++) {
                    source
                    .test()
                    .assertFailure(Throwable.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
            assertFalse(errors.isEmpty());
        }
    }

    @Test
    public void onCompleteRace() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable<Object> source = Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    final FlowableEmitter<Object> f = e.serialize();

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            f.onComplete();
                        }
                    };

                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            f.onComplete();
                        }
                    };

                    TestCommonHelper.race(r1, r2, Schedulers.single());
                }
            }, m);

            for (int i = 0; i < 500; i++) {
                source
                .test()
                .assertResult();
            }
        }
    }

    @Test
    public void nullValue() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    e.onNext(null);
                }
            }, m)
            .test()
            .assertFailure(NullPointerException.class);
        }
    }

    @Test
    public void nullThrowable() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            System.out.println(m);
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    e.onError(null);
                }
            }, m)
            .test()
            .assertFailure(NullPointerException.class);
        }
    }

    @Test
    public void serializedConcurrentOnNextOnError() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    final FlowableEmitter<Object> f = e.serialize();

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 1000; i++) {
                                f.onNext(1);
                            }
                        }
                    };

                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 100; i++) {
                                f.onNext(1);
                            }
                            f.onError(new TestException());
                        }
                    };

                    TestCommonHelper.race(r1, r2, Schedulers.single());
                }
            }, m)
            .test()
            .assertSubscribed().assertNotComplete()
            .assertError(TestException.class);
        }
    }

    @Test
    public void callbackThrows() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    throw new TestException();
                }
            }, m)
            .test()
            .assertFailure(TestException.class);
        }
    }

    @Test
    public void nullValueSync() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    e.serialize().onNext(null);
                }
            }, m)
            .test()
            .assertFailure(NullPointerException.class);
        }
    }

    @Test
    public void createNullValue() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            final Throwable[] error = { null };

            Flowable.create(new FlowableOnSubscribe<Integer>() {
                @Override
                public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                    try {
                        e.onNext(null);
                        e.onNext(1);
                        e.onError(new TestException());
                        e.onComplete();
                    } catch (Throwable ex) {
                        error[0] = ex;
                    }
                }
            }, m)
            .test()
            .assertFailure(NullPointerException.class);

            assertNull(error[0]);
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullArgument() {
        Flowable.create(null, BackpressureStrategy.MISSING);
    }

    @Test
    public void onErrorCrash() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    Disposable d = Disposables.empty();
                    e.setDisposable(d);
                    try {
                        e.onError(new IOException());
                        fail("Should have thrown");
                    } catch (TestException ex) {
                        // expected
                    }
                    assertTrue(d.isDisposed());
                }
            }, m)
            .subscribe(new RelaxedSubscriber<Object>() {
                @Override
                public void onSubscribe(Subscription d) {
                }

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
        }
    }

    @Test
    public void onCompleteCrash() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    Disposable d = Disposables.empty();
                    e.setDisposable(d);
                    try {
                        e.onComplete();
                        fail("Should have thrown");
                    } catch (TestException ex) {
                        // expected
                    }
                    assertTrue(d.isDisposed());
                }
            }, m)
            .subscribe(new RelaxedSubscriber<Object>() {
                @Override
                public void onSubscribe(Subscription d) {
                }

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
        }
    }

    @Test
    public void createNullValueSerialized() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            final Throwable[] error = { null };

            Flowable.create(new FlowableOnSubscribe<Integer>() {
                @Override
                public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                    e = e.serialize();
                    try {
                        e.onNext(null);
                        e.onNext(1);
                        e.onError(new TestException());
                        e.onComplete();
                    } catch (Throwable ex) {
                        error[0] = ex;
                    }
                }
            }, m)
            .test()
            .assertFailure(NullPointerException.class);

            assertNull(error[0]);
        }
    }

    @Test
    public void nullThrowableSync() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    e.serialize().onError(null);
                }
            }, m)
            .test()
            .assertFailure(NullPointerException.class);
        }
    }

    @Test
    public void serializedConcurrentOnNext() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    final FlowableEmitter<Object> f = e.serialize();

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 1000; i++) {
                                f.onNext(1);
                            }
                        }
                    };

                    TestCommonHelper.race(r1, r1, Schedulers.single());
                }
            }, m)
            .take(1000)
            .test()
            .assertSubscribed().assertValueCount(1000).assertComplete().assertNoErrors();
        }
    }

    @Test
    public void serializedConcurrentOnNextOnComplete() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            TestSubscriber<Object> to = Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> e) throws Exception {
                    final FlowableEmitter<Object> f = e.serialize();

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 1000; i++) {
                                f.onNext(1);
                            }
                        }
                    };

                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 100; i++) {
                                f.onNext(1);
                            }
                            f.onComplete();
                        }
                    };

                    TestCommonHelper.race(r1, r2, Schedulers.single());
                }
            }, m)
            .test()
            .assertSubscribed().assertComplete()
            .assertNoErrors();

            int c = to.valueCount();
            assertTrue("" + c, c >= 100);
        }
    }

    @Test
    public void serialized() {
        for (BackpressureStrategy m : BackpressureStrategy.values()) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();
            try {
                Flowable.create(new FlowableOnSubscribe<Object>() {
                    @Override
                    public void subscribe(FlowableEmitter<Object> e) throws Exception {
                        FlowableEmitter<Object> f = e.serialize();

                        assertSame(f, f.serialize());

                        assertFalse(f.isCancelled());

                        final int[] calls = { 0 };

                        f.setCancellable(new Function0() {
                            @Override
                            public Unit invoke() {
                                calls[0]++;
                                return Unit.INSTANCE;
                            }
                        });

                        e.onComplete();

                        assertTrue(f.isCancelled());

                        assertEquals(1, calls[0]);
                    }
                }, m)
                .test()
                .assertResult();

                assertTrue(errors.toString(), errors.isEmpty());
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

}
