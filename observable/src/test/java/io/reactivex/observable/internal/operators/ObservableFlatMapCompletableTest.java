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
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.CompletableSource;
import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.QueueDisposable;
import io.reactivex.observable.observers.ObserverFusion;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ObservableFlatMapCompletableTest {

    @Test
    public void normalObservable() {
        Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        }).toObservable()
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrowsObservable() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                throw new TestException();
            }
        }).<Integer>toObservable()
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void mapperReturnsNullObservable() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return null;
            }
        }).<Integer>toObservable()
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(NullPointerException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void normalDelayErrorObservable() {
        Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        }, true).toObservable()
        .test()
        .assertResult();
    }

    @Test
    public void normalAsyncObservable() {
        Observable.range(1, 1000)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Observable.range(1, 100).subscribeOn(Schedulers.computation()).ignoreElements();
            }
        }).toObservable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalDelayErrorAllObservable() {
        TestObserver<Integer> to = Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.error(new TestException());
            }
        }, true).<Integer>toObservable()
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 11; i++) {
            TestCommonHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalDelayInnerErrorAllObservable() {
        TestObserver<Integer> to = Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.error(new TestException());
            }
        }, true).<Integer>toObservable()
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 10; i++) {
            TestCommonHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalNonDelayErrorOuterObservable() {
        Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        }, false).toObservable()
        .test()
        .assertFailure(TestException.class);
    }


    @Test
    public void fusedObservable() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        }).<Integer>toObservable()
        .subscribe(to);

        to
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult();
    }

    @Test
    public void disposedObservable() {
        TestHelper.checkDisposed(Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        }).toObservable());
    }

    @Test
    public void normal() {
        Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrows() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Void> to = ps
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                throw new TestException();
            }
        })
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void mapperReturnsNull() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Void> to = ps
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return null;
            }
        })
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertFailure(NullPointerException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void normalDelayError() {
        Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        }, true)
        .test()
        .assertResult();
    }

    @Test
    public void normalAsync() {
        Observable.range(1, 1000)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Observable.range(1, 100).subscribeOn(Schedulers.computation()).ignoreElements();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalDelayErrorAll() {
        TestObserver<Void> to = Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.error(new TestException());
            }
        }, true)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 11; i++) {
            TestCommonHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalDelayInnerErrorAll() {
        TestObserver<Void> to = Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.error(new TestException());
            }
        }, true)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 10; i++) {
            TestCommonHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalNonDelayErrorOuter() {
        Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        }, false)
        .test()
        .assertFailure(TestException.class);
    }


    @Test
    public void fused() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        })
        .<Integer>toObservable()
        .subscribe(to);

        to
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult();
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        }));
    }

    @Test
    public void innerObserver() {
        Observable.range(1, 3)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return new Completable() {
                    @Override
                    protected void subscribeActual(CompletableObserver s) {
                        s.onSubscribe(Disposables.empty());

                        assertFalse(((Disposable)s).isDisposed());

                        ((Disposable)s).dispose();

                        assertTrue(((Disposable)s).isDisposed());
                    }
                };
            }
        })
        .test();
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Integer>, Object>() {
            @Override
            public Object invoke(Observable<Integer> o) {
                return o.flatMapCompletable(new Function1<Integer, CompletableSource>() {
                    @Override
                    public CompletableSource invoke(Integer v) {
                        return Completable.complete();
                    }
                });
            }
        }, false, 1, null);
    }

    @Test
    public void fusedInternalsObservable() {
        Observable.range(1, 10)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return Completable.complete();
            }
        })
        .toObservable()
        .subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
                QueueDisposable<?> qd = (QueueDisposable<?>)d;
                try {
                    assertNull(qd.poll());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
                assertTrue(qd.isEmpty());
                qd.clear();
            }

            @Override
            public void onNext(Object t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void innerObserverObservable() {
        Observable.range(1, 3)
                .flatMapCompletable(new Function1<Integer, CompletableSource>() {
            @Override
            public CompletableSource invoke(Integer v) {
                return new Completable() {
                    @Override
                    protected void subscribeActual(CompletableObserver s) {
                        s.onSubscribe(Disposables.empty());

                        assertFalse(((Disposable)s).isDisposed());

                        ((Disposable)s).dispose();

                        assertTrue(((Disposable)s).isDisposed());
                    }
                };
            }
        })
        .toObservable()
        .test();
    }

    @Test
    public void badSourceObservable() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Integer>, Object>() {
            @Override
            public Object invoke(Observable<Integer> o) {
                return o.flatMapCompletable(new Function1<Integer, CompletableSource>() {
                    @Override
                    public CompletableSource invoke(Integer v) {
                        return Completable.complete();
                    }
                }).toObservable();
            }
        }, false, 1, null);
    }
}
