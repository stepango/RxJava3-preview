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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposable;
import io.reactivex.common.functions.Consumer;
import io.reactivex.observable.Observable;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertEquals;

public class ObservableDoOnUnsubscribeTest {

    @Test
    public void testDoOnUnsubscribe() throws Exception {
        int subCount = 3;
        final CountDownLatch upperLatch = new CountDownLatch(subCount);
        final CountDownLatch lowerLatch = new CountDownLatch(subCount);
        final CountDownLatch onNextLatch = new CountDownLatch(subCount);

        final AtomicInteger upperCount = new AtomicInteger();
        final AtomicInteger lowerCount = new AtomicInteger();
        Observable<Long> longs = Observable
                // The stream needs to be infinite to ensure the stream does not terminate
                // before it is unsubscribed
                .interval(50, TimeUnit.MILLISECONDS)
                .doOnDispose(new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        // Test that upper stream will be notified for un-subscription
                        // from a child Observer
                            upperLatch.countDown();
                            upperCount.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                })
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                            // Ensure there is at least some onNext events before un-subscription happens
                            onNextLatch.countDown();
                    }
                })
                .doOnDispose(new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        // Test that lower stream will be notified for a direct un-subscription
                            lowerLatch.countDown();
                            lowerCount.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                });

        List<Disposable> subscriptions = new ArrayList<Disposable>();
        List<TestObserver<Long>> subscribers = new ArrayList<TestObserver<Long>>();

        for (int i = 0; i < subCount; ++i) {
            TestObserver<Long> observer = new TestObserver<Long>();
            subscriptions.add(observer);
            longs.subscribe(observer);
            subscribers.add(observer);
        }

        onNextLatch.await();
        for (int i = 0; i < subCount; ++i) {
            subscriptions.get(i).dispose();
            // Test that unsubscribe() method is not affected in any way
            // FIXME no longer valid
//            subscribers.get(i).assertUnsubscribed();
        }

        upperLatch.await();
        lowerLatch.await();
        assertEquals(String.format("There should exactly %d un-subscription events for upper stream", subCount), subCount, upperCount.get());
        assertEquals(String.format("There should exactly %d un-subscription events for lower stream", subCount), subCount, lowerCount.get());
    }

    @Test
    public void testDoOnUnSubscribeWorksWithRefCount() throws Exception {
        int subCount = 3;
        final CountDownLatch upperLatch = new CountDownLatch(1);
        final CountDownLatch lowerLatch = new CountDownLatch(1);
        final CountDownLatch onNextLatch = new CountDownLatch(subCount);

        final AtomicInteger upperCount = new AtomicInteger();
        final AtomicInteger lowerCount = new AtomicInteger();
        Observable<Long> longs = Observable
                // The stream needs to be infinite to ensure the stream does not terminate
                // before it is unsubscribed
                .interval(50, TimeUnit.MILLISECONDS)
                .doOnDispose(new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        // Test that upper stream will be notified for un-subscription
                            upperLatch.countDown();
                            upperCount.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                })
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                            // Ensure there is at least some onNext events before un-subscription happens
                            onNextLatch.countDown();
                    }
                })
                .doOnDispose(new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        // Test that lower stream will be notified for un-subscription
                            lowerLatch.countDown();
                            lowerCount.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                })
                .publish()
                .refCount();

        List<Disposable> subscriptions = new ArrayList<Disposable>();
        List<TestObserver<Long>> subscribers = new ArrayList<TestObserver<Long>>();

        for (int i = 0; i < subCount; ++i) {
            TestObserver<Long> observer = new TestObserver<Long>();
            longs.subscribe(observer);
            subscriptions.add(observer);
            subscribers.add(observer);
        }

        onNextLatch.await();
        for (int i = 0; i < subCount; ++i) {
            subscriptions.get(i).dispose();
            // Test that unsubscribe() method is not affected in any way
            // FIXME no longer valid
//            subscribers.get(i).assertUnsubscribed();
        }

        upperLatch.await();
        lowerLatch.await();
        assertEquals("There should exactly 1 un-subscription events for upper stream", 1, upperCount.get());
        assertEquals("There should exactly 1 un-subscription events for lower stream", 1, lowerCount.get());
    }
}
