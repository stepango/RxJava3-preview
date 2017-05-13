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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Schedulers;
import io.reactivex.observable.Completable;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertNotEquals;

public class CompletableDelayTest {

    @Test
    public void delayCustomScheduler() {

        Completable.complete()
        .delay(100, TimeUnit.MILLISECONDS, Schedulers.trampoline())
        .test()
        .assertResult();
    }

    @Test
    public void testOnErrorCalledOnScheduler() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> thread = new AtomicReference<Thread>();

        Completable.error(new Exception())
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .doOnError(new Function1<Throwable, Unit>() {
                    @Override
                    public Unit invoke(Throwable throwable) {
                        thread.set(Thread.currentThread());
                        latch.countDown();
                        return Unit.INSTANCE;
                    }
                })
                .onErrorComplete()
                .subscribe();

        latch.await();

        assertNotEquals(Thread.currentThread(), thread.get());
    }

}
