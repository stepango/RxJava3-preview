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
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.HasUpstreamMaybeSource;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MaybeContainsTest {

    @Test
    public void doesContain() {
        Maybe.just(1).contains(1).test().assertResult(true);
    }

    @Test
    public void doesntContain() {
        Maybe.just(1).contains(2).test().assertResult(false);
    }

    @Test
    public void empty() {
        Maybe.empty().contains(2).test().assertResult(false);
    }

    @Test
    public void error() {
        Maybe.error(new TestException()).contains(2).test().assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Boolean> ts = pp.singleElement().contains(1).test();

        assertTrue(pp.hasObservers());

        ts.cancel();

        assertFalse(pp.hasObservers());
    }


    @Test
    public void isDisposed() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestHelper.checkDisposed(pp.singleElement().contains(1));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToSingle(new Function1<Maybe<Object>, SingleSource<Boolean>>() {
            @Override
            public SingleSource<Boolean> invoke(Maybe<Object> f) {
                return f.contains(1);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void hasSource() {
        assertSame(Maybe.empty(), ((HasUpstreamMaybeSource<Object>)(Maybe.empty().contains(0))).source());
    }
}
