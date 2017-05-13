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

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.common.Emitter;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.BiConsumer;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;

public class FlowableGenerateTest {

    @Test
    public void statefulBiconsumer() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 10;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void invoke(Object s, Emitter<Object> e) throws Exception {
                e.onNext(s);
            }
        }, new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        })
        .take(5)
        .test()
        .assertResult(10, 10, 10, 10, 10);
    }

    @Test
    public void stateSupplierThrows() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void invoke(Object s, Emitter<Object> e) throws Exception {
                e.onNext(s);
            }
        }, Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void generatorThrows() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void invoke(Object s, Emitter<Object> e) throws Exception {
                throw new TestException();
            }
        }, Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void disposerThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Flowable.generate(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new BiConsumer<Object, Emitter<Object>>() {
                @Override
                public void invoke(Object s, Emitter<Object> e) throws Exception {
                    e.onComplete();
                }
            }, new Function1<Object, kotlin.Unit>() {
                @Override
                public Unit invoke(Object d) {
                    throw new TestException();
                }
            })
            .test()
            .assertResult();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.generate(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new BiConsumer<Object, Emitter<Object>>() {
                @Override
                public void invoke(Object s, Emitter<Object> e) throws Exception {
                    e.onComplete();
                }
            }, Functions.emptyConsumer()));
    }

    @Test
    public void nullError() {
        final int[] call = { 0 };
        Flowable.generate(Functions.justCallable(1),
        new BiConsumer<Integer, Emitter<Object>>() {
            @Override
            public void invoke(Integer s, Emitter<Object> e) throws Exception {
                try {
                    e.onError(null);
                } catch (NullPointerException ex) {
                    call[0]++;
                }
            }
        }, Functions.emptyConsumer())
        .test()
        .assertFailure(NullPointerException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.generate(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new BiConsumer<Object, Emitter<Object>>() {
                @Override
                public void invoke(Object s, Emitter<Object> e) throws Exception {
                    e.onComplete();
                }
            }, Functions.emptyConsumer()));
    }

    @Test
    public void rebatchAndTake() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void invoke(Object s, Emitter<Object> e) throws Exception {
                e.onNext(1);
            }
        }, Functions.emptyConsumer())
        .rebatchRequests(1)
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void backpressure() {
        Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void invoke(Object s, Emitter<Object> e) throws Exception {
                e.onNext(1);
            }
        }, Functions.emptyConsumer())
        .rebatchRequests(1)
        .test(5)
        .assertSubscribed()
        .assertValues(1, 1, 1, 1, 1)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void requestRace() {
        Flowable<Object> source = Flowable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new BiConsumer<Object, Emitter<Object>>() {
            @Override
            public void invoke(Object s, Emitter<Object> e) throws Exception {
                e.onNext(1);
            }
        }, Functions.emptyConsumer());

        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Object> ts = source.test(0L);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 500; j++) {
                        ts.request(1);
                    }
                }
            };

            TestCommonHelper.race(r, r);

            ts.assertValueCount(1000);
        }
    }

    @Test
    public void multipleOnNext() {
        Flowable.generate(new Function1<Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Emitter<Object> e) {
                e.onNext(1);
                e.onNext(2);
                return Unit.INSTANCE;
            }
        })
        .test(1)
        .assertFailure(IllegalStateException.class, 1);
    }

    @Test
    public void multipleOnError() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Flowable.generate(new Function1<Emitter<Object>, kotlin.Unit>() {
                @Override
                public Unit invoke(Emitter<Object> e) {
                    e.onError(new TestException("First"));
                    e.onError(new TestException("Second"));
                    return Unit.INSTANCE;
                }
            })
            .test(1)
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void multipleOnComplete() {
        Flowable.generate(new Function1<Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Emitter<Object> e) {
                e.onComplete();
                e.onComplete();
                return Unit.INSTANCE;
            }
        })
        .test(1)
        .assertResult();
    }
}
