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
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.MissingBackpressureException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.common.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.ParallelFlowable;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParallelRunOnTest {

    @Test
    public void subscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .runOn(Schedulers.computation()));
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .runOn(ImmediateThinScheduler.INSTANCE)
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertFalse(errors.isEmpty());
            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void conditionalPath() {
        Flowable.range(1, 1000)
        .parallel(2)
        .runOn(Schedulers.computation())
                .filter(new kotlin.jvm.functions.Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(500)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void missingBackpressure() {
        new ParallelFlowable<Integer>() {
            @Override
            public int parallelism() {
                return 1;
            }

            @Override
            public void subscribe(Subscriber<? super Integer>[] subscribers) {
                subscribers[0].onSubscribe(new BooleanSubscription());
                subscribers[0].onNext(1);
                subscribers[0].onNext(2);
                subscribers[0].onNext(3);
            }
        }
        .runOn(ImmediateThinScheduler.INSTANCE, 1)
        .sequential(1)
        .test(0)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorBackpressured() {
        Flowable.error(new TestException())
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .sequential(1)
        .test(0)
        .assertFailure(TestException.class);
    }

    @Test
    public void errorConditional() {
        Flowable.error(new TestException())
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorConditionalBackpressured() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>(0L);

        Flowable.error(new TestException())
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .subscribe(new Subscriber[] { ts });

        ts
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyConditionalBackpressured() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>(0L);

        Flowable.empty()
        .parallel(1)
        .runOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .subscribe(new Subscriber[] { ts });

        ts
        .assertResult();
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = pp.parallel(1)
            .runOn(Schedulers.computation())
            .sequential()
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nextCancelRaceBackpressured() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = TestSubscriber.create(0L);

            pp.parallel(1)
            .runOn(Schedulers.computation())
            .subscribe(new Subscriber[] { ts });

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void nextCancelRaceConditional() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = pp.parallel(1)
            .runOn(Schedulers.computation())
            .filter(Functions.alwaysTrue())
            .sequential()
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nextCancelRaceBackpressuredConditional() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = TestSubscriber.create(0L);

            pp.parallel(1)
            .runOn(Schedulers.computation())
            .filter(Functions.alwaysTrue())
            .subscribe(new Subscriber[] { ts });

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }
}
