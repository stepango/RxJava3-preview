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

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposable;
import io.reactivex.common.Scheduler;
import io.reactivex.common.Schedulers;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableEmitter;
import io.reactivex.observable.ObservableOnSubscribe;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ObservableDelaySubscriptionOtherTest {
    @Test
    public void testNoPrematureSubscription() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.just(1)
                .doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable d) {
                subscribed.getAndIncrement();
                return Unit.INSTANCE;
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testNoMultipleSubscriptions() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.just(1)
                .doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable d) {
                subscribed.getAndIncrement();
                return Unit.INSTANCE;
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);
        other.onNext(2);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testCompleteTriggersSubscription() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.just(1)
                .doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable d) {
                subscribed.getAndIncrement();
                return Unit.INSTANCE;
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onComplete();

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testNoPrematureSubscriptionToError() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.<Integer>error(new TestException())
                .doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable d) {
                subscribed.getAndIncrement();
                return Unit.INSTANCE;
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onComplete();

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void testNoSubscriptionIfOtherErrors() {
        PublishSubject<Object> other = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Observable.<Integer>error(new TestException())
                .doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable d) {
                subscribed.getAndIncrement();
                return Unit.INSTANCE;
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onError(new TestException());

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void badSourceOther() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Integer>, Object>() {
            @Override
            public Object invoke(Observable<Integer> o) {
                return Observable.just(1).delaySubscription(o);
            }
        }, false, 1, 1, 1);
    }


    @Test
    public void afterDelayNoInterrupt() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            for (Scheduler s : new Scheduler[] { Schedulers.single(), Schedulers.computation(), Schedulers.newThread(), Schedulers.io(), Schedulers.from(exec) }) {
                final TestObserver<Boolean> observer = TestObserver.create();
                observer.withTag(s.getClass().getSimpleName());

                Observable.<Boolean>create(new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                      emitter.onNext(Thread.interrupted());
                      emitter.onComplete();
                    }
                })
                .delaySubscription(100, TimeUnit.MILLISECONDS, s)
                .subscribe(observer);

                observer.awaitTerminalEvent();
                observer.assertValue(false);
            }
        } finally {
            exec.shutdown();
        }
    }

}
