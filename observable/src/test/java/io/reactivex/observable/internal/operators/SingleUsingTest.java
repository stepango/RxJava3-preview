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

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleUsingTest {

    Function1<Disposable, Single<Integer>> mapper = new Function1<Disposable, Single<Integer>>() {
        @Override
        public Single<Integer> invoke(Disposable d) {
            return Single.just(1);
        }
    };

    Function1<Disposable, Single<Integer>> mapperThrows = new Function1<Disposable, Single<Integer>>() {
        @Override
        public Single<Integer> invoke(Disposable d) {
            throw new TestException("Mapper");
        }
    };

    Function1<Disposable, kotlin.Unit> disposer = new Function1<Disposable, kotlin.Unit>() {
        @Override
        public Unit invoke(Disposable d) {
            d.dispose();
            return Unit.INSTANCE;
        }
    };

    Function1<Disposable, kotlin.Unit> disposerThrows = new Function1<Disposable, kotlin.Unit>() {
        @Override
        public Unit invoke(Disposable d) {
            throw new TestException("Disposer");
        }
    };

    @Test
    public void resourceSupplierThrows() {
        Single.using(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new TestException();
            }
        }, Functions.justFunction(Single.just(1)), Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void normalEager() {
        Single.using(Functions.justCallable(1), Functions.justFunction(Single.just(1)), Functions.emptyConsumer())
        .test()
        .assertResult(1);
    }

    @Test
    public void normalNonEager() {
        Single.using(Functions.justCallable(1), Functions.justFunction(Single.just(1)), Functions.emptyConsumer(), false)
        .test()
        .assertResult(1);
    }

    @Test
    public void errorEager() {
        Single.using(Functions.justCallable(1), Functions.justFunction(Single.error(new TestException())), Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorNonEager() {
        Single.using(Functions.justCallable(1), Functions.justFunction(Single.error(new TestException())), Functions.emptyConsumer(), false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void eagerMapperThrowsDisposerThrows() {
        TestObserver<Integer> ts = Single.using(Functions.justCallable(Disposables.empty()), mapperThrows, disposerThrows)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> ce = TestCommonHelper.compositeList(ts.errors().get(0));
        TestCommonHelper.assertError(ce, 0, TestException.class, "Mapper");
        TestCommonHelper.assertError(ce, 1, TestException.class, "Disposer");
    }

    @Test
    public void noneagerMapperThrowsDisposerThrows() {

        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Single.using(Functions.justCallable(Disposables.empty()), mapperThrows, disposerThrows, false)
            .test()
            .assertFailureAndMessage(TestException.class, "Mapper");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Disposer");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void resourceDisposedIfMapperCrashes() {
        Disposable d = Disposables.empty();

        Single.using(Functions.justCallable(d), mapperThrows, disposer)
        .test()
        .assertFailure(TestException.class);

        assertTrue(d.isDisposed());
    }

    @Test
    public void resourceDisposedIfMapperCrashesNonEager() {
        Disposable d = Disposables.empty();

        Single.using(Functions.justCallable(d), mapperThrows, disposer, false)
        .test()
        .assertFailure(TestException.class);

        assertTrue(d.isDisposed());
    }

    @Test
    public void dispose() {
        Disposable d = Disposables.empty();

        Single.using(Functions.justCallable(d), mapper, disposer, false)
        .test(true);

        assertTrue(d.isDisposed());
    }

    @Test
    public void disposerThrowsEager() {
        Single.using(Functions.justCallable(Disposables.empty()), mapper, disposerThrows)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void disposerThrowsNonEager() {

        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Single.using(Functions.justCallable(Disposables.empty()), mapper, disposerThrows, false)
            .test()
            .assertResult(1);
            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Disposer");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void errorAndDisposerThrowsEager() {
        TestObserver<Integer> ts = Single.using(Functions.justCallable(Disposables.empty()),
                new Function1<Disposable, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Disposable v) {
                return Single.<Integer>error(new TestException("Mapper-run"));
            }
        }, disposerThrows)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> ce = TestCommonHelper.compositeList(ts.errors().get(0));
        TestCommonHelper.assertError(ce, 0, TestException.class, "Mapper-run");
        TestCommonHelper.assertError(ce, 1, TestException.class, "Disposer");
    }

    @Test
    public void errorAndDisposerThrowsNonEager() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Single.using(Functions.justCallable(Disposables.empty()),
                    new Function1<Disposable, SingleSource<Integer>>() {
                @Override
                public SingleSource<Integer> invoke(Disposable v) {
                    return Single.<Integer>error(new TestException("Mapper-run"));
                }
            }, disposerThrows, false)
            .test()
            .assertFailure(TestException.class);
            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Disposer");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void successDisposeRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> pp = PublishSubject.create();

            Disposable d = Disposables.empty();

            final TestObserver<Integer> ts = Single.using(Functions.justCallable(d), new Function1<Disposable, SingleSource<Integer>>() {
                @Override
                public SingleSource<Integer> invoke(Disposable v) {
                    return pp.single(-99);
                }
            }, disposer)
            .test();

            pp.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());

            assertTrue(d.isDisposed());
        }
    }

    @Test
    public void doubleOnSubscribe() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Single.using(Functions.justCallable(1), new Function1<Integer, SingleSource<Integer>>() {
                @Override
                public SingleSource<Integer> invoke(Integer v) {
                    return new Single<Integer>() {
                        @Override
                        protected void subscribeActual(SingleObserver<? super Integer> observer) {
                            observer.onSubscribe(Disposables.empty());

                            assertFalse(((Disposable)observer).isDisposed());

                            Disposable d = Disposables.empty();
                            observer.onSubscribe(d);

                            assertTrue(d.isDisposed());

                            assertFalse(((Disposable)observer).isDisposed());

                            observer.onSuccess(1);

                            assertTrue(((Disposable)observer).isDisposed());
                        }
                    };
                }
            }, Functions.emptyConsumer())
            .test()
            .assertResult(1)
            ;

            TestCommonHelper.assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void errorDisposeRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> pp = PublishSubject.create();

            Disposable d = Disposables.empty();

            final TestObserver<Integer> ts = Single.using(Functions.justCallable(d), new Function1<Disposable, SingleSource<Integer>>() {
                @Override
                public SingleSource<Integer> invoke(Disposable v) {
                    return pp.single(-99);
                }
            }, disposer)
            .test();

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onError(ex);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());

            assertTrue(d.isDisposed());
        }
    }
}
