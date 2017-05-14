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

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Schedulers;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.DefaultObserver;
import io.reactivex.observable.observers.TestObserver;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableTakeLastTest {

    @Test
    public void testTakeLastEmpty() {
        Observable<String> w = Observable.empty();
        Observable<String> take = w.takeLast(2);

        Observer<String> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeLast1() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> take = w.takeLast(2);

        Observer<String> observer = TestHelper.mockObserver();
        InOrder inOrder = inOrder(observer);
        take.subscribe(observer);
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeLast2() {
        Observable<String> w = Observable.just("one");
        Observable<String> take = w.takeLast(10);

        Observer<String> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeLastWithZeroCount() {
        Observable<String> w = Observable.just("one");
        Observable<String> take = w.takeLast(0);

        Observer<String> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testTakeLastWithNull() {
        Observable<String> w = Observable.just("one", null, "three");
        Observable<String> take = w.takeLast(2);

        Observer<String> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTakeLastWithNegativeCount() {
        Observable.just("one").takeLast(-1);
    }

    @Test
    public void testBackpressure1() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(1, 100000).takeLast(1)
        .observeOn(Schedulers.newThread())
        .map(newSlowProcessor()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertValue(100000);
    }

    @Test
    public void testBackpressure2() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(1, 100000).takeLast(Observable.bufferSize() * 4)
        .observeOn(Schedulers.newThread()).map(newSlowProcessor()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 4, ts.valueCount());
    }

    private Function1<Integer, Integer> newSlowProcessor() {
        return new Function1<Integer, Integer>() {
            int c;

            @Override
            public Integer invoke(Integer i) {
                if (c++ < 100) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
                return i;
            }

        };
    }

    @Test
    public void testIssue1522() {
        // https://github.com/ReactiveX/RxJava/issues/1522
        assertNull(Observable
                .empty()
                .count()
                .filter(new Function1<Long, Boolean>() {
                    @Override
                    public Boolean invoke(Long v) {
                        return false;
                    }
                })
                .blockingGet());
    }

    @Test
    public void testUnsubscribeTakesEffectEarlyOnFastPath() {
        final AtomicInteger count = new AtomicInteger();
        Observable.range(0, 100000).takeLast(100000).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onStart() {
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                count.incrementAndGet();
                cancel();
            }
        });
        assertEquals(1,count.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 10).takeLast(5));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Object> o) {
                return o.takeLast(5);
            }
        });
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .takeLast(5)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void takeLastTake() {
        Observable.range(1, 10)
        .takeLast(5)
        .take(2)
        .test()
        .assertResult(6, 7);
    }
}
