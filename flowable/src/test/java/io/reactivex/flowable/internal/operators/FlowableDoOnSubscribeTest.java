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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;

public class FlowableDoOnSubscribeTest {

    @Test
    public void testDoOnSubscribe() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> o = Flowable.just(1).doOnSubscribe(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                    count.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(3, count.get());
    }

    @Test
    public void testDoOnSubscribe2() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> o = Flowable.just(1).doOnSubscribe(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                    count.incrementAndGet();
                return Unit.INSTANCE;
            }
        }).take(1).doOnSubscribe(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                    count.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        o.subscribe();
        assertEquals(2, count.get());
    }

    @Test
    public void testDoOnUnSubscribeWorksWithRefCount() throws Exception {
        final AtomicInteger onSubscribed = new AtomicInteger();
        final AtomicInteger countBefore = new AtomicInteger();
        final AtomicInteger countAfter = new AtomicInteger();
        final AtomicReference<Subscriber<? super Integer>> sref = new AtomicReference<Subscriber<? super Integer>>();
        Flowable<Integer> o = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                onSubscribed.incrementAndGet();
                sref.set(s);
            }

        }).doOnSubscribe(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                    countBefore.incrementAndGet();
                return Unit.INSTANCE;
            }
        }).publish().refCount()
                .doOnSubscribe(new Function1<Subscription, kotlin.Unit>() {
            @Override
            public Unit invoke(Subscription s) {
                    countAfter.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(1, countBefore.get());
        assertEquals(1, onSubscribed.get());
        assertEquals(3, countAfter.get());
        sref.get().onComplete();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(2, countBefore.get());
        assertEquals(2, onSubscribed.get());
        assertEquals(6, countAfter.get());
    }

}
