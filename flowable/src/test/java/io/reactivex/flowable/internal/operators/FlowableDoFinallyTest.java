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
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Action;
import io.reactivex.common.functions.Consumer;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.processors.UnicastProcessor;
import io.reactivex.flowable.subscribers.SubscriberFusion;
import io.reactivex.flowable.subscribers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FlowableDoFinallyTest implements Action {

    int calls;

    @Override
    public void invoke() {
        calls++;
    }

    @Test
    public void normalJust() {
        Flowable.just(1)
        .doFinally(this)
        .test()
        .assertResult(1);

        assertEquals(1, calls);
    }

    @Test
    public void normalEmpty() {
        Flowable.empty()
        .doFinally(this)
        .test()
        .assertResult();

        assertEquals(1, calls);
    }

    @Test
    public void normalError() {
        Flowable.error(new TestException())
        .doFinally(this)
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void normalTake() {
        Flowable.range(1, 10)
        .doFinally(this)
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.doFinally(FlowableDoFinallyTest.this);
            }
        });
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.doFinally(FlowableDoFinallyTest.this).filter(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void syncFused() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.SYNC);

        Flowable.range(1, 5)
        .doFinally(this)
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedBoundary() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.SYNC | FusedQueueSubscription.BOUNDARY);

        Flowable.range(1, 5)
        .doFinally(this)
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFused() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ASYNC);

        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedBoundary() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ASYNC | FusedQueueSubscription.BOUNDARY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }


    @Test
    public void normalJustConditional() {
        Flowable.just(1)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult(1);

        assertEquals(1, calls);
    }

    @Test
    public void normalEmptyConditional() {
        Flowable.empty()
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult();

        assertEquals(1, calls);
    }

    @Test
    public void normalErrorConditional() {
        Flowable.error(new TestException())
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void normalTakeConditional() {
        Flowable.range(1, 10)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedConditional() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.SYNC);

        Flowable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void nonFused() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.SYNC);

        Flowable.range(1, 5).hide()
        .doFinally(this)
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void nonFusedConditional() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.SYNC);

        Flowable.range(1, 5).hide()
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedBoundaryConditional() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.SYNC | FusedQueueSubscription.BOUNDARY);

        Flowable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedConditional() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ASYNC);

        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedBoundaryConditional() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(FusedQueueSubscription.ASYNC | FusedQueueSubscription.BOUNDARY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test(expected = NullPointerException.class)
    public void nullAction() {
        Flowable.just(1).doFinally(null);
    }

    @Test
    public void actionThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .doFinally(new Action() {
                @Override
                public void invoke() {
                    throw new TestException();
                }
            })
            .test()
            .assertResult(1)
            .cancel();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void actionThrowsConditional() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .doFinally(new Action() {
                @Override
                public void invoke() {
                    throw new TestException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertResult(1)
            .cancel();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void clearIsEmpty() {
        Flowable.range(1, 5)
        .doFinally(this)
        .subscribe(new RelaxedSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {
                @SuppressWarnings("unchecked")
                FusedQueueSubscription<Integer> qs = (FusedQueueSubscription<Integer>)s;

                qs.requestFusion(FusedQueueSubscription.ANY);

                assertFalse(qs.isEmpty());

                try {
                    assertEquals(1, qs.poll().intValue());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }

                assertFalse(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                qs.cancel();
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(1, calls);
    }

    @Test
    public void clearIsEmptyConditional() {
        Flowable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(new RelaxedSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {
                @SuppressWarnings("unchecked")
                FusedQueueSubscription<Integer> qs = (FusedQueueSubscription<Integer>)s;

                qs.requestFusion(FusedQueueSubscription.ANY);

                assertFalse(qs.isEmpty());

                try {
                    assertEquals(1, qs.poll().intValue());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }

                assertFalse(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                qs.cancel();
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(1, calls);
    }

    @Test
    public void eventOrdering() {
        final List<String> list = new ArrayList<String>();

        Flowable.error(new TestException())
        .doOnCancel(new Action() {
            @Override
            public void invoke() {
                list.add("cancel");
            }
        })
        .doFinally(new Action() {
            @Override
            public void invoke() {
                list.add("finally");
            }
        })
        .subscribe(
                new Consumer<Object>() {
                    @Override
                    public void accept(Object v) throws Exception {
                        list.add("onNext");
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) throws Exception {
                        list.add("onError");
                    }
                },
                new Action() {
                    @Override
                    public void invoke() {
                        list.add("onComplete");
                    }
                });

        assertEquals(Arrays.asList("onError", "finally"), list);
    }

    @Test
    public void eventOrdering2() {
        final List<String> list = new ArrayList<String>();

        Flowable.just(1)
        .doOnCancel(new Action() {
            @Override
            public void invoke() {
                list.add("cancel");
            }
        })
        .doFinally(new Action() {
            @Override
            public void invoke() {
                list.add("finally");
            }
        })
        .subscribe(
                new Consumer<Object>() {
                    @Override
                    public void accept(Object v) throws Exception {
                        list.add("onNext");
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) throws Exception {
                        list.add("onError");
                    }
                },
                new Action() {
                    @Override
                    public void invoke() {
                        list.add("onComplete");
                    }
                });

        assertEquals(Arrays.asList("onNext", "onComplete", "finally"), list);
    }
}
