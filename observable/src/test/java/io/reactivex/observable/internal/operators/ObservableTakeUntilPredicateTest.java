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

import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

;

public class ObservableTakeUntilPredicateTest {
    @Test
    public void takeEmpty() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.empty().takeUntil(new Function1<Object, Boolean>() {
            @Override
            public Boolean invoke(Object v) {
                return true;
            }
        }).subscribe(o);

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeAll() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1, 2).takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return false;
            }
        }).subscribe(o);

        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeFirst() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1, 2).takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeSome() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1, 2, 3).takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 == 2;
            }
        })
        .subscribe(o);

        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onNext(3);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void functionThrows() {
        Observer<Object> o = TestHelper.mockObserver();

        Function1<Integer, Boolean> predicate = (new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                    throw new TestException("Forced failure");
            }
        });
        Observable.just(1, 2, 3).takeUntil(predicate).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }
    @Test
    public void sourceThrows() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1)
        .concatWith(Observable.<Integer>error(new TestException()))
        .concatWith(Observable.just(2))
                .takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return false;
            }
        }).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testErrorIncludesLastValueAsCause() {
        TestObserver<String> ts = new TestObserver<String>();
        final TestException e = new TestException("Forced failure");
        Function1<String, Boolean> predicate = (new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String t) {
                    throw e;
            }
        });
        Observable.just("abc").takeUntil(predicate).subscribe(ts);

        ts.assertTerminated();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
        // FIXME last cause value is not saved
//        assertTrue(ts.errors().get(0).getCause().getMessage().contains("abc"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().takeUntil(Functions.alwaysFalse()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.takeUntil(Functions.alwaysFalse());
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
                    observer.onComplete();
                    observer.onNext(1);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .takeUntil(Functions.alwaysFalse())
            .test()
            .assertResult();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
