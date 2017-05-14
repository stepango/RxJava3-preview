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
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertTrue;

public class MaybeIsEmptyTest {

    @Test
    public void normal() {
        Maybe.just(1)
        .isEmpty()
        .test()
        .assertResult(false);
    }

    @Test
    public void empty() {
        Maybe.empty()
        .isEmpty()
        .test()
        .assertResult(true);
    }

    @Test
    public void error() {
        Maybe.error(new TestException())
        .isEmpty()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedBackToMaybe() {
        assertTrue(Maybe.just(1)
        .isEmpty()
        .toMaybe() instanceof MaybeIsEmpty);
    }


    @Test
    public void normalToMaybe() {
        Maybe.just(1)
        .isEmpty()
        .toMaybe()
        .test()
        .assertResult(false);
    }

    @Test
    public void emptyToMaybe() {
        Maybe.empty()
        .isEmpty()
        .toMaybe()
        .test()
        .assertResult(true);
    }

    @Test
    public void errorToMaybe() {
        Maybe.error(new TestException())
        .isEmpty()
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposedMaybeToSingle(new Function1<Maybe<Object>, SingleSource<Boolean>>() {
            @Override
            public SingleSource<Boolean> invoke(Maybe<Object> m) {
                return m.isEmpty();
            }
        });
    }

    @Test
    public void isDisposed() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestHelper.checkDisposed(pp.singleElement().isEmpty());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToSingle(new Function1<Maybe<Object>, Single<Boolean>>() {
            @Override
            public Single<Boolean> invoke(Maybe<Object> f) {
                return f.isEmpty();
            }
        });
    }

    @Test
    public void disposeToMaybe() {
        TestHelper.checkDisposedMaybe(new Function1<Maybe<Object>, Maybe<Boolean>>() {
            @Override
            public Maybe<Boolean> invoke(Maybe<Object> m) {
                return m.isEmpty().toMaybe();
            }
        });
    }

    @Test
    public void isDisposedToMaybe() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestHelper.checkDisposed(pp.singleElement().isEmpty().toMaybe());
    }

    @Test
    public void doubleOnSubscribeToMaybe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function1<Maybe<Object>, Maybe<Boolean>>() {
            @Override
            public Maybe<Boolean> invoke(Maybe<Object> f) {
                return f.isEmpty().toMaybe();
            }
        });
    }
}
