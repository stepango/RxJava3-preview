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
import io.reactivex.common.functions.Function;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.CompletableSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompletableUsingTest {

    @Test
    public void resourceSupplierThrows() {

        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.complete();
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorEager() {

        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.error(new TestException());
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyEager() {

        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.complete();
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        }, true)
        .test()
        .assertResult();
    }

    @Test
    public void errorNonEager() {

        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.error(new TestException());
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        }, false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyNonEager() {

        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.complete();
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        }, false)
        .test()
        .assertResult();
    }

    @Test
    public void supplierCrashEager() {

        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                throw new TestException();
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void supplierCrashNonEager() {

        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                throw new TestException();
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        }, false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void supplierAndDisposerCrashEager() {
        TestObserver<Void> to = Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                throw new TestException("Main");
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                throw new TestException("Disposer");
            }
        }, true)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> list = TestCommonHelper.compositeList(to.errors().get(0));

        TestCommonHelper.assertError(list, 0, TestException.class, "Main");
        TestCommonHelper.assertError(list, 1, TestException.class, "Disposer");
    }

    @Test
    public void supplierAndDisposerCrashNonEager() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Completable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function<Object, CompletableSource>() {
                @Override
                public CompletableSource apply(Object v) throws Exception {
                    throw new TestException("Main");
                }
            }, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object d) {
                    throw new TestException("Disposer");
                }
            }, false)
            .test()
            .assertFailureAndMessage(TestException.class, "Main");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Disposer");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        final int[] call = {0 };

        TestObserver<Void> to = Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.never();
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                call[0]++;
                return Unit.INSTANCE;
            }
        }, false)
        .test();

        to.cancel();

        assertEquals(1, call[0]);
    }

    @Test
    public void disposeCrashes() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            TestObserver<Void> to = Completable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function<Object, CompletableSource>() {
                @Override
                public CompletableSource apply(Object v) throws Exception {
                    return Completable.never();
                }
            }, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object d) {
                    throw new TestException();
                }
            }, false)
            .test();

            to.cancel();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void isDisposed() {
        TestHelper.checkDisposed(Completable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function<Object, CompletableSource>() {
                @Override
                public CompletableSource apply(Object v) throws Exception {
                    return Completable.never();
                }
        }, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object d) {
                    return Unit.INSTANCE;
                }
            }, false));
    }

    @Test
    public void justDisposerCrashes() {
        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.complete();
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                throw new TestException("Disposer");
            }
        }, true)
        .test()
        .assertFailure(TestException.class);
    }


    @Test
    public void emptyDisposerCrashes() {
        Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.complete();
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                throw new TestException("Disposer");
            }
        }, true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorDisposerCrash() {
        TestObserver<Void> to = Completable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function<Object, CompletableSource>() {
            @Override
            public CompletableSource apply(Object v) throws Exception {
                return Completable.error(new TestException("Main"));
            }
        }, new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object d) {
                throw new TestException("Disposer");
            }
        }, true)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> list = TestCommonHelper.compositeList(to.errors().get(0));

        TestCommonHelper.assertError(list, 0, TestException.class, "Main");
        TestCommonHelper.assertError(list, 1, TestException.class, "Disposer");
    }

    @Test
    public void doubleOnSubscribe() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Completable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function<Object, CompletableSource>() {
                @Override
                public CompletableSource apply(Object v) throws Exception {
                    return Completable.wrap(new CompletableSource() {
                        @Override
                        public void subscribe(CompletableObserver s) {
                            Disposable d1 = Disposables.empty();

                            s.onSubscribe(d1);

                            Disposable d2 = Disposables.empty();

                            s.onSubscribe(d2);

                            assertFalse(d1.isDisposed());

                            assertTrue(d2.isDisposed());
                        }
                    });
                }
            }, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object d) {
                    return Unit.INSTANCE;
                }
            }, false).test();
            TestCommonHelper.assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void successDisposeRace() {
        for (int i = 0; i < 500; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Void> to = Completable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function<Object, CompletableSource>() {
                @Override
                public CompletableSource apply(Object v) throws Exception {
                    return ps.ignoreElements();
                }
            }, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object d) {
                    return Unit.INSTANCE;
                }
            }, true)
            .test();

            ps.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void errorDisposeRace() {
        for (int i = 0; i < 500; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Void> to = Completable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function<Object, CompletableSource>() {
                @Override
                public CompletableSource apply(Object v) throws Exception {
                    return ps.ignoreElements();
                }
            }, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object d) {
                    return Unit.INSTANCE;
                }
            }, true)
            .test();

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onError(ex);
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void emptyDisposeRace() {
        for (int i = 0; i < 500; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Void> to = Completable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function<Object, CompletableSource>() {
                @Override
                public CompletableSource apply(Object v) throws Exception {
                    return ps.ignoreElements();
                }
            }, new Function1<Object, Unit>() {
                @Override
                public Unit invoke(Object d) {
                    return Unit.INSTANCE;
                }
            }, true)
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

}
