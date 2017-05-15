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

public class ParallelFilterTryTest implements Function1<Object, kotlin.Unit> {

    volatile int calls;

    @Override
    public Unit invoke(Object t) {
        calls++;
        return Unit.INSTANCE;
    }

    @Test
    public void filterNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .filter(Functions.alwaysTrue(), e)
            .sequential()
            .test()
            .assertResult(1);
        }
    }

    @Test
    public void filterFalse() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .filter(Functions.alwaysFalse(), e)
            .sequential()
            .test()
            .assertResult();
        }
    }

    @Test
    public void filterFalseConditional() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .filter(Functions.alwaysFalse(), e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertResult();
        }
    }

    @Test
    public void filterErrorNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .filter(Functions.alwaysTrue(), e)
            .sequential()
            .test()
            .assertFailure(TestException.class);
        }
    }

    @Test
    public void filterConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .filter(Functions.alwaysTrue(), e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertResult(1);
        }
    }
    @Test
    public void filterErrorConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .filter(Functions.alwaysTrue(), e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertFailure(TestException.class);
        }
    }

    @Test
    public void filterFailWithError() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
            }
        }, ParallelFailureHandling.ERROR)
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void filterFailWithStop() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
            }
        }, ParallelFailureHandling.STOP)
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void filterFailWithRetry() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            int count;
            @Override
            public Boolean invoke(Integer v) {
                if (count++ == 1) {
                    return true;
                }
                return 1 / v > 0;
            }
        }, ParallelFailureHandling.RETRY)
        .sequential()
        .test()
        .assertResult(0, 1);
    }

    @Test
    public void filterFailWithRetryLimited() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
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
    public void filterFailWithSkip() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
            }
        }, ParallelFailureHandling.SKIP)
        .sequential()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void filterFailHandlerThrows() {
        TestSubscriber<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
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
    public void filterWrongParallelism() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .filter(Functions.alwaysTrue(), ParallelFailureHandling.ERROR)
        );
    }

    @Test
    public void filterInvalidSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .filter(Functions.alwaysTrue(), ParallelFailureHandling.ERROR)
            .sequential()
            .test();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void filterFailWithErrorConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
            }
        }, ParallelFailureHandling.ERROR)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void filterFailWithStopConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
            }
        }, ParallelFailureHandling.STOP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void filterFailWithRetryConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            int count;
            @Override
            public Boolean invoke(Integer v) {
                if (count++ == 1) {
                    return true;
                }
                return 1 / v > 0;
            }
        }, ParallelFailureHandling.RETRY)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(0, 1);
    }

    @Test
    public void filterFailWithRetryLimitedConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
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
    public void filterFailWithSkipConditional() {
        Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
            }
        }, ParallelFailureHandling.SKIP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void filterFailHandlerThrowsConditional() {
        TestSubscriber<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return 1 / v > 0;
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
    public void filterWrongParallelismConditional() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .filter(Functions.alwaysTrue(), ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
        );
    }

    @Test
    public void filterInvalidSourceConditional() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .filter(Functions.alwaysTrue(), ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
