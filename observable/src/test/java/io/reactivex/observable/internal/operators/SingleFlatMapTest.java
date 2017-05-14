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

import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Completable;
import io.reactivex.observable.Observable;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleFlatMapTest {

    @Test
    public void normal() {
        final boolean[] b = { false };

        Single.just(1)
                .flatMapCompletable(new Function1<Integer, Completable>() {
            @Override
            public Completable invoke(Integer t) {
                return Completable.complete().doOnComplete(new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        b[0] = true;
                        return Unit.INSTANCE;
                    }
                });
            }
        })
        .test()
        .assertResult();

        assertTrue(b[0]);
    }

    @Test
    public void error() {
        final boolean[] b = { false };

        Single.<Integer>error(new TestException())
                .flatMapCompletable(new Function1<Integer, Completable>() {
            @Override
            public Completable invoke(Integer t) {
                return Completable.complete().doOnComplete(new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        b[0] = true;
                        return Unit.INSTANCE;
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(b[0]);
    }

    @Test
    public void mapperThrows() {
        final boolean[] b = { false };

        Single.just(1)
                .flatMapCompletable(new Function1<Integer, Completable>() {
            @Override
            public Completable invoke(Integer t) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(b[0]);
    }

    @Test
    public void mapperReturnsNull() {
        final boolean[] b = { false };

        Single.just(1)
                .flatMapCompletable(new Function1<Integer, Completable>() {
            @Override
            public Completable invoke(Integer t) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);

        assertFalse(b[0]);
    }


    @Test
    public void flatMapObservable() {
        Single.just(1).flatMapObservable(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return Observable.range(v, 5);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNull() {
        Single.just(1)
            .flatMap(null);
    }

    @Test
    public void flatMapValue() {
        Single.just(1).flatMap(new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(final Integer integer) {
                if (integer == 1) {
                    return Single.just(2);
                }

                return Single.just(1);
            }
        })
            .test()
            .assertResult(2);
    }

    @Test
    public void flatMapValueDifferentType() {
        Single.just(1).flatMap(new Function1<Integer, SingleSource<String>>() {
            @Override
            public SingleSource<String> invoke(final Integer integer) {
                if (integer == 1) {
                    return Single.just("2");
                }

                return Single.just("1");
            }
        })
            .test()
            .assertResult("2");
    }

    @Test
    public void flatMapValueNull() {
        Single.just(1).flatMap(new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(final Integer integer) {
                return null;
            }
        })
            .test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("The single returned by the mapper is null");
    }

    @Test
    public void flatMapValueErrorThrown() {
        Single.just(1).flatMap(new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(final Integer integer) {
                throw new RuntimeException("something went terribly wrong!");
            }
        })
            .test()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("something went terribly wrong!");
    }

    @Test
    public void flatMapError() {
        RuntimeException exception = new RuntimeException("test");

        Single.error(exception).flatMap(new Function1<Object, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> invoke(final Object integer) {
                return Single.just(new Object());
            }
        })
            .test()
            .assertError(exception);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).flatMap(new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.just(2);
            }
        }));
    }

    @Test
    public void mappedSingleOnError() {
        Single.just(1).flatMap(new Function1<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> invoke(Integer v) {
                return Single.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
}
