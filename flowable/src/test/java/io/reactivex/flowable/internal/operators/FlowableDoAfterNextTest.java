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

package io.reactivex.flowable.internal.operators;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.processors.UnicastProcessor;
import io.reactivex.flowable.subscribers.SubscriberFusion;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlowableDoAfterNextTest {

    final List<Integer> values = new ArrayList<Integer>();

    final Function1<Integer, kotlin.Unit> afterNext = new Function1<Integer, kotlin.Unit>() {
        @Override
        public Unit invoke(Integer e) {
            values.add(-e);
            return Unit.INSTANCE;
        }
    };

    final TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
        @Override
        public void onNext(Integer t) {
            super.onNext(t);
            FlowableDoAfterNextTest.this.values.add(t);
        }
    };

    @Test
    public void just() {
        Flowable.just(1)
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void range() {
        Flowable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1, -1, 2, -2, 3, -3, 4, -4, 5, -5), values);
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new TestException())
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .doAfterNext(afterNext)
        .subscribeWith(ts)
        .assertResult();

        assertTrue(values.isEmpty());
    }

    @Test
    public void syncFused() {
        TestSubscriber<Integer> ts0 = SubscriberFusion.newTest(FusedQueueSubscription.SYNC);

        Flowable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribe(ts0);

        SubscriberFusion.assertFusion(ts0, FusedQueueSubscription.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedRejected() {
        TestSubscriber<Integer> ts0 = SubscriberFusion.newTest(FusedQueueSubscription.ASYNC);

        Flowable.range(1, 5)
        .doAfterNext(afterNext)
        .subscribe(ts0);

        SubscriberFusion.assertFusion(ts0, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFused() {
        TestSubscriber<Integer> ts0 = SubscriberFusion.newTest(FusedQueueSubscription.ASYNC);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doAfterNext(afterNext)
        .subscribe(ts0);

        SubscriberFusion.assertFusion(ts0, FusedQueueSubscription.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test(expected = NullPointerException.class)
    public void consumerNull() {
        Flowable.just(1).doAfterNext(null);
    }

    @Test
    public void justConditional() {
        Flowable.just(1)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void rangeConditional() {
        Flowable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1, -1, 2, -2, 3, -3, 4, -4, 5, -5), values);
    }

    @Test
    public void errorConditional() {
        Flowable.<Integer>error(new TestException())
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void emptyConditional() {
        Flowable.<Integer>empty()
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult();

        assertTrue(values.isEmpty());
    }

    @Test
    public void syncFusedConditional() {
        TestSubscriber<Integer> ts0 = SubscriberFusion.newTest(FusedQueueSubscription.SYNC);

        Flowable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        SubscriberFusion.assertFusion(ts0, FusedQueueSubscription.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedRejectedConditional() {
        TestSubscriber<Integer> ts0 = SubscriberFusion.newTest(FusedQueueSubscription.ASYNC);

        Flowable.range(1, 5)
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        SubscriberFusion.assertFusion(ts0, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void asyncFusedConditional() {
        TestSubscriber<Integer> ts0 = SubscriberFusion.newTest(FusedQueueSubscription.ASYNC);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doAfterNext(afterNext)
        .filter(Functions.alwaysTrue())
        .subscribe(ts0);

        SubscriberFusion.assertFusion(ts0, FusedQueueSubscription.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(-1, -2, -3, -4, -5), values);
    }

    @Test
    public void consumerThrows() {
        Flowable.just(1, 2)
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
        Flowable.just(1, 2)
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
        Flowable.just(1, 2).hide()
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
