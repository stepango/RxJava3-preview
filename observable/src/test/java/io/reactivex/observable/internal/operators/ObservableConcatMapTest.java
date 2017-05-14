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

import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import io.reactivex.observable.subjects.UnicastSubject;
import kotlin.jvm.functions.Function1;

public class ObservableConcatMapTest {

    @Test
    public void asyncFused() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Integer> to = us.concatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.range(v, 2);
            }
        })
        .test();

        us.onNext(1);
        us.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.<Integer>just(1).hide()
                .concatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.error(new TestException());
            }
        }));
    }

    @Test
    public void dispose2() {
        TestHelper.checkDisposed(Observable.<Integer>just(1).hide()
                .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.error(new TestException());
            }
        }));
    }

    @Test
    public void mainError() {
        Observable.<Integer>error(new TestException())
                .concatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Observable.<Integer>just(1).hide()
                .concatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainErrorDelayed() {
        Observable.<Integer>error(new TestException())
                .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorDelayError() {
        Observable.<Integer>just(1).hide()
                .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorDelayError2() {
        Observable.<Integer>just(1).hide()
                .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        throw new TestException();
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
                    .concatMap(new Function1<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> invoke(Integer v) {
                    return Observable.range(v, 2);
                }
            })
            .test()
            .assertResult(1, 2);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void badSourceDelayError() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
                    .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> invoke(Integer v) {
                    return Observable.range(v, 2);
                }
            })
            .test()
            .assertResult(1, 2);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void normalDelayErrors() {
        Observable.just(1).hide()
                .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void normalDelayErrorsTillTheEnd() {
        Observable.just(1).hide()
                .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.range(v, 2);
            }
        }, 16, true)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestObserver<Integer> to = ps1.concatMap(new Function1<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> invoke(Integer v) {
                        return ps2;
                    }
                }).test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertError(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @Test
    public void mapperThrows() {
        Observable.just(1).hide()
                .concatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedPollThrows() {
        Observable.just(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                throw new TestException();
            }
        })
                .concatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedPollThrowsDelayError() {
        Observable.just(1)
                .map(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                throw new TestException();
            }
        })
                .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperThrowsDelayError() {
        Observable.just(1).hide()
                .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badInnerDelayError() {
        @SuppressWarnings("rawtypes")
        final Observer[] o = { null };

        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable.just(1).hide()
                    .concatMapDelayError(new Function1<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> invoke(Integer v) {
                    return new Observable<Integer>() {
                        @Override
                        protected void subscribeActual(Observer<? super Integer> observer) {
                            o[0] = observer;
                            observer.onSubscribe(Disposables.empty());
                            observer.onComplete();
                        }
                    };
                }
            })
            .test()
            .assertResult();

            o[0].onError(new TestException());

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
