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
import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableTimestampTest {
    Observer<Object> observer;

    @Before
    public void before() {
        observer = TestHelper.mockObserver();
    }

    @Test
    public void timestampWithScheduler() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();
        Observable<Timed<Integer>> m = source.timestamp(scheduler);
        m.subscribe(observer);

        source.onNext(1);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(2);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(3);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext(new Timed<Integer>(1, 0, TimeUnit.MILLISECONDS));
        inOrder.verify(observer, times(1)).onNext(new Timed<Integer>(2, 100, TimeUnit.MILLISECONDS));
        inOrder.verify(observer, times(1)).onNext(new Timed<Integer>(3, 200, TimeUnit.MILLISECONDS));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void timestampWithScheduler2() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();
        Observable<Timed<Integer>> m = source.timestamp(scheduler);
        m.subscribe(observer);

        source.onNext(1);
        source.onNext(2);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(3);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext(new Timed<Integer>(1, 0, TimeUnit.MILLISECONDS));
        inOrder.verify(observer, times(1)).onNext(new Timed<Integer>(2, 0, TimeUnit.MILLISECONDS));
        inOrder.verify(observer, times(1)).onNext(new Timed<Integer>(3, 200, TimeUnit.MILLISECONDS));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
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
            .timestamp()
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
            .timestamp(TimeUnit.SECONDS)
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

}
