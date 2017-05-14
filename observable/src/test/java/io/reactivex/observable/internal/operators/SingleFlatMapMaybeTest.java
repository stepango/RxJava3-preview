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
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.Single;
import io.reactivex.observable.TestHelper;
import kotlin.jvm.functions.Function1;

public class SingleFlatMapMaybeTest {
    @Test(expected = NullPointerException.class)
    public void flatMapMaybeNull() {
        Single.just(1)
            .flatMapMaybe(null);
    }

    @Test
    public void flatMapMaybeValue() {
        Single.just(1).flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(final Integer integer) {
                if (integer == 1) {
                    return Maybe.just(2);
                }

                return Maybe.just(1);
            }
        })
            .test()
            .assertResult(2);
    }

    @Test
    public void flatMapMaybeValueDifferentType() {
        Single.just(1).flatMapMaybe(new Function1<Integer, MaybeSource<String>>() {
            @Override
            public MaybeSource<String> invoke(final Integer integer) {
                if (integer == 1) {
                    return Maybe.just("2");
                }

                return Maybe.just("1");
            }
        })
            .test()
            .assertResult("2");
    }

    @Test
    public void flatMapMaybeValueNull() {
        Single.just(1).flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(final Integer integer) {
                return null;
            }
        })
            .test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("The mapper returned a null MaybeSource");
    }

    @Test
    public void flatMapMaybeValueErrorThrown() {
        Single.just(1).flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(final Integer integer) {
                throw new RuntimeException("something went terribly wrong!");
            }
        })
            .test()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("something went terribly wrong!");
    }

    @Test
    public void flatMapMaybeError() {
        RuntimeException exception = new RuntimeException("test");

        Single.error(exception).flatMapMaybe(new Function1<Object, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> invoke(final Object integer) {
                return Maybe.just(new Object());
            }
        })
            .test()
            .assertError(exception);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(1);
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe(new Function1<Single<Integer>, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Single<Integer> v) {
                return v.flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(Integer v) {
                        return Maybe.just(1);
                    }
                });
            }
        });
    }

    @Test
    public void mapsToError() {
        Single.just(1).flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapsToEmpty() {
        Single.just(1).flatMapMaybe(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.empty();
            }
        })
        .test()
        .assertResult();
    }
}
