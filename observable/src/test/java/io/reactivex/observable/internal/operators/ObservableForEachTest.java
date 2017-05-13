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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.OnErrorNotImplementedException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObservableForEachTest {

    @Test
    public void forEachWile() {
        final List<Object> list = new ArrayList<Object>();

        Observable.range(1, 5)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        })
                .forEachWhile(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v < 3;
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void forEachWileWithError() {
        final List<Object> list = new ArrayList<Object>();

        Observable.range(1, 5).concatWith(Observable.<Integer>error(new TestException()))
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        })
                .forEachWhile(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
                }, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                list.add(100);
                return Unit.INSTANCE;
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(Observable<Integer> f) throws Exception {
                return f.forEachWhile(Functions.alwaysTrue());
            }
        }, false, 1, 1, (Object[])null);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> ps = PublishSubject.create();

        Disposable d = ps.forEachWhile(Functions.alwaysTrue());

        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());
    }

    @Test
    public void whilePredicateThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Observable.just(1).forEachWhile(new Function1<Integer, Boolean>() {
                @Override
                public Boolean invoke(Integer v) {
                    throw new TestException();
                }
            });

            TestCommonHelper.assertError(errors, 0, OnErrorNotImplementedException.class);
            Throwable c = errors.get(0).getCause();
            assertTrue("" + c, c instanceof TestException);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void whileErrorThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Observable.<Integer>error(new TestException("Outer"))
                    .forEachWhile(Functions.alwaysTrue(), new Function1<Throwable, kotlin.Unit>() {
                @Override
                public Unit invoke(Throwable v) {
                    throw new TestException("Inner");
                }
            });

            TestCommonHelper.assertError(errors, 0, CompositeException.class);

            List<Throwable> ce = TestCommonHelper.compositeList(errors.get(0));

            TestCommonHelper.assertError(ce, 0, TestException.class, "Outer");
            TestCommonHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void whileCompleteThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Observable.just(1).forEachWhile(Functions.alwaysTrue(), Functions.emptyConsumer(),
                    new Function0() {
                        @Override
                        public kotlin.Unit invoke() {
                            throw new TestException();
                        }
                    });

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

}
