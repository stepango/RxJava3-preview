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
import org.mockito.InOrder;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.functions.BiFunction;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class ObservableSingleTest {

    @Test
    public void testSingleObservable() {
        Observable<Integer> o = Observable.just(1).singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2).singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmptyObservable() {
        Observable<Integer> o = Observable.<Integer> empty().singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateObservable() {
        Observable<Integer> o = Observable.just(1, 2)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmptyObservable() {
        Observable<Integer> o = Observable.just(1)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toObservable();
        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultObservable() {
        Observable<Integer> o = Observable.just(1).single(2).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2).single(3).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmptyObservable() {
        Observable<Integer> o = Observable.<Integer> empty()
                .single(1).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateObservable() {
        Observable<Integer> o = Observable.just(1, 2)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmptyObservable() {
        Observable<Integer> o = Observable.just(1)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 30000)
    public void testIssue1527Observable() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Observable<Integer> reduced = source.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 + i2;
            }
        }).toObservable();

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
    }

    @Test
    public void testSingle() {
        Maybe<Integer> o = Observable.just(1).singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElements() {
        Maybe<Integer> o = Observable.just(1, 2).singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmpty() {
        Maybe<Integer> o = Observable.<Integer> empty().singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicate() {
        Maybe<Integer> o = Observable.just(1, 2)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElements() {
        Maybe<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmpty() {
        Maybe<Integer> o = Observable.just(1)
                .filter(
                        new Function1<Integer, Boolean>() {

                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();
        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefault() {
        Single<Integer> o = Observable.just(1).single(2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElements() {
        Single<Integer> o = Observable.just(1, 2).single(3);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmpty() {
        Single<Integer> o = Observable.<Integer> empty()
                .single(1);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicate() {
        Single<Integer> o = Observable.just(1, 2)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElements() {
        Single<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmpty() {
        Single<Integer> o = Observable.just(1)
                .filter(new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 30000)
    public void testIssue1527() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Maybe<Integer> reduced = source.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 + i2;
            }
        });

        Integer r = reduced.blockingGet();
        assertEquals(21, r.intValue());
    }

    @Test
    public void singleElementOperatorDoNotSwallowExceptionWhenDone() {
        final Throwable exception = new RuntimeException("some error");
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        try {
            RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
                @Override
                public Unit invoke(final Throwable throwable) {
                    error.set(throwable);
                    return Unit.INSTANCE;
                }
            });

            Observable.unsafeCreate(new ObservableSource<Integer>() {
                @Override public void subscribe(final Observer<? super Integer> observer) {
                    observer.onComplete();
                    observer.onError(exception);
                }
            }).singleElement().test().assertComplete();

            assertSame(exception, error.get().getCause());
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void singleOrErrorNoElement() {
        Observable.empty()
            .singleOrError()
            .test()
            .assertNoValues()
            .assertError(NoSuchElementException.class);
    }

    @Test
    public void singleOrErrorOneElement() {
        Observable.just(1)
            .singleOrError()
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void singleOrErrorMultipleElements() {
        Observable.just(1, 2, 3)
            .singleOrError()
            .test()
            .assertNoValues()
            .assertError(IllegalArgumentException.class);
    }

    @Test
    public void singleOrErrorError() {
        Observable.error(new RuntimeException("error"))
            .singleOrError()
            .test()
            .assertNoValues()
            .assertErrorMessage("error")
            .assertError(RuntimeException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Object>, Object>() {
            @Override
            public Object invoke(Observable<Object> o) {
                return o.singleOrError();
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceObservable(new Function1<Observable<Object>, Object>() {
            @Override
            public Object invoke(Observable<Object> o) {
                return o.singleElement();
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function1<Observable<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> invoke(Observable<Object> o) {
                return o.singleOrError();
            }
        });

        TestHelper.checkDoubleOnSubscribeObservableToMaybe(new Function1<Observable<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> invoke(Observable<Object> o) {
                return o.singleElement();
            }
        });
    }
}
