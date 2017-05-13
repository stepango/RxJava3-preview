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

package io.reactivex.observable.internal.operators;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.common.Emitter;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.TestHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static org.junit.Assert.assertEquals;

public class ObservableGenerateTest {

    @Test
    public void statefulBiconsumer() {
        Observable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 10;
            }
        }, new Function2<Object, Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Object s, Emitter<Object> e) {
                e.onNext(s);
                return Unit.INSTANCE;
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
        Observable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        }, new Function2<Object, Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Object s, Emitter<Object> e) {
                e.onNext(s);
                return Unit.INSTANCE;
            }
        }, Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void generatorThrows() {
        Observable.generate(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function2<Object, Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Object s, Emitter<Object> e) {
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
            Observable.generate(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function2<Object, Emitter<Object>, kotlin.Unit>() {
                @Override
                public Unit invoke(Object s, Emitter<Object> e) {
                    e.onComplete();
                    return Unit.INSTANCE;
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
        TestHelper.checkDisposed(Observable.generate(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
        }, new Function2<Object, Emitter<Object>, kotlin.Unit>() {
                @Override
                public Unit invoke(Object s, Emitter<Object> e) {
                    e.onComplete();
                    return Unit.INSTANCE;
                }
            }, Functions.emptyConsumer()));
    }

    @Test
    public void nullError() {
        final int[] call = { 0 };
        Observable.generate(Functions.justCallable(1),
                new Function2<Integer, Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer s, Emitter<Object> e) {
                try {
                    e.onError(null);
                } catch (NullPointerException ex) {
                    call[0]++;
                }
                return Unit.INSTANCE;
            }
        }, Functions.emptyConsumer())
        .test()
        .assertFailure(NullPointerException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void multipleOnNext() {
        Observable.generate(new Function1<Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Emitter<Object> e) {
                e.onNext(1);
                e.onNext(2);
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertFailure(IllegalStateException.class, 1);
    }

    @Test
    public void multipleOnError() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Observable.generate(new Function1<Emitter<Object>, kotlin.Unit>() {
                @Override
                public Unit invoke(Emitter<Object> e) {
                    e.onError(new TestException("First"));
                    e.onError(new TestException("Second"));
                    return Unit.INSTANCE;
                }
            })
            .test()
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void multipleOnComplete() {
        Observable.generate(new Function1<Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Emitter<Object> e) {
                e.onComplete();
                e.onComplete();
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertResult();
    }
}
