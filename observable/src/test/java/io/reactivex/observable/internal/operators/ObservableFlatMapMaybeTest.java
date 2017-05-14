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
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObservableFlatMapMaybeTest {

    @Test
    public void normal() {
        Observable.range(1, 10)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalEmpty() {
        Observable.range(1, 10)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.empty();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void normalDelayError() {
        Observable.range(1, 10)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(v);
            }
        }, true)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalAsync() {
        Observable.range(1, 10)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(v).subscribeOn(Schedulers.computation());
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
    public void mapperThrowsObservable() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
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
    public void mapperReturnsNullObservable() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
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
    public void normalDelayErrorAll() {
        TestObserver<Integer> to = Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.error(new TestException());
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
    public void takeAsync() {
        Observable.range(1, 10)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(v).subscribeOn(Schedulers.computation());
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
        Observable.range(1, 10)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(v);
            }
        })
        .take(2)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void middleError() {
        Observable.fromArray(new String[]{"1", "a", "2"}).flatMapMaybe(new Function1<String, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(final String s) {
                //return Single.just(Integer.valueOf(s)); //This works
                return Maybe.fromCallable(new Callable<Integer>() {
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
        Observable.range(1, 1000)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(1).subscribeOn(Schedulers.computation());
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
    public void asyncFlattenNone() {
        Observable.range(1, 1000)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.<Integer>empty().subscribeOn(Schedulers.computation());
            }
        })
        .take(500)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void successError() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = Observable.range(1, 2)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                if (v == 2) {
                    return ps.singleElement();
                }
                return Maybe.error(new TestException());
            }
        }, true)
        .test();

        ps.onNext(1);
        ps.onComplete();

        to
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void completeError() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = Observable.range(1, 2)
                .flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                if (v == 2) {
                    return ps.singleElement();
                }
                return Maybe.error(new TestException());
            }
        }, true)
        .test();

        ps.onComplete();

        to
        .assertFailure(TestException.class);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishSubject.<Integer>create().flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.<Integer>empty();
            }
        }));
    }

    @Test
    public void innerSuccessCompletesAfterMain() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = Observable.just(1).flatMapMaybe(Functions.justFunction(ps.singleElement()))
        .test();

        ps.onNext(2);
        ps.onComplete();

        to
        .assertResult(2);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Observable<Object> f) {
                return f.flatMapMaybe(Functions.justFunction(Maybe.just(2)));
            }
        });
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new TestException("First"));
                    observer.onError(new TestException("Second"));
                }
            }
            .flatMapMaybe(Functions.justFunction(Maybe.just(2)))
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
            Observable.just(1)
            .flatMapMaybe(Functions.justFunction(new Maybe<Integer>() {
                @Override
                protected void subscribeActual(MaybeObserver<? super Integer> observer) {
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
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps2.onNext(2);
                    ps2.onComplete();
                }
            }
        };

        Observable.just(ps1, ps2)
                .flatMapMaybe(new Function1<PublishSubject<Integer>, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(PublishSubject<Integer> v) {
                        return v.singleElement();
                    }
                })
        .subscribe(to);

        ps1.onNext(1);
        ps1.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void emissionQueueTrigger2() {
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();
        final PublishSubject<Integer> ps3 = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps2.onNext(2);
                    ps2.onComplete();
                }
            }
        };

        Observable.just(ps1, ps2, ps3)
                .flatMapMaybe(new Function1<PublishSubject<Integer>, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(PublishSubject<Integer> v) {
                        return v.singleElement();
                    }
                })
        .subscribe(to);

        ps1.onNext(1);
        ps1.onComplete();

        ps3.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void disposeInner() {
        final TestObserver<Object> to = new TestObserver<Object>();

        Observable.just(1).flatMapMaybe(new Function1<Integer, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> invoke(Integer v) {
                return new Maybe<Object>() {
                    @Override
                    protected void subscribeActual(MaybeObserver<? super Object> observer) {
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
}
