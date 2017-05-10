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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Scheduler;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.annotations.NonNull;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Action;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ObservableUnsubscribeOnTest {

    @Test(timeout = 5000)
    public void unsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnSameThread() throws InterruptedException {
        UIEventLoopScheduler UI_EVENT_LOOP = new UIEventLoopScheduler();
        try {
            final ThreadSubscription subscription = new ThreadSubscription();
            final AtomicReference<Thread> subscribeThread = new AtomicReference<Thread>();
            Observable<Integer> w = Observable.unsafeCreate(new ObservableSource<Integer>() {

                @Override
                public void subscribe(Observer<? super Integer> t1) {
                    subscribeThread.set(Thread.currentThread());
                    t1.onSubscribe(subscription);
                    t1.onNext(1);
                    t1.onNext(2);
                    t1.onComplete();
                }
            });

            TestObserver<Integer> observer = new TestObserver<Integer>();
            w.subscribeOn(UI_EVENT_LOOP).observeOn(Schedulers.computation())
            .unsubscribeOn(UI_EVENT_LOOP)
            .take(2)
            .subscribe(observer);

            observer.awaitTerminalEvent(5, TimeUnit.SECONDS);

            Thread unsubscribeThread = subscription.getThread();

            assertNotNull(unsubscribeThread);
            assertNotSame(Thread.currentThread(), unsubscribeThread);

            assertNotNull(subscribeThread.get());
            assertNotSame(Thread.currentThread(), subscribeThread.get());
            // True for Schedulers.newThread()

            System.out.println("unsubscribeThread: " + unsubscribeThread);
            System.out.println("subscribeThread.get(): " + subscribeThread.get());
            assertTrue(unsubscribeThread.toString(), unsubscribeThread == UI_EVENT_LOOP.getThread());

            observer.assertValues(1, 2);
            observer.assertTerminated();
        } finally {
            UI_EVENT_LOOP.shutdown();
        }
    }

    @Test(timeout = 5000)
    public void unsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnDifferentThreads() throws InterruptedException {
        UIEventLoopScheduler UI_EVENT_LOOP = new UIEventLoopScheduler();
        try {
            final ThreadSubscription subscription = new ThreadSubscription();
            final AtomicReference<Thread> subscribeThread = new AtomicReference<Thread>();
            Observable<Integer> w = Observable.unsafeCreate(new ObservableSource<Integer>() {

                @Override
                public void subscribe(Observer<? super Integer> t1) {
                    subscribeThread.set(Thread.currentThread());
                    t1.onSubscribe(subscription);
                    t1.onNext(1);
                    t1.onNext(2);
                    t1.onComplete();
                }
            });

            TestObserver<Integer> observer = new TestObserver<Integer>();
            w.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.computation())
            .unsubscribeOn(UI_EVENT_LOOP)
            .take(2)
            .subscribe(observer);

            observer.awaitTerminalEvent(1, TimeUnit.SECONDS);

            Thread unsubscribeThread = subscription.getThread();

            assertNotNull(unsubscribeThread);
            assertNotSame(Thread.currentThread(), unsubscribeThread);

            assertNotNull(subscribeThread.get());
            assertNotSame(Thread.currentThread(), subscribeThread.get());
            // True for Schedulers.newThread()

            System.out.println("UI Thread: " + UI_EVENT_LOOP.getThread());
            System.out.println("unsubscribeThread: " + unsubscribeThread);
            System.out.println("subscribeThread.get(): " + subscribeThread.get());
            assertSame(unsubscribeThread, UI_EVENT_LOOP.getThread());

            observer.assertValues(1, 2);
            observer.assertTerminated();
        } finally {
            UI_EVENT_LOOP.shutdown();
        }
    }

    private static class ThreadSubscription extends AtomicBoolean implements Disposable {

        private static final long serialVersionUID = -5011338112974328771L;

        private volatile Thread thread;

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void dispose() {
            set(true);
            System.out.println("unsubscribe invoked: " + Thread.currentThread());
            thread = Thread.currentThread();
            latch.countDown();
        }

        @Override public boolean isDisposed() {
            return get();
        }

        public Thread getThread() throws InterruptedException {
            latch.await();
            return thread;
        }
    }

    public static class UIEventLoopScheduler extends Scheduler {

        private final Scheduler eventLoop;
        private volatile Thread t;

        public UIEventLoopScheduler() {

            eventLoop = Schedulers.single();

            /*
             * DON'T DO THIS IN PRODUCTION CODE
             */
            final CountDownLatch latch = new CountDownLatch(1);
            eventLoop.scheduleDirect(new Runnable() {

                @Override
                public void run() {
                    t = Thread.currentThread();
                    latch.countDown();
                }

            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("failed to initialize and get inner thread");
            }
        }

        @NonNull
        @Override
        public Worker createWorker() {
            return eventLoop.createWorker();
        }

        public Thread getThread() {
            return t;
        }

    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).unsubscribeOn(Schedulers.single()));
    }

    @Test
    public void normal() {
        final int[] calls = { 0 };

        Observable.just(1)
        .doOnDispose(new Action() {
            @Override
            public kotlin.Unit invoke() {
                calls[0]++;
                return Unit.INSTANCE;
            }
        })
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertResult(1);

        assertEquals(0, calls[0]);
    }

    @Test
    public void error() {
        final int[] calls = { 0 };

        Observable.error(new TestException())
        .doOnDispose(new Action() {
            @Override
            public kotlin.Unit invoke() {
                calls[0]++;
                return Unit.INSTANCE;
            }
        })
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, calls[0]);
    }

    @Test
    public void signalAfterDispose() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .unsubscribeOn(Schedulers.single())
            .take(1)
            .test()
            .assertResult(1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
