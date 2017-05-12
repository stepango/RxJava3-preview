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
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ObservableAllTest {

    @Test
    public void testAllObservable() {
        Observable<String> obs = Observable.just("one", "two", "six");

        Observer <Boolean> observer = TestHelper.mockObserver();

        obs.all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return s.length() == 3;
            }
        }).toObservable()
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onNext(true);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testNotAllObservable() {
        Observable<String> obs = Observable.just("one", "two", "three", "six");

        Observer <Boolean> observer = TestHelper.mockObserver();

        obs.all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return s.length() == 3;
            }
        }).toObservable()
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onNext(false);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testEmptyObservable() {
        Observable<String> obs = Observable.empty();

        Observer <Boolean> observer = TestHelper.mockObserver();

        obs.all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return s.length() == 3;
            }
        }).toObservable()
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onNext(true);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testErrorObservable() {
        Throwable error = new Throwable();
        Observable<String> obs = Observable.error(error);

        Observer <Boolean> observer = TestHelper.mockObserver();

        obs.all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return s.length() == 3;
            }
        }).toObservable()
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testFollowingFirstObservable() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Observable<Boolean> allOdd = o.all(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer i) {
                return i % 2 == 1;
            }
        }).toObservable();

        assertFalse(allOdd.blockingFirst());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstreamObservable() {
        Observable<Integer> source = Observable.just(1)
                .all(new Function1<Integer, Boolean>() {
                @Override
                public Boolean invoke(Integer t1) {
                    return false;
                }
            }).toObservable()
            .flatMap(new Function<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }


    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessageObservable() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();

        final IllegalArgumentException ex = new IllegalArgumentException();

        Observable.just("Boo!").all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String v) {
                throw ex;
            }
        })
        .subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }


    @Test
    public void testAll() {
        Observable<String> obs = Observable.just("one", "two", "six");

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(true);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testNotAll() {
        Observable<String> obs = Observable.just("one", "two", "three", "six");

        SingleObserver <Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(false);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testEmpty() {
        Observable<String> obs = Observable.empty();

        SingleObserver <Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(true);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testError() {
        Throwable error = new Throwable();
        Observable<String> obs = Observable.error(error);

        SingleObserver <Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testFollowingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Single<Boolean> allOdd = o.all(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer i) {
                return i % 2 == 1;
            }
        });

        assertFalse(allOdd.blockingGet());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1)
                .all(new Function1<Integer, Boolean>() {
                @Override
                public Boolean invoke(Integer t1) {
                    return false;
                }
            })
            .flatMapObservable(new Function<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }


    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();

        final IllegalArgumentException ex = new IllegalArgumentException();

        Observable.just("Boo!").all(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String v) {
                throw ex;
            }
        })
        .subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).all(Functions.alwaysTrue()).toObservable());

        TestHelper.checkDisposed(Observable.just(1).all(Functions.alwaysTrue()));
    }

    @Test
    public void predicateThrowsObservable() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
                    .all(new Function1<Integer, Boolean>() {
                @Override
                public Boolean invoke(Integer v) {
                    throw new TestException();
                }
            })
            .toObservable()
            .test()
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void predicateThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
                    .all(new Function1<Integer, Boolean>() {
                @Override
                public Boolean invoke(Integer v) {
                    throw new TestException();
                }
            })
            .test()
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
