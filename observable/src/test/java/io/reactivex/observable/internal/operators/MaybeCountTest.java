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

public class MaybeCountTest {

    @Test
    public void one() {
        Maybe.just(1).count().test().assertResult(1L);
    }

    @Test
    public void empty() {
        Maybe.empty().count().test().assertResult(0L);
    }

    @Test
    public void error() {
        Maybe.error(new TestException()).count().test().assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Long> ts = pp.singleElement().count().test();

        assertTrue(pp.hasObservers());

        ts.cancel();

        assertFalse(pp.hasObservers());
    }

    @Test
    public void isDisposed() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestHelper.checkDisposed(pp.singleElement().count());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToSingle(new Function1<Maybe<Object>, SingleSource<Long>>() {
            @Override
            public SingleSource<Long> invoke(Maybe<Object> f) {
                return f.count();
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void hasSource() {
        assertSame(Maybe.empty(), ((HasUpstreamMaybeSource<Object>)(Maybe.empty().count())).source());
    }
}
