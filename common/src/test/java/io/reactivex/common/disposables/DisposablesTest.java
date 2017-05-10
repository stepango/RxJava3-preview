/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.common.disposables;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.disposables.DisposableHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DisposablesTest {

    @Test
    public void testUnsubscribeOnlyOnce() {
        Runnable dispose = mock(Runnable.class);
        Disposable subscription = Disposables.fromRunnable(dispose);
        subscription.dispose();
        subscription.dispose();
        verify(dispose, times(1)).run();
    }

    @Test
    public void testEmpty() {
        Disposable empty = Disposables.empty();
        assertFalse(empty.isDisposed());
        empty.dispose();
        assertTrue(empty.isDisposed());
    }

    @Test
    public void testUnsubscribed() {
        Disposable disposed = Disposables.disposed();
        assertTrue(disposed.isDisposed());
    }

    @Test
    public void utilityClass() {
        TestCommonHelper.checkUtilityClass(Disposables.class);
    }

    @Test
    public void fromAction() {
        class AtomicAction extends AtomicBoolean implements Function0 {

            private static final long serialVersionUID = -1517510584253657229L;

            @Override
            public kotlin.Unit invoke() {
                set(true);
                return Unit.INSTANCE;
            }
        }

        AtomicAction aa = new AtomicAction();

        Disposables.fromAction(aa).dispose();

        assertTrue(aa.get());
    }

    @Test
    public void fromActionThrows() {
        try {
            Disposables.fromAction(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new IllegalArgumentException();
                }
            }).dispose();
            fail("Should have thrown!");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            Disposables.fromAction(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new InternalError();
                }
            }).dispose();
            fail("Should have thrown!");
        } catch (InternalError ex) {
            // expected
        }

        try {
            Disposables.fromAction(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            }).dispose();
            fail("Should have thrown!");
        } catch (RuntimeException ex) {
            if (!(ex instanceof TestException)) {
                fail(ex.toString() + ": Should have cause of TestException");
            }
            // expected
        }

    }

    @Test
    public void disposeRace() {
        for (int i = 0; i < 100; i++) {
            final Disposable d = Disposables.empty();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            TestCommonHelper.race(r, r, Schedulers.io());
        }
    }

    @Test
    public void setOnceTwice() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {

            AtomicReference<Disposable> target = new AtomicReference<Disposable>();
            Disposable d = Disposables.empty();

            DisposableHelper.setOnce(target, d);

            Disposable d1 = Disposables.empty();

            DisposableHelper.setOnce(target, d1);

            assertTrue(d1.isDisposed());

            TestCommonHelper.assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
