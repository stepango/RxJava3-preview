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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.common.Scheduler;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestScheduler;
import io.reactivex.observable.Completable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertTrue;

public class CompletableTimerTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Completable.timer(1, TimeUnit.SECONDS, new TestScheduler()));
    }

    @Test
    public void timerInterruptible() throws Exception {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            for (Scheduler s : new Scheduler[] { Schedulers.single(), Schedulers.computation(), Schedulers.newThread(), Schedulers.io(), Schedulers.from(exec) }) {
                final AtomicBoolean interrupted = new AtomicBoolean();
                TestObserver<Void> ts = Completable.timer(1, TimeUnit.MILLISECONDS, s)
                        .doOnComplete(new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        try {
                        Thread.sleep(3000);
                        } catch (InterruptedException ex) {
                            interrupted.set(true);
                        }
                        return Unit.INSTANCE;
                    }
                })
                .test();

                Thread.sleep(500);

                ts.cancel();

                Thread.sleep(500);

                assertTrue(s.getClass().getSimpleName(), interrupted.get());
            }
        } finally {
            exec.shutdown();
        }
    }

}
