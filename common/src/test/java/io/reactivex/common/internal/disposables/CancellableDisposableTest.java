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

package io.reactivex.common.internal.disposables;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CancellableDisposableTest {

    @Test
    public void normal() {
        final AtomicInteger count = new AtomicInteger();

        Function0 c = new Function0() {
            @Override
            public Unit invoke() {
                count.getAndIncrement();
                return Unit.INSTANCE;
            }
        };

        CancellableDisposable cd = new CancellableDisposable(c);

        assertFalse(cd.isDisposed());

        cd.dispose();
        cd.dispose();

        assertTrue(cd.isDisposed());

        assertEquals(1, count.get());
    }

    @Test
    public void cancelThrows() {
        final AtomicInteger count = new AtomicInteger();

        Function0 c = new Function0() {
            @Override
            public Unit invoke() {
                count.getAndIncrement();
                throw new TestException();
            }
        };

        CancellableDisposable cd = new CancellableDisposable(c);

        assertFalse(cd.isDisposed());

        List<Throwable> list = TestCommonHelper.trackPluginErrors();
        try {
            cd.dispose();
            cd.dispose();

            TestCommonHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
        assertTrue(cd.isDisposed());

        assertEquals(1, count.get());
    }

    @Test
    public void disposeRace() {

        for (int i = 0; i < 100; i++) {
            final AtomicInteger count = new AtomicInteger();

            Function0 c = new Function0() {
                @Override
                public Unit invoke() {
                    count.getAndIncrement();
                    return Unit.INSTANCE;
                }
            };

            final CancellableDisposable cd = new CancellableDisposable(c);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    cd.dispose();
                }
            };

            TestCommonHelper.race(r, r, Schedulers.io());

            assertEquals(1, count.get());
        }
    }

}
