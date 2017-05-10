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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.observable.Completable;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertEquals;

public class CompletableFromActionTest {
    @Test(expected = NullPointerException.class)
    public void fromActionNull() {
        Completable.fromAction(null);
    }

    @Test
    public void fromAction() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Completable.fromAction(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                atomicInteger.incrementAndGet();
                return Unit.INSTANCE;
            }
        })
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromActionTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Function0 run = new Function0() {
            @Override
            public kotlin.Unit invoke() {
                atomicInteger.incrementAndGet();
                return Unit.INSTANCE;
            }
        };

        Completable.fromAction(run)
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Completable.fromAction(run)
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromActionInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Completable completable = Completable.fromAction(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                atomicInteger.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        assertEquals(0, atomicInteger.get());

        completable
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromActionThrows() {
        Completable.fromAction(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                throw new UnsupportedOperationException();
            }
        })
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }
}
