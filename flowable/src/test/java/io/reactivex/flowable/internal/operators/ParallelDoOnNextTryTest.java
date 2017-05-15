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

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.ParallelFailureHandling;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;

public class ParallelDoOnNextTryTest implements Function1<Object, kotlin.Unit> {

    volatile int calls;

    @Override
    public Unit invoke(Object t) {
        calls++;
        return Unit.INSTANCE;
    }

    @Test
    public void doOnNextNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .doOnNext(this, e)
            .sequential()
            .test()
            .assertResult(1);

            assertEquals(calls, 1);
            calls = 0;
        }
    }
    @Test
    public void doOnNextErrorNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .doOnNext(this, e)
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertEquals(calls, 0);
        }
    }

    @Test
    public void doOnNextConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .doOnNext(this, e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertResult(1);

            assertEquals(calls, 1);
            calls = 0;
        }
    }

    @Test
    public void doOnNextErrorConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .doOnNext(this, e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertEquals(calls, 0);
        }
    }

    @Test
    public void doOnNextFailWithError() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, ParallelFailureHandling.ERROR)
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void doOnNextFailWithStop() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, ParallelFailureHandling.STOP)
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void doOnNextFailWithRetry() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            int count;
            @Override
            public Unit invoke(Integer v) {
                if (count++ == 1) {
                    return Unit.INSTANCE;
                }
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, ParallelFailureHandling.RETRY)
        .sequential()
        .test()
        .assertResult(0, 1);
    }

    @Test
    public void doOnNextFailWithRetryLimited() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, new Function2<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling invoke(Long n, Throwable e) {
                return n < 5 ? ParallelFailureHandling.RETRY : ParallelFailureHandling.SKIP;
            }
        })
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void doOnNextFailWithSkip() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, ParallelFailureHandling.SKIP)
        .sequential()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doOnNextFailHandlerThrows() {
        TestSubscriber<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, new Function2<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling invoke(Long n, Throwable e) {
                throw new TestException();
            }
        })
        .sequential()
        .test()
        .assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts, ArithmeticException.class, TestException.class);
    }

    @Test
    public void doOnNextWrongParallelism() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .doOnNext(Functions.emptyConsumer(), ParallelFailureHandling.ERROR)
        );
    }

    @Test
    public void filterInvalidSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .doOnNext(Functions.emptyConsumer(), ParallelFailureHandling.ERROR)
            .sequential()
            .test();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void doOnNextFailWithErrorConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, ParallelFailureHandling.ERROR)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void doOnNextFailWithStopConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, ParallelFailureHandling.STOP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void doOnNextFailWithRetryConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            int count;
            @Override
            public Unit invoke(Integer v) {
                if (count++ == 1) {
                    return Unit.INSTANCE;
                }
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, ParallelFailureHandling.RETRY)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(0, 1);
    }

    @Test
    public void doOnNextFailWithRetryLimitedConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, new Function2<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling invoke(Long n, Throwable e) {
                return n < 5 ? ParallelFailureHandling.RETRY : ParallelFailureHandling.SKIP;
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void doOnNextFailWithSkipConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, ParallelFailureHandling.SKIP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doOnNextFailHandlerThrowsConditional() {
        TestSubscriber<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
                return Unit.INSTANCE;
            }
        }, new Function2<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling invoke(Long n, Throwable e) {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts, ArithmeticException.class, TestException.class);
    }

    @Test
    public void doOnNextWrongParallelismConditional() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .doOnNext(Functions.emptyConsumer(), ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
        );
    }

    @Test
    public void filterInvalidSourceConditional() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .doOnNext(Functions.emptyConsumer(), ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
