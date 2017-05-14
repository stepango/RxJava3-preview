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

import java.util.concurrent.Callable;

import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.internal.operators.FlowableMapNotification.MapNotificationSubscriber;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.jvm.functions.Function1;

public class FlowableMapNotificationTest {
    @Test
    public void testJust() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        Flowable.just(1)
        .flatMap(
                new Function1<Integer, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> invoke(Integer item) {
                        return Flowable.just((Object)(item + 1));
                    }
                },
                new Function1<Throwable, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> invoke(Throwable e) {
                        return Flowable.error(e);
                    }
                },
                new Callable<Flowable<Object>>() {
                    @Override
                    public Flowable<Object> call() {
                        return Flowable.never();
                    }
                }
        ).subscribe(ts);

        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValue(2);
    }

    @Test
    public void backpressure() {
        TestSubscriber<Object> ts = TestSubscriber.create(0L);

        new FlowableMapNotification<Integer, Integer>(Flowable.range(1, 3),
                new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer item) {
                        return item + 1;
                    }
                },
                new Function1<Throwable, Integer>() {
                    @Override
                    public Integer invoke(Throwable e) {
                        return 0;
                    }
                },
                new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        return 5;
                    }
                }
        ).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(3);

        ts.assertValues(2, 3, 4);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1);

        ts.assertValues(2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void noBackpressure() {
        TestSubscriber<Object> ts = TestSubscriber.create(0L);

        PublishProcessor<Integer> ps = PublishProcessor.create();

        new FlowableMapNotification<Integer, Integer>(ps,
                new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer item) {
                        return item + 1;
                    }
                },
                new Function1<Throwable, Integer>() {
                    @Override
                    public Integer invoke(Throwable e) {
                        return 0;
                    }
                },
                new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        return 5;
                    }
                }
        ).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ps.onNext(1);
        ps.onNext(2);
        ps.onNext(3);
        ps.onComplete();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1);

        ts.assertValue(0);
        ts.assertNoErrors();
        ts.assertComplete();

    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(new Flowable<Integer>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected void subscribeActual(Subscriber<? super Integer> observer) {
                MapNotificationSubscriber mn = new MapNotificationSubscriber(
                        observer,
                        Functions.justFunction(Flowable.just(1)),
                        Functions.justFunction(Flowable.just(2)),
                        Functions.justCallable(Flowable.just(3))
                );
                mn.onSubscribe(new BooleanSubscription());
            }
        });
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function1<Flowable<Object>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Flowable<Object> o) {
                return o.flatMap(
                        Functions.justFunction(Flowable.just(1)),
                        Functions.justFunction(Flowable.just(2)),
                        Functions.justCallable(Flowable.just(3))
                );
            }
        });
    }
}
