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

import java.util.Arrays;
import java.util.List;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.utils.CrashingMappedIterable;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaybeZipIterableTest {

    final Function1<Object[], Object> addString = new Function1<Object[], Object>() {
        @Override
        public Object invoke(Object[] a) {
            return Arrays.toString(a);
        }
    };

    @SuppressWarnings("unchecked")
    @Test
    public void firstError() {
        Maybe.zip(Arrays.asList(Maybe.error(new TestException()), Maybe.just(1)), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void secondError() {
        Maybe.zip(Arrays.asList(Maybe.just(1), Maybe.<Integer>error(new TestException())), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void dispose() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Object> to = Maybe.zip(Arrays.asList(pp.singleElement(), pp.singleElement()), addString)
        .test();

        assertTrue(pp.hasObservers());

        to.cancel();

        assertFalse(pp.hasObservers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipperThrows() {
        Maybe.zip(Arrays.asList(Maybe.just(1), Maybe.just(2)), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] b) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipperReturnsNull() {
        Maybe.zip(Arrays.asList(Maybe.just(1), Maybe.just(2)), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void middleError() {
        PublishSubject<Integer> pp0 = PublishSubject.create();
        PublishSubject<Integer> pp1 = PublishSubject.create();

        TestObserver<Object> to = Maybe.zip(
                Arrays.asList(pp0.singleElement(), pp1.singleElement(), pp0.singleElement()), addString)
        .test();

        pp1.onError(new TestException());

        assertFalse(pp0.hasObservers());

        to.assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> pp0 = PublishSubject.create();
                final PublishSubject<Integer> pp1 = PublishSubject.create();

                final TestObserver<Object> to = Maybe.zip(
                        Arrays.asList(pp0.singleElement(), pp1.singleElement()), addString)
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

    @Test
    public void iteratorThrows() {
        Maybe.zip(new CrashingMappedIterable<Maybe<Integer>>(1, 100, 100, new Function1<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> invoke(Integer v) {
                return Maybe.just(v);
            }
        }), addString)
        .test()
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNextThrows() {
        Maybe.zip(new CrashingMappedIterable<Maybe<Integer>>(100, 20, 100, new Function1<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> invoke(Integer v) {
                return Maybe.just(v);
            }
        }), addString)
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextThrows() {
        Maybe.zip(new CrashingMappedIterable<Maybe<Integer>>(100, 100, 5, new Function1<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> invoke(Integer v) {
                return Maybe.just(v);
            }
        }), addString)
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableOneIsNull() {
        Maybe.zip(Arrays.asList(null, Maybe.just(1)), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        })
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableTwoIsNull() {
        Maybe.zip(Arrays.asList(Maybe.just(1), null), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        })
        .blockingGet();
    }
}
