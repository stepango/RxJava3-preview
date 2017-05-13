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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.QueueDisposable;
import io.reactivex.observable.observers.DefaultObserver;
import io.reactivex.observable.observers.ObserverFusion;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableRangeTest {

    @Test
    public void testRangeStartAt2Count3() {
        Observer<Integer> observer = TestHelper.mockObserver();

        Observable.range(2, 3).subscribe(observer);

        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(4);
        verify(observer, never()).onNext(5);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testRangeUnsubscribe() {
        Observer<Integer> observer = TestHelper.mockObserver();

        final AtomicInteger count = new AtomicInteger();

        Observable.range(1, 1000).doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer t1) {
                count.incrementAndGet();
                return Unit.INSTANCE;
            }
        })
        .take(3).subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, never()).onNext(4);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        assertEquals(3, count.get());
    }

    @Test
    public void testRangeWithZero() {
        Observable.range(1, 0);
    }

    @Test
    public void testRangeWithOverflow2() {
        Observable.range(Integer.MAX_VALUE, 0);
    }

    @Test
    public void testRangeWithOverflow3() {
        Observable.range(1, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeWithOverflow4() {
        Observable.range(2, Integer.MAX_VALUE);
    }

    @Test
    public void testRangeWithOverflow5() {
        assertFalse(Observable.range(Integer.MIN_VALUE, 0).blockingIterable().iterator().hasNext());
    }

    @Test
    public void testNoBackpressure() {
        ArrayList<Integer> list = new ArrayList<Integer>(Observable.bufferSize() * 2);
        for (int i = 1; i <= Observable.bufferSize() * 2 + 1; i++) {
            list.add(i);
        }

        Observable<Integer> o = Observable.range(1, list.size());

        TestObserver<Integer> ts = new TestObserver<Integer>();

        o.subscribe(ts);

        ts.assertValueSequence(list);
        ts.assertTerminated();
    }

    @Test
    public void testEmptyRangeSendsOnCompleteEagerlyWithRequestZero() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Observable.range(1, 0).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onStart() {
//                request(0);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {

            }});
        assertTrue(completed.get());
    }

    @Test(timeout = 1000)
    public void testNearMaxValueWithoutBackpressure() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);

        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }

    @Test
    public void negativeCount() {
        try {
            Observable.range(1, -1);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -1", ex.getMessage());
        }
    }

    @Test
    public void requestWrongFusion() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ASYNC);

        Observable.range(1, 5)
        .subscribe(to);

        ObserverFusion.assertFusion(to, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }
}
