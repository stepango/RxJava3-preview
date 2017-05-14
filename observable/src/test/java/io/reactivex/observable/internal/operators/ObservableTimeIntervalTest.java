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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.concurrent.TimeUnit;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Scheduler;
import io.reactivex.common.TestScheduler;
import io.reactivex.common.Timed;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

public class ObservableTimeIntervalTest {

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private Observer<Timed<Integer>> observer;

    private TestScheduler testScheduler;
    private PublishSubject<Integer> subject;
    private Observable<Timed<Integer>> observable;

    @Before
    public void setUp() {
        observer = TestHelper.mockObserver();
        testScheduler = new TestScheduler();
        subject = PublishSubject.create();
        observable = subject.timeInterval(testScheduler);
    }

    @Test
    public void testTimeInterval() {
        InOrder inOrder = inOrder(observer);
        observable.subscribe(observer);

        testScheduler.advanceTimeBy(1000, TIME_UNIT);
        subject.onNext(1);
        testScheduler.advanceTimeBy(2000, TIME_UNIT);
        subject.onNext(2);
        testScheduler.advanceTimeBy(3000, TIME_UNIT);
        subject.onNext(3);
        subject.onComplete();

        inOrder.verify(observer, times(1)).onNext(
                new Timed<Integer>(1, 1000, TIME_UNIT));
        inOrder.verify(observer, times(1)).onNext(
                new Timed<Integer>(2, 2000, TIME_UNIT));
        inOrder.verify(observer, times(1)).onNext(
                new Timed<Integer>(3, 3000, TIME_UNIT));
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void timeIntervalDefault() {
        final TestScheduler scheduler = new TestScheduler();

        RxJavaCommonPlugins.setComputationSchedulerHandler(new Function1<Scheduler, Scheduler>() {
            @Override
            public Scheduler invoke(Scheduler v) {
                return scheduler;
            }
        });

        try {
            Observable.range(1, 5)
            .timeInterval()
                    .map(new Function1<Timed<Integer>, Long>() {
                @Override
                public Long invoke(Timed<Integer> v) {
                    return v.time();
                }
            })
            .test()
            .assertResult(0L, 0L, 0L, 0L, 0L);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void timeIntervalDefaultSchedulerCustomUnit() {
        final TestScheduler scheduler = new TestScheduler();

        RxJavaCommonPlugins.setComputationSchedulerHandler(new Function1<Scheduler, Scheduler>() {
            @Override
            public Scheduler invoke(Scheduler v) {
                return scheduler;
            }
        });

        try {
            Observable.range(1, 5)
            .timeInterval(TimeUnit.SECONDS)
                    .map(new Function1<Timed<Integer>, Long>() {
                @Override
                public Long invoke(Timed<Integer> v) {
                    return v.time();
                }
            })
            .test()
            .assertResult(0L, 0L, 0L, 0L, 0L);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).timeInterval());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void error() {
        Observable.error(new TestException())
        .timeInterval()
        .test()
        .assertFailure(TestException.class);
    }
}
