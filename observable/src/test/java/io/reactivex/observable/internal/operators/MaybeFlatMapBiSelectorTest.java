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
import kotlin.jvm.functions.Function2;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;

public class MaybeFlatMapBiSelectorTest {

    Function2<Integer, Integer, String> stringCombine() {
        return new Function2<Integer, Integer, String>() {
            @Override
            public String invoke(Integer a, Integer b) {
                return a + ":" + b;
            }
        };
    }

    @Test
    public void normal() {
        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(2);
            }
        }, stringCombine())
        .test()
        .assertResult("1:2");
    }

    @Test
    public void normalWithEmpty() {
        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.empty();
            }
        }, stringCombine())
        .test()
        .assertResult();
    }

    @Test
    public void emptyWithJust() {
        final int[] call = { 0 };

        Maybe.<Integer>empty()
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                call[0]++;
                return Maybe.just(1);
            }
        }, stringCombine())
        .test()
        .assertResult();

        assertEquals(0, call[0]);
    }

    @Test
    public void errorWithJust() {
        final int[] call = { 0 };

        Maybe.<Integer>error(new TestException())
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                call[0]++;
                return Maybe.just(1);
            }
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void justWithError() {
        final int[] call = { 0 };

        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                call[0]++;
                return Maybe.<Integer>error(new TestException());
            }
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, call[0]);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().singleElement()
                .flatMap(new Function1<Object, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Object v) {
                return Maybe.just(1);
            }
        }, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return b;
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function1<Maybe<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> invoke(Maybe<Object> v) {
                return v.flatMap(new Function1<Object, MaybeSource<Integer>>() {
                    @Override
                    public MaybeSource<Integer> invoke(Object v) {
                        return Maybe.just(1);
                    }
                }, new Function2<Object, Integer, Object>() {
                    @Override
                    public Object invoke(Object a, Integer b) {
                        return b;
                    }
                });
            }
        });
    }

    @Test
    public void mapperThrows() {
        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                throw new TestException();
            }
        }, stringCombine())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperReturnsNull() {
        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return null;
            }
        }, stringCombine())
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void resultSelectorThrows() {
        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(2);
            }
        }, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void resultSelectorReturnsNull() {
        Maybe.just(1)
                .flatMap(new Function1<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> invoke(Integer v) {
                return Maybe.just(2);
            }
        }, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }
}
