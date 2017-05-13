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
import org.reactivestreams.Subscription;

import java.util.List;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlowableDoOnLifecycleTest {

    @Test
    public void onSubscribeCrashed() {
        Flowable.just(1)
                .doOnLifecycle(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                throw new TestException();
            }
        }, Functions.EMPTY_LONG_CONSUMER, Functions.EMPTY_ACTION)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        final int[] calls = { 0, 0 };

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> o) throws Exception {
                return o
                        .doOnLifecycle(new Function1<Subscription, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Subscription s) {
                        calls[0]++;
                        return Unit.INSTANCE;
                    }
                }, Functions.EMPTY_LONG_CONSUMER, new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        calls[1]++;
                        return Unit.INSTANCE;
                    }
                });
            }
        });

        assertEquals(2, calls[0]);
        assertEquals(0, calls[1]);
    }

    @Test
    public void dispose() {
        final int[] calls = { 0, 0 };

        TestHelper.checkDisposed(Flowable.just(1)
                .doOnLifecycle(new Function1<Subscription, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Subscription s) {
                        calls[0]++;
                        return Unit.INSTANCE;
                    }
                }, Functions.EMPTY_LONG_CONSUMER, new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        calls[1]++;
                        return Unit.INSTANCE;
                    }
                })
            );

        assertEquals(1, calls[0]);
        assertEquals(2, calls[1]);
    }

    @Test
    public void requestCrashed() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .doOnLifecycle(Functions.emptyConsumer(),
                    new Function1<Long, Unit>() {
                        @Override
                        public Unit invoke(Long v) {
                            throw new TestException();
                        }
                    },
                    Functions.EMPTY_ACTION)
            .test()
            .assertResult(1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void cancelCrashed() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .doOnLifecycle(Functions.emptyConsumer(),
                    Functions.EMPTY_LONG_CONSUMER,
                    new Function0() {
                        @Override
                        public kotlin.Unit invoke() {
                            throw new TestException();
                        }
                    })
            .take(1)
            .test()
            .assertResult(1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            final BooleanSubscription bs = new BooleanSubscription();

            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(bs);
                    s.onError(new TestException("Second"));
                    s.onComplete();
                }
            }
                    .doOnSubscribe(new Function1<Subscription, kotlin.Unit>() {
                @Override
                public Unit invoke(Subscription s) {
                    throw new TestException("First");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isCancelled());

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
