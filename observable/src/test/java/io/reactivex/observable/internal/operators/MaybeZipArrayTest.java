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

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaybeZipArrayTest {

    final Function2<Object, Object, Object> addString = new Function2<Object, Object, Object>() {
        @Override
        public Object invoke(Object a, Object b) {
            return "" + a + b;
        }
    };


    final Function3<Object, Object, Object, Object> addString3 = new Function3<Object, Object, Object, Object>() {
        @Override
        public Object invoke(Object a, Object b, Object c) {
            return "" + a + b + c;
        }
    };

    @Test
    public void firstError() {
        Maybe.zip(Maybe.error(new TestException()), Maybe.just(1), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void secondError() {
        Maybe.zip(Maybe.just(1), Maybe.<Integer>error(new TestException()), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Object> to = Maybe.zip(pp.singleElement(), pp.singleElement(), addString)
        .test();

        assertTrue(pp.hasObservers());

        to.cancel();

        assertFalse(pp.hasObservers());
    }

    @Test
    public void zipperThrows() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void zipperReturnsNull() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void middleError() {
        PublishSubject<Integer> pp0 = PublishSubject.create();
        PublishSubject<Integer> pp1 = PublishSubject.create();

        TestObserver<Object> to = Maybe.zip(pp0.singleElement(), pp1.singleElement(), pp0.singleElement(), addString3)
        .test();

        pp1.onError(new TestException());

        assertFalse(pp0.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> pp0 = PublishSubject.create();
                final PublishSubject<Integer> pp1 = PublishSubject.create();

                final TestObserver<Object> to = Maybe.zip(pp0.singleElement(), pp1.singleElement(), addString)
                .test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp0.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }
    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayOneIsNull() {
        Maybe.zipArray(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, Maybe.just(1), null)
        .blockingGet();
    }
}
