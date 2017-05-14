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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableAnyTest {

    @Test
    public void testAnyWithTwoItemsObservable() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithTwoItemsObservable() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> observable = w.isEmpty().toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithOneItemObservable() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithOneItemObservable() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> observable = w.isEmpty().toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmptyObservable() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithEmptyObservable() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.isEmpty().toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate1Observable() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 2;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testExists1Observable() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 2;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate2Observable() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 1;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmptyAndPredicateObservable() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t) {
                return true;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirstObservable() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Observable<Boolean> anyEven = o.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer i) {
                return i % 2 == 0;
            }
        }).toObservable();

        assertTrue(anyEven.blockingFirst());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstreamObservable() {
        Observable<Integer> source = Observable.just(1).isEmpty().toObservable()
                .flatMap(new Function1<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> invoke(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessageObservable() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();
        final IllegalArgumentException ex = new IllegalArgumentException();

        Observable.just("Boo!").any(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String v) {
                throw ex;
            }
        }).subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void testAnyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Single<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Single<Boolean> observable = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Single<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Single<Boolean> observable = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Single<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Single<Boolean> observable = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithPredicate1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testExists1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithPredicate2() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 < 1;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Single<Boolean> observable = w.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWithFollowingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Single<Boolean> anyEven = o.any(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer i) {
                return i % 2 == 0;
            }
        });

        assertTrue(anyEven.blockingGet());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1).isEmpty()
                .flatMapObservable(new Function1<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> invoke(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();
        final IllegalArgumentException ex = new IllegalArgumentException();

        Observable.just("Boo!").any(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String v) {
                throw ex;
            }
        }).subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).any(Functions.alwaysTrue()).toObservable());

        TestHelper.checkDisposed(Observable.just(1).any(Functions.alwaysTrue()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Boolean>>() {
            @Override
            public ObservableSource<Boolean> invoke(Observable<Object> o) {
                return o.any(Functions.alwaysTrue()).toObservable();
            }
        });
        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function1<Observable<Object>, SingleSource<Boolean>>() {
            @Override
            public SingleSource<Boolean> invoke(Observable<Object> o) {
                return o.any(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void predicateThrowsSuppressOthers() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new IOException());
                    observer.onComplete();
                }
            }
                    .any(new Function1<Integer, Boolean>() {
                @Override
                public Boolean invoke(Integer v) {
                    throw new TestException();
                }
            })
            .toObservable()
            .test()
            .assertFailure(TestException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void badSourceSingle() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new TestException("First"));

                    observer.onNext(1);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }
            .any(Functions.alwaysTrue())
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
