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

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SingleDoOnTest {

    @Test
    public void doOnDispose() {
        final int[] count = { 0 };

        Single.never().doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                count[0]++;
                return Unit.INSTANCE;
            }
        }).test(true);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnError() {
        final Object[] event = { null };

        Single.error(new TestException()).doOnError(new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                event[0] = e;
                return Unit.INSTANCE;
            }
        })
        .test();

        assertTrue(event[0].toString(), event[0] instanceof TestException);
    }

    @Test
    public void doOnSubscribe() {
        final int[] count = { 0 };

        Single.never().doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable d) {
                count[0]++;
                return Unit.INSTANCE;
            }
        }).test();

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSuccess() {
        final Object[] event = { null };

        Single.just(1).doOnSuccess(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer e) {
                event[0] = e;
                return Unit.INSTANCE;
            }
        })
        .test();

        assertEquals(1, event[0]);
    }

    @Test
    public void doOnSubscribeNormal() {
        final int[] count = { 0 };

        Single.just(1).doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                count[0]++;
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSubscribeError() {
        final int[] count = { 0 };

        Single.error(new TestException()).doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                count[0]++;
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSubscribeJustCrash() {

        Single.just(1).doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnSubscribeErrorCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Single.error(new TestException("Outer")).doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
                @Override
                public Unit invoke(Disposable s) {
                    throw new TestException("Inner");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "Inner");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Outer");
        } finally {
            RxJavaCommonPlugins.reset();
        }

    }

    @Test
    public void onErrorSuccess() {
        final int[] call = { 0 };

        Single.just(1)
                .doOnError(new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable v) {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(0, call[0]);
    }

    @Test
    public void onErrorCrashes() {
        TestObserver<Object> to = Single.error(new TestException("Outer"))
                .doOnError(new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable v) {
                throw new TestException("Inner");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        TestCommonHelper.assertError(errors, 0, TestException.class, "Outer");
        TestCommonHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void doOnEventThrowsSuccess() {
        Single.just(1)
                .doOnEvent(new Function2<Integer, Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v, Throwable e) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnEventThrowsError() {
        TestObserver<Integer> to = Single.<Integer>error(new TestException("Main"))
                .doOnEvent(new Function2<Integer, Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v, Throwable e) {
                throw new TestException("Inner");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        TestCommonHelper.assertError(errors, 0, TestException.class, "Main");
        TestCommonHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void doOnDisposeDispose() {
        final int[] calls = { 0 };
        TestHelper.checkDisposed(PublishSubject.create().singleOrError().doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                calls[0]++;
                return Unit.INSTANCE;
            }
        }));

        assertEquals(1, calls[0]);
    }

    @Test
    public void doOnDisposeSuccess() {
        final int[] calls = { 0 };

        Single.just(1)
                .doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                calls[0]++;
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(0, calls[0]);
    }

    @Test
    public void doOnDisposeError() {
        final int[] calls = { 0 };

        Single.error(new TestException())
                .doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                calls[0]++;
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, calls[0]);
    }

    @Test
    public void doOnDisposeDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Single<Object> s) throws Exception {
                return s.doOnDispose(Functions.EMPTY_ACTION);
            }
        });
    }

    @Test
    public void doOnDisposeCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            PublishSubject<Integer> ps = PublishSubject.create();

            ps.singleOrError().doOnDispose(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .test()
            .cancel();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void doOnSuccessErrors() {
        final int[] call = { 0 };

        Single.error(new TestException())
                .doOnSuccess(new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object v) {
                call[0]++;
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void doOnSuccessCrash() {
        Single.just(1)
                .doOnSuccess(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            final Disposable bs = Disposables.empty();

            new Single<Integer>() {
                @Override
                protected void subscribeActual(SingleObserver<? super Integer> s) {
                    s.onSubscribe(bs);
                    s.onError(new TestException("Second"));
                    s.onSuccess(1);
                }
            }
                    .doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
                @Override
                public Unit invoke(Disposable s) {
                    throw new TestException("First");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isDisposed());

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
