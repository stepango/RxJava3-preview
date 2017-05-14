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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.List;

import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.QueueDisposable;
import io.reactivex.observable.observers.ObserverFusion;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import io.reactivex.observable.subjects.UnicastSubject;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableDistinctUntilChangedTest {

    Observer<String> w;
    Observer<String> w2;

    // nulls lead to exceptions
    final Function1<String, String> TO_UPPER_WITH_EXCEPTION = new Function1<String, String>() {
        @Override
        public String invoke(String s) {
            if (s.equals("x")) {
                return "xx";
            }
            return s.toUpperCase();
        }
    };

    @Before
    public void before() {
        w = TestHelper.mockObserver();
        w2 = TestHelper.mockObserver();
    }

    @Test
    public void testDistinctUntilChangedOfNone() {
        Observable<String> src = Observable.empty();
        src.distinctUntilChanged().subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testDistinctUntilChangedOfNoneWithKeySelector() {
        Observable<String> src = Observable.empty();
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testDistinctUntilChangedOfNormalSource() {
        Observable<String> src = Observable.just("a", "b", "c", "c", "c", "b", "b", "a", "e");
        src.distinctUntilChanged().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctUntilChangedOfNormalSourceWithKeySelector() {
        Observable<String> src = Observable.just("a", "b", "c", "C", "c", "B", "b", "a", "e");
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("B");
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testDistinctUntilChangedOfSourceWithNulls() {
        Observable<String> src = Observable.just(null, "a", "a", null, null, "b", null, null);
        src.distinctUntilChanged().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testDistinctUntilChangedOfSourceWithExceptionsFromKeySelector() {
        Observable<String> src = Observable.just("a", "b", null, "c");
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        verify(w, times(1)).onError(any(NullPointerException.class));
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, never()).onComplete();
    }

    @Test
    public void customComparator() {
        Observable<String> source = Observable.just("a", "b", "B", "A","a", "C");

        TestObserver<String> ts = TestObserver.create();

        source.distinctUntilChanged(new Function2<String, String, Boolean>() {
            @Override
            public Boolean invoke(String a, String b) {
                return a.compareToIgnoreCase(b) == 0;
            }
        })
        .subscribe(ts);

        ts.assertValues("a", "b", "A", "C");
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void customComparatorThrows() {
        Observable<String> source = Observable.just("a", "b", "B", "A","a", "C");

        TestObserver<String> ts = TestObserver.create();

        source.distinctUntilChanged(new Function2<String, String, Boolean>() {
            @Override
            public Boolean invoke(String a, String b) {
                throw new TestException();
            }
        })
        .subscribe(ts);

        ts.assertValue("a");
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void fused() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        Observable.just(1, 2, 2, 3, 3, 4, 5)
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .subscribe(to);

        to.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.SYNC))
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void fusedAsync() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        UnicastSubject<Integer> up = UnicastSubject.create();

        up
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .subscribe(to);

        TestHelper.emit(up, 1, 2, 2, 3, 3, 4, 5);

        to.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void ignoreCancel() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable.wrap(new ObservableSource<Integer>() {
                @Override
                public void subscribe(Observer<? super Integer> s) {
                    s.onSubscribe(Disposables.empty());
                    s.onNext(1);
                    s.onNext(2);
                    s.onNext(3);
                    s.onError(new IOException());
                    s.onComplete();
                }
            })
                    .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
                @Override
                public Boolean invoke(Integer a, Integer b) {
                    throw new TestException();
                }
            })
            .test()
            .assertFailure(TestException.class, 1);

            TestCommonHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
   }

    class Mutable {
        int value;
    }

    @Test
    public void mutableWithSelector() {
        Mutable m = new Mutable();

        PublishSubject<Mutable> pp = PublishSubject.create();

        TestObserver<Mutable> ts = pp.distinctUntilChanged(new Function1<Mutable, Object>() {
            @Override
            public Object invoke(Mutable m) {
                return m.value;
            }
        })
        .test();

        pp.onNext(m);
        m.value = 1;
        pp.onNext(m);
        pp.onComplete();

        ts.assertResult(m, m);
    }
}
