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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.Callable;

import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.EmptySubscription;
import io.reactivex.flowable.internal.subscriptions.ScalarSubscription;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FlowableScalarXMapTest {

    @Test
    public void utilityClass() {
        TestCommonHelper.checkUtilityClass(FlowableScalarXMap.class);
    }

    static final class CallablePublisher implements Publisher<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            EmptySubscription.error(new TestException(), s);
        }

        @Override
        public Integer call() throws Exception {
            throw new TestException();
        }
    }

    static final class EmptyCallablePublisher implements Publisher<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            EmptySubscription.complete(s);
        }

        @Override
        public Integer call() throws Exception {
            return null;
        }
    }

    static final class OneCallablePublisher implements Publisher<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            s.onSubscribe(new ScalarSubscription<Integer>(s, 1));
        }

        @Override
        public Integer call() throws Exception {
            return 1;
        }
    }

    @Test
    public void tryScalarXMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new CallablePublisher(), ts, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer f) {
                return Flowable.just(1);
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void emptyXMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new EmptyCallablePublisher(), ts, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer f) {
                return Flowable.just(1);
            }
        }));

        ts.assertResult();
    }

    @Test
    public void mapperCrashes() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer f) {
                throw new TestException();
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void mapperToJust() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer f) {
                return Flowable.just(1);
            }
        }));

        ts.assertResult(1);
    }

    @Test
    public void mapperToEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer f) {
                return Flowable.empty();
            }
        }));

        ts.assertResult();
    }

    @Test
    public void mapperToCrashingCallable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer f) {
                return new CallablePublisher();
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void scalarMapToEmpty() {
        FlowableScalarXMap.scalarXMap(1, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return Flowable.empty();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void scalarMapToCrashingCallable() {
        FlowableScalarXMap.scalarXMap(1, new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return new CallablePublisher();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void scalarDisposableStateCheck() {
        TestSubscriber<Integer> to = new TestSubscriber<Integer>();
        ScalarSubscription<Integer> sd = new ScalarSubscription<Integer>(to, 1);
        to.onSubscribe(sd);

        assertFalse(sd.isCancelled());

        assertTrue(sd.isEmpty());

        sd.request(1);

        assertFalse(sd.isCancelled());

        assertTrue(sd.isEmpty());

        to.assertResult(1);

        try {
            sd.offer(1);
            fail("Should have thrown");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    @Test
    public void scalarDisposableRunDisposeRace() {
        for (int i = 0; i < 500; i++) {
            TestSubscriber<Integer> to = new TestSubscriber<Integer>();
            final ScalarSubscription<Integer> sd = new ScalarSubscription<Integer>(to, 1);
            to.onSubscribe(sd);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    sd.request(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    sd.cancel();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void cancelled() {
        ScalarSubscription<Integer> scalar = new ScalarSubscription<Integer>(new TestSubscriber<Integer>(), 1);

        assertFalse(scalar.isCancelled());

        scalar.cancel();

        assertTrue(scalar.isCancelled());
    }
}
