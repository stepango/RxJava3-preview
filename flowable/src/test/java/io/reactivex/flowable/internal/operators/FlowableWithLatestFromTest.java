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
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import io.reactivex.common.internal.utils.CrashingMappedIterable;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function4;
import kotlin.jvm.functions.Function5;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class FlowableWithLatestFromTest {
    static final Function2<Integer, Integer, Integer> COMBINER = new Function2<Integer, Integer, Integer>() {
        @Override
        public Integer invoke(Integer t1, Integer t2) {
            return (t1 << 8) + t2;
        }
    };
    static final Function2<Integer, Integer, Integer> COMBINER_ERROR = new Function2<Integer, Integer, Integer>() {
        @Override
        public Integer invoke(Integer t1, Integer t2) {
            throw new TestException("Forced failure");
        }
    };
    @Test
    public void testSimple() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Subscriber<Integer> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        result.subscribe(o);

        source.onNext(1);
        inOrder.verify(o, never()).onNext(anyInt());

        other.onNext(1);
        inOrder.verify(o, never()).onNext(anyInt());

        source.onNext(2);
        inOrder.verify(o).onNext((2 << 8) + 1);

        other.onNext(2);
        inOrder.verify(o, never()).onNext(anyInt());

        other.onComplete();
        inOrder.verify(o, never()).onComplete();

        source.onNext(3);
        inOrder.verify(o).onNext((3 << 8) + 2);

        source.onComplete();
        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testEmptySource() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);

        source.onComplete();

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertNoValues();

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void testEmptyOther() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        source.onNext(1);

        source.onComplete();

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertNoValues();

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }


    @Test
    public void testUnsubscription() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);

        ts.dispose();

        ts.assertValue((1 << 8) + 1);
        ts.assertNoErrors();
        ts.assertNotComplete();

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void testSourceThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);

        source.onError(new TestException());

        ts.assertTerminated();
        ts.assertValue((1 << 8) + 1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }
    @Test
    public void testOtherThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);

        other.onError(new TestException());

        ts.assertTerminated();
        ts.assertValue((1 << 8) + 1);
        ts.assertNotComplete();
        ts.assertError(TestException.class);

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void testFunctionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER_ERROR);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);

        ts.assertTerminated();
        ts.assertNotComplete();
        ts.assertNoValues();
        ts.assertError(TestException.class);

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void testNoDownstreamUnsubscribe() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        source.onComplete();

        assertFalse(ts.isCancelled());
    }

    @Test
    public void testBackpressure() {
        Flowable<Integer> source = Flowable.range(1, 10);
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        result.subscribe(ts);

        assertTrue("Other has no observers!", other.hasSubscribers());

        ts.request(1);

        assertTrue("Other has no observers!", other.hasSubscribers());

        ts.assertNoValues();

        other.onNext(1);

        ts.request(1);

        ts.assertValue((2 << 8) + 1);

        ts.request(5);
        ts.assertValues(
                (2 << 8) + 1, (3 << 8) + 1, (4 << 8) + 1, (5 << 8) + 1,
                (6 << 8) + 1, (7 << 8) + 1
        );

        ts.dispose();

        assertFalse("Other has observers!", other.hasSubscribers());

        ts.assertNoErrors();
    }

    static final Function1<Object[], String> toArray = new Function1<Object[], String>() {
        @Override
        public String invoke(Object[] args) {
            return Arrays.toString(args);
        }
    };

    @Test
    public void manySources() {
        PublishProcessor<String> ps1 = PublishProcessor.create();
        PublishProcessor<String> ps2 = PublishProcessor.create();
        PublishProcessor<String> ps3 = PublishProcessor.create();
        PublishProcessor<String> main = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<String>();

        main.withLatestFrom(new Flowable[] { ps1, ps2, ps3 }, toArray)
        .subscribe(ts);

        main.onNext("1");
        ts.assertNoValues();
        ps1.onNext("a");
        ts.assertNoValues();
        ps2.onNext("A");
        ts.assertNoValues();
        ps3.onNext("=");
        ts.assertNoValues();

        main.onNext("2");
        ts.assertValues("[2, a, A, =]");

        ps2.onNext("B");

        ts.assertValues("[2, a, A, =]");

        ps3.onComplete();
        ts.assertValues("[2, a, A, =]");

        ps1.onNext("b");

        main.onNext("3");

        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onComplete();
        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", ps1.hasSubscribers());
        assertFalse("ps2 has subscribers?", ps2.hasSubscribers());
        assertFalse("ps3 has subscribers?", ps3.hasSubscribers());
    }

    @Test
    public void manySourcesIterable() {
        PublishProcessor<String> ps1 = PublishProcessor.create();
        PublishProcessor<String> ps2 = PublishProcessor.create();
        PublishProcessor<String> ps3 = PublishProcessor.create();
        PublishProcessor<String> main = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<String>();

        main.withLatestFrom(Arrays.<Flowable<?>>asList(ps1, ps2, ps3), toArray)
        .subscribe(ts);

        main.onNext("1");
        ts.assertNoValues();
        ps1.onNext("a");
        ts.assertNoValues();
        ps2.onNext("A");
        ts.assertNoValues();
        ps3.onNext("=");
        ts.assertNoValues();

        main.onNext("2");
        ts.assertValues("[2, a, A, =]");

        ps2.onNext("B");

        ts.assertValues("[2, a, A, =]");

        ps3.onComplete();
        ts.assertValues("[2, a, A, =]");

        ps1.onNext("b");

        main.onNext("3");

        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onComplete();
        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", ps1.hasSubscribers());
        assertFalse("ps2 has subscribers?", ps2.hasSubscribers());
        assertFalse("ps3 has subscribers?", ps3.hasSubscribers());
    }

    @Test
    public void manySourcesIterableSweep() {
        for (String val : new String[] { "1" /*, null*/ }) {
            int n = 35;
            for (int i = 0; i < n; i++) {
                List<Flowable<?>> sources = new ArrayList<Flowable<?>>();
                List<String> expected = new ArrayList<String>();
                expected.add(val);

                for (int j = 0; j < i; j++) {
                    sources.add(Flowable.just(val));
                    expected.add(String.valueOf(val));
                }

                TestSubscriber<String> ts = new TestSubscriber<String>();

                PublishProcessor<String> main = PublishProcessor.create();

                main.withLatestFrom(sources, toArray).subscribe(ts);

                ts.assertNoValues();

                main.onNext(val);
                main.onComplete();

                ts.assertValue(expected.toString());
                ts.assertNoErrors();
                ts.assertComplete();
            }
        }
    }

    @Test
    public void backpressureNoSignal() {
        PublishProcessor<String> ps1 = PublishProcessor.create();
        PublishProcessor<String> ps2 = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Flowable.range(1, 10).withLatestFrom(new Flowable<?>[] { ps1, ps2 }, toArray)
        .subscribe(ts);

        ts.assertNoValues();

        ts.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", ps1.hasSubscribers());
        assertFalse("ps2 has subscribers?", ps2.hasSubscribers());
    }

    @Test
    public void backpressureWithSignal() {
        PublishProcessor<String> ps1 = PublishProcessor.create();
        PublishProcessor<String> ps2 = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Flowable.range(1, 3).withLatestFrom(new Flowable<?>[] { ps1, ps2 }, toArray)
        .subscribe(ts);

        ts.assertNoValues();

        ps1.onNext("1");
        ps2.onNext("1");

        ts.request(1);

        ts.assertValue("[1, 1, 1]");

        ts.request(1);

        ts.assertValues("[1, 1, 1]", "[2, 1, 1]");

        ts.request(1);

        ts.assertValues("[1, 1, 1]", "[2, 1, 1]", "[3, 1, 1]");
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", ps1.hasSubscribers());
        assertFalse("ps2 has subscribers?", ps2.hasSubscribers());
    }

    @Test
    public void withEmpty() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Flowable.range(1, 3).withLatestFrom(
                new Flowable<?>[] { Flowable.just(1), Flowable.empty() }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void withError() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Flowable.range(1, 3).withLatestFrom(
                new Flowable<?>[] { Flowable.just(1), Flowable.error(new TestException()) }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void withMainError() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Flowable.error(new TestException()).withLatestFrom(
                new Flowable<?>[] { Flowable.just(1), Flowable.just(1) }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void with2Others() {
        Flowable<Integer> just = Flowable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, new Function3<Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(Integer a, Integer b, Integer c) {
                return Arrays.asList(a, b, c);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void with3Others() {
        Flowable<Integer> just = Flowable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, just, new Function4<Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(Integer a, Integer b, Integer c, Integer d) {
                return Arrays.asList(a, b, c, d);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void with4Others() {
        Flowable<Integer> just = Flowable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, just, just, new Function5<Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(Integer a, Integer b, Integer c, Integer d, Integer e) {
                return Arrays.asList(a, b, c, d, e);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).withLatestFrom(Flowable.just(2), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return a;
            }
        }));

        TestHelper.checkDisposed(Flowable.just(1).withLatestFrom(Flowable.just(2), Flowable.just(3), new Function3<Integer, Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b, Integer c) {
                return a;
            }
        }));
    }

    @Test
    public void manyIteratorThrows() {
        Flowable.just(1)
                .withLatestFrom(new CrashingMappedIterable<Flowable<Integer>>(1, 100, 100, new Function1<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Integer v) {
                return Flowable.just(2);
            }
                }), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return a;
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void manyCombinerThrows() {
        Flowable.just(1).withLatestFrom(Flowable.just(2), Flowable.just(3), new Function3<Integer, Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b, Integer c) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void manyErrors() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onError(new TestException("First"));
                    observer.onNext(1);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }.withLatestFrom(Flowable.just(2), Flowable.just(3), new Function3<Integer, Integer, Integer, Object>() {
                @Override
                public Object invoke(Integer a, Integer b, Integer c) {
                    return a;
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void otherErrors() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .withLatestFrom(new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new TestException("First"));
                    s.onError(new TestException("Second"));
                }
            }, new Function2<Integer, Integer, Integer>() {
                @Override
                public Integer invoke(Integer a, Integer b) {
                    return a + b;
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void combineToNull1() {
        Flowable.just(1)
        .withLatestFrom(Flowable.just(2), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void combineToNull2() {
        Flowable.just(1)
                .withLatestFrom(Arrays.asList(Flowable.just(2), Flowable.just(3)), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] o) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }
}
