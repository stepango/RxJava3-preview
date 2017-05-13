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
import java.util.Arrays;
import java.util.List;

import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.QueueDisposable;
import io.reactivex.observable.observers.ObserverFusion;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.UnicastSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObservableDoAfterNextTest {

    final List<Integer> values = new ArrayList<Integer>();

    final Function1<Integer, kotlin.Unit> afterNext = new Function1<Integer, kotlin.Unit>() {
        @Override
        public Unit invoke(Integer e) {
            values.add(-e);
            return Unit.INSTANCE;
        }
    };

    final TestObserver<Integer> ts = new TestObserver<Integer>() {
        @Override
        public void onNext(Integer t) {
            super.onNext(t);
            ObservableDoAfterNextTest.this.values.add(t);
        }
    };

    @Test
    public void just() {
        Observable.just(1)
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void range() {
        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1, -1, 2, -2, 3, -3, 4, -4, 5, -5), values);
    }

    @Test
    public void error() {
        Observable.<Integer>error(new TestException())
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void empty() {
        Observable.<Integer>empty()
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult();

        assertTrue(values.isEmpty());
    }

    @Test
    public void syncFused() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedRejected() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueDisposable.ASYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFused() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueDisposable.ASYNC);

        UnicastSubject<Integer> up = UnicastSubject.create();

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doAfterNext(afterNext)
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test(expected = NullPointerException.class)
    public void consumerNull() {
        Observable.just(1).doAfterNext(null);
    }

    @Test
    public void justConditional() {
        Observable.just(1)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void rangeConditional() {
        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1, -1, 2, -2, 3, -3, 4, -4, 5, -5), values);
    }

    @Test
    public void errorConditional() {
        Observable.<Integer>error(new TestException())
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void emptyConditional() {
        Observable.<Integer>empty()
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult();

        assertTrue(values.isEmpty());
    }

    @Test
    public void syncFusedConditional() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedRejectedConditional() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueDisposable.ASYNC);

        Observable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedConditional() {
        TestObserver<Integer> ts0 = ObserverFusion.newTest(QueueDisposable.ASYNC);

        UnicastSubject<Integer> up = UnicastSubject.create();

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        ObserverFusion.assertFusion(ts0, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void consumerThrows() {
        Observable.just(1, 2)
                .doAfterNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer e) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void consumerThrowsConditional() {
        Observable.just(1, 2)
                .doAfterNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer e) {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void consumerThrowsConditional2() {
        Observable.just(1, 2).hide()
                .doAfterNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer e) {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class, 1);
    }
}
