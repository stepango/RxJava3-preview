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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.functions.Action;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MaybeFromActionTest {
    @Test(expected = NullPointerException.class)
    public void fromActionNull() {
        Maybe.fromAction(null);
    }

    @Test
    public void fromAction() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Maybe.fromAction(new Action() {
            @Override
            public void invoke() {
                atomicInteger.incrementAndGet();
            }
        })
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromActionTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Action run = new Action() {
            @Override
            public void invoke() {
                atomicInteger.incrementAndGet();
            }
        };

        Maybe.fromAction(run)
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Maybe.fromAction(run)
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromActionInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Maybe<Object> maybe = Maybe.fromAction(new Action() {
            @Override
            public void invoke() {
                atomicInteger.incrementAndGet();
            }
        });

        assertEquals(0, atomicInteger.get());

        maybe
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromActionThrows() {
        Maybe.fromAction(new Action() {
            @Override
            public void invoke() {
                throw new UnsupportedOperationException();
            }
        })
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void callable() throws Exception {
        final int[] counter = { 0 };

        Maybe<Void> m = Maybe.fromAction(new Action() {
            @Override
            public void invoke() {
                counter[0]++;
            }
        });

        assertTrue(m.getClass().toString(), m instanceof Callable);

        assertNull(((Callable<Void>)m).call());

        assertEquals(1, counter[0]);
    }

    @Test
    public void noErrorLoss() throws Exception {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            final CountDownLatch cdl1 = new CountDownLatch(1);
            final CountDownLatch cdl2 = new CountDownLatch(1);

            TestObserver<Object> to = Maybe.fromAction(new Action() {
                @Override
                public void invoke() {
                    cdl1.countDown();
                    try {
                        cdl2.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).subscribeOn(Schedulers.single()).test();

            assertTrue(cdl1.await(5, TimeUnit.SECONDS));

            to.cancel();

            cdl2.countDown();

            int timeout = 10;

            while (timeout-- > 0 && errors.isEmpty()) {
                Thread.sleep(100);
            }

            TestCommonHelper.assertUndeliverable(errors, 0, RuntimeException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
