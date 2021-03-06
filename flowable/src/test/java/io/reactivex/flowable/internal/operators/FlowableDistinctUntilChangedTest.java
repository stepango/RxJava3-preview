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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.processors.UnicastProcessor;
import io.reactivex.flowable.subscribers.SubscriberFusion;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableDistinctUntilChangedTest {

    Subscriber<String> w;
    Subscriber<String> w2;

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
        w = TestHelper.mockSubscriber();
        w2 = TestHelper.mockSubscriber();
    }

    @Test
    public void testDistinctUntilChangedOfNone() {
        Flowable<String> src = Flowable.empty();
        src.distinctUntilChanged().subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testDistinctUntilChangedOfNoneWithKeySelector() {
        Flowable<String> src = Flowable.empty();
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testDistinctUntilChangedOfNormalSource() {
        Flowable<String> src = Flowable.just("a", "b", "c", "c", "c", "b", "b", "a", "e");
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
        Flowable<String> src = Flowable.just("a", "b", "c", "C", "c", "B", "b", "a", "e");
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
        Flowable<String> src = Flowable.just(null, "a", "a", null, null, "b", null, null);
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
        Flowable<String> src = Flowable.just("a", "b", null, "c");
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        verify(w, times(1)).onError(any(NullPointerException.class));
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, never()).onComplete();
    }

    @Test
    public void directComparer() {
        Flowable.fromArray(1, 2, 2, 3, 2, 4, 1, 1, 2)
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .test()
        .assertResult(1, 2, 3, 2, 4, 1, 2);
    }

    @Test
    public void directComparerConditional() {
        Flowable.fromArray(1, 2, 2, 3, 2, 4, 1, 1, 2)
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                return a.equals(b);
            }
        })
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        })
        .test()
        .assertResult(1, 2, 3, 2, 4, 1, 2);
    }

    @Test
    public void directComparerFused() {
        Flowable.fromArray(1, 2, 2, 3, 2, 4, 1, 1, 2)
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .to(SubscriberFusion.<Integer>test(Long.MAX_VALUE, FusedQueueSubscription.ANY, false))
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(FusedQueueSubscription.SYNC))
        .assertResult(1, 2, 3, 2, 4, 1, 2);
    }

    @Test
    public void directComparerConditionalFused() {
        Flowable.fromArray(1, 2, 2, 3, 2, 4, 1, 1, 2)
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                return a.equals(b);
            }
        })
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        })
        .to(SubscriberFusion.<Integer>test(Long.MAX_VALUE, FusedQueueSubscription.ANY, false))
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(FusedQueueSubscription.SYNC))
        .assertResult(1, 2, 3, 2, 4, 1, 2);
    }

    private static final Function1<String, String> THROWS_NON_FATAL = new Function1<String, String>() {
        @Override
        public String invoke(String s) {
            throw new RuntimeException();
        }
    };

    @Test
    public void testDistinctUntilChangedWhenNonFatalExceptionThrownByKeySelectorIsNotReportedByUpstream() {
        Flowable<String> src = Flowable.just("a", "b", "null", "c");
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);
        src
                .doOnError(new Function1<Throwable, kotlin.Unit>() {
                @Override
                public Unit invoke(Throwable t) {
                    errorOccurred.set(true);
                    return Unit.INSTANCE;
                }
            })
          .distinctUntilChanged(THROWS_NON_FATAL)
          .subscribe(w);
        Assert.assertFalse(errorOccurred.get());
    }

    @Test
    public void customComparator() {
        Flowable<String> source = Flowable.just("a", "b", "B", "A","a", "C");

        TestSubscriber<String> ts = TestSubscriber.create();

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
        Flowable<String> source = Flowable.just("a", "b", "B", "A","a", "C");

        TestSubscriber<String> ts = TestSubscriber.create();

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
        TestSubscriber<Integer> to = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        Flowable.just(1, 2, 2, 3, 3, 4, 5)
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .subscribe(to);

        to.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(FusedQueueSubscription.SYNC))
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void fusedAsync() {
        TestSubscriber<Integer> to = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                return a.equals(b);
            }
        })
        .subscribe(to);

        TestHelper.emit(up, 1, 2, 2, 3, 3, 4, 5);

        to.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(FusedQueueSubscription.ASYNC))
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void ignoreCancel() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            new Flowable<Integer>() {
                @Override
                public void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onNext(3);
                    s.onError(new IOException());
                    s.onComplete();
                }
            }
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

        PublishProcessor<Mutable> pp = PublishProcessor.create();

        TestSubscriber<Mutable> ts = pp.distinctUntilChanged(new Function1<Mutable, Object>() {
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

    @Test
    public void conditionalNormal() {
        Flowable.just(1, 2, 1, 3, 3, 4, 3, 5, 5)
        .distinctUntilChanged()
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
        .test()
        .assertResult(2, 4);
    }

    @Test
    public void conditionalNormal2() {
        Flowable.just(1, 2, 1, 3, 3, 4, 3, 5, 5).hide()
        .distinctUntilChanged()
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
        .test()
        .assertResult(2, 4);
    }

    @Test
    public void conditionalNormal3() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestSubscriber<Integer> ts = up.hide()
        .distinctUntilChanged()
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
        .test();

        TestHelper.emit(up, 1, 2, 1, 3, 3, 4, 3, 5, 5);

        ts
        .assertResult(2, 4);
    }

    @Test
    public void conditionalSelectorCrash() {
        Flowable.just(1, 2, 1, 3, 3, 4, 3, 5, 5)
                .distinctUntilChanged(new Function2<Integer, Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer a, Integer b) {
                throw new TestException();
            }
        })
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void conditionalFused() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        Flowable.just(1, 2, 1, 3, 3, 4, 3, 5, 5)
        .distinctUntilChanged()
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.SYNC)
        .assertResult(2, 4);
    }

    @Test
    public void conditionalAsyncFused() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ANY);
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .distinctUntilChanged()
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);


        TestHelper.emit(up, 1, 2, 1, 3, 3, 4, 3, 5, 5);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.ASYNC)
        .assertResult(2, 4);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Integer>, Object>() {
            @Override
            public Object invoke(Flowable<Integer> f) {
                return f.distinctUntilChanged().filter(Functions.alwaysTrue());
            }
        }, false, 1, 1, 1);
    }
}
