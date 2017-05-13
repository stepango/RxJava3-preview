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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.functions.Function;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObservableTakeLastOneTest {

    @Test
    public void testLastOfManyReturnsLast() {
        TestObserver<Integer> s = new TestObserver<Integer>();
        Observable.range(1, 10).takeLast(1).subscribe(s);
        s.assertValue(10);
        s.assertNoErrors();
        s.assertTerminated();
        // NO longer assertable
//        s.assertUnsubscribed();
    }

    @Test
    public void testLastOfEmptyReturnsEmpty() {
        TestObserver<Object> s = new TestObserver<Object>();
        Observable.empty().takeLast(1).subscribe(s);
        s.assertNoValues();
        s.assertNoErrors();
        s.assertTerminated();
        // NO longer assertable
//      s.assertUnsubscribed();
    }

    @Test
    public void testLastOfOneReturnsLast() {
        TestObserver<Integer> s = new TestObserver<Integer>();
        Observable.just(1).takeLast(1).subscribe(s);
        s.assertValue(1);
        s.assertNoErrors();
        s.assertTerminated();
        // NO longer assertable
//      s.assertUnsubscribed();
    }

    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Function0 unsubscribeAction = new Function0() {
            @Override
            public kotlin.Unit invoke() {
                unsubscribed.set(true);
                return Unit.INSTANCE;
            }
        };
        Observable.just(1)
        .concatWith(Observable.<Integer>never())
        .doOnDispose(unsubscribeAction)
        .takeLast(1)
        .subscribe()
        .dispose();

        assertTrue(unsubscribed.get());
    }

    @Test
    public void testTakeLastZeroProcessesAllItemsButIgnoresThem() {
        final AtomicInteger upstreamCount = new AtomicInteger();
        final int num = 10;
        long count = Observable.range(1, num).doOnNext(new Function1<Integer, kotlin.Unit>() {

            @Override
            public Unit invoke(Integer t) {
                upstreamCount.incrementAndGet();
                return Unit.INSTANCE;
            }})
            .takeLast(0).count().blockingGet();
        assertEquals(num, upstreamCount.get());
        assertEquals(0L, count);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).takeLast(1));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> f) throws Exception {
                return f.takeLast(1);
            }
        });
    }
}
