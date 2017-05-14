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

package io.reactivex.common;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Scheduler.Worker;
import io.reactivex.common.TestScheduler.TestWorker;
import io.reactivex.common.TestScheduler.TimedRunnable;
import io.reactivex.common.internal.utils.ExceptionHelper;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestSchedulerTest {

    @SuppressWarnings("unchecked")
    // mocking is unchecked, unfortunately
    @Test
    public final void testPeriodicScheduling() throws Exception {
        final Function1<Long, Void> calledOp = mock(Function1.class);

        final TestScheduler scheduler = new TestScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            inner.schedulePeriodically(new Runnable() {
                @Override
                public void run() {
                    System.out.println(scheduler.now(TimeUnit.MILLISECONDS));
                    try {
                        calledOp.invoke(scheduler.now(TimeUnit.MILLISECONDS));
                    } catch (Throwable ex) {
                        ExceptionHelper.wrapOrThrow(ex);
                    }
                }
            }, 1, 2, TimeUnit.SECONDS);

            verify(calledOp, never()).invoke(anyLong());

            InOrder inOrder = Mockito.inOrder(calledOp);

            scheduler.advanceTimeBy(999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).invoke(anyLong());

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).invoke(1000L);

            scheduler.advanceTimeBy(1999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).invoke(3000L);

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).invoke(3000L);

            scheduler.advanceTimeBy(5L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).invoke(5000L);
            inOrder.verify(calledOp, times(1)).invoke(7000L);

            inner.dispose();
            scheduler.advanceTimeBy(11L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, never()).invoke(anyLong());
        } finally {
            inner.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    // mocking is unchecked, unfortunately
    @Test
    public final void testPeriodicSchedulingUnsubscription() throws Exception {
        final Function1<Long, Void> calledOp = mock(Function1.class);

        final TestScheduler scheduler = new TestScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            final Disposable subscription = inner.schedulePeriodically(new Runnable() {
                @Override
                public void run() {
                    System.out.println(scheduler.now(TimeUnit.MILLISECONDS));
                    try {
                        calledOp.invoke(scheduler.now(TimeUnit.MILLISECONDS));
                    } catch (Throwable ex) {
                        ExceptionHelper.wrapOrThrow(ex);
                    }
                }
            }, 1, 2, TimeUnit.SECONDS);

            verify(calledOp, never()).invoke(anyLong());

            InOrder inOrder = Mockito.inOrder(calledOp);

            scheduler.advanceTimeBy(999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).invoke(anyLong());

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).invoke(1000L);

            scheduler.advanceTimeBy(1999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).invoke(3000L);

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).invoke(3000L);

            scheduler.advanceTimeBy(5L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).invoke(5000L);
            inOrder.verify(calledOp, times(1)).invoke(7000L);

            subscription.dispose();
            scheduler.advanceTimeBy(11L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, never()).invoke(anyLong());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void testImmediateUnsubscribes() {
        TestScheduler s = new TestScheduler();
        final Scheduler.Worker inner = s.createWorker();
        final AtomicInteger counter = new AtomicInteger(0);

        try {
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    counter.incrementAndGet();
                    System.out.println("counter: " + counter.get());
                    inner.schedule(this);
                }

            });
            inner.dispose();
            assertEquals(0, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void testImmediateUnsubscribes2() {
        TestScheduler s = new TestScheduler();
        final Scheduler.Worker inner = s.createWorker();
        try {
            final AtomicInteger counter = new AtomicInteger(0);

            final Disposable subscription = inner.schedule(new Runnable() {

                @Override
                public void run() {
                    counter.incrementAndGet();
                    System.out.println("counter: " + counter.get());
                    inner.schedule(this);
                }

            });
            subscription.dispose();
            assertEquals(0, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void timedRunnableToString() {
        TimedRunnable r = new TimedRunnable((TestWorker) new TestScheduler().createWorker(), 5, new Runnable() {
            @Override
            public void run() {
            }
            @Override
            public String toString() {
                return "Runnable";
            }
        }, 1);

        assertEquals("TimedRunnable(time = 5, run = Runnable)", r.toString());
    }

    @Test
    public void workerDisposed() {
        TestScheduler scheduler = new TestScheduler();

        Worker w = scheduler.createWorker();
        w.dispose();
        assertTrue(w.isDisposed());
    }


}
