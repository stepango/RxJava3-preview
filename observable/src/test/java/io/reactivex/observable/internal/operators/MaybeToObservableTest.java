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

import io.reactivex.observable.Maybe;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.HasUpstreamMaybeSource;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertSame;

public class MaybeToObservableTest {

    @Test
    public void source() {
        Maybe<Integer> m = Maybe.just(1);

        assertSame(m, (((HasUpstreamMaybeSource<?>)m.toObservable()).source()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToObservable(new Function1<Maybe<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Maybe<Object> m) {
                return m.toObservable();
            }
        });
    }
}
