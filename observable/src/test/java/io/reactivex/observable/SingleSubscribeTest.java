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

package io.reactivex.observable;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.BiConsumer;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SingleSubscribeTest {

    @Test
    public void consumer() {
        final Integer[] value = { null };

        Single.just(1).subscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                value[0] = v;
                return Unit.INSTANCE;
            }
        });

        assertEquals((Integer)1, value[0]);
    }

    @Test
    public void biconsumer() {
        final Object[] value = { null, null };

        Single.just(1).subscribe(new BiConsumer<Integer, Throwable>() {
            @Override
            public void invoke(Integer v, Throwable e) throws Exception {
                value[0] = v;
                value[1] = e;
            }
        });

        assertEquals(1, value[0]);
        assertNull(value[1]);
    }

    @Test
    public void biconsumerError() {
        final Object[] value = { null, null };

        TestException ex = new TestException();

        Single.error(ex).subscribe(new BiConsumer<Object, Throwable>() {
            @Override
            public void invoke(Object v, Throwable e) throws Exception {
                value[0] = v;
                value[1] = e;
            }
        });

        assertNull(value[0]);
        assertEquals(ex, value[1]);
    }

    @Test
    public void subscribeThrows() {
        try {
            new Single<Integer>() {
                @Override
                protected void subscribeActual(SingleObserver<? super Integer> observer) {
                    throw new IllegalArgumentException();
                }
            }.test();
        } catch (NullPointerException ex) {
            if (!(ex.getCause() instanceof IllegalArgumentException)) {
                fail(ex.toString() + ": should have thrown NPE(IAE)");
            }
        }
    }

    @Test
    public void biConsumerDispose() {
        PublishSubject<Integer> ps = PublishSubject.create();

        Disposable d = ps.single(-99).subscribe(new BiConsumer<Object, Object>() {
            @Override
            public void invoke(Object t1, Object t2) throws Exception {

            }
        });

        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());

        assertFalse(ps.hasObservers());
    }

    @Test
    public void consumerDispose() {
        PublishSubject<Integer> ps = PublishSubject.create();

        Disposable d = ps.single(-99).subscribe(Functions.<Integer>emptyConsumer());

        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());

        assertFalse(ps.hasObservers());
    }

    @Test
    public void consumerSuccessThrows() {
        List<Throwable> list = TestCommonHelper.trackPluginErrors();

        try {
            Single.just(1).subscribe(new Function1<Integer, kotlin.Unit>() {
                @Override
                public Unit invoke(Integer t) {
                    throw new TestException();
                }
            });

            TestCommonHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void consumerErrorThrows() {
        List<Throwable> list = TestCommonHelper.trackPluginErrors();

        try {
            Single.<Integer>error(new TestException("Outer failure")).subscribe(
            Functions.<Integer>emptyConsumer(),
                    new Function1<Throwable, kotlin.Unit>() {
                @Override
                public Unit invoke(Throwable t) {
                    throw new TestException("Inner failure");
                }
            });

            TestCommonHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> cel = TestCommonHelper.compositeList(list.get(0));
            TestCommonHelper.assertError(cel, 0, TestException.class, "Outer failure");
            TestCommonHelper.assertError(cel, 1, TestException.class, "Inner failure");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void biConsumerThrows() {
        List<Throwable> list = TestCommonHelper.trackPluginErrors();

        try {
            Single.just(1).subscribe(new BiConsumer<Integer, Throwable>() {
                @Override
                public void invoke(Integer t, Throwable e) throws Exception {
                    throw new TestException();
                }
            });

            TestCommonHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void biConsumerErrorThrows() {
        List<Throwable> list = TestCommonHelper.trackPluginErrors();

        try {
            Single.<Integer>error(new TestException("Outer failure")).subscribe(
            new BiConsumer<Integer, Throwable>() {
                @Override
                public void invoke(Integer a, Throwable t) throws Exception {
                    throw new TestException("Inner failure");
                }
            });

            TestCommonHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> cel = TestCommonHelper.compositeList(list.get(0));
            TestCommonHelper.assertError(cel, 0, TestException.class, "Outer failure");
            TestCommonHelper.assertError(cel, 1, TestException.class, "Inner failure");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void methodTestNoCancel() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.single(-99).test(false);

        assertTrue(ps.hasObservers());
    }

    @Test
    public void successIsDisposed() {
        assertTrue(Single.just(1).subscribe().isDisposed());
    }

    @Test
    public void errorIsDisposed() {
        assertTrue(Single.error(new TestException()).subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()).isDisposed());
    }

    @Test
    public void biConsumerIsDisposedOnSuccess() {
        final Object[] result = { null, null };

        Disposable d = Single.just(1)
        .subscribe(new BiConsumer<Integer, Throwable>() {
            @Override
            public void invoke(Integer t1, Throwable t2) throws Exception {
                result[0] = t1;
                result[1] = t2;
            }
        });

        assertTrue("Not disposed?!", d.isDisposed());
        assertEquals(1, result[0]);
        assertNull(result[1]);
    }

    @Test
    public void biConsumerIsDisposedOnError() {
        final Object[] result = { null, null };

        Disposable d = Single.<Integer>error(new IOException())
        .subscribe(new BiConsumer<Integer, Throwable>() {
            @Override
            public void invoke(Integer t1, Throwable t2) throws Exception {
                result[0] = t1;
                result[1] = t2;
            }
        });

        assertTrue("Not disposed?!", d.isDisposed());
        assertNull(result[0]);
        assertTrue("" + result[1], result[1] instanceof IOException);
    }
}
