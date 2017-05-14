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

import io.reactivex.observable.CompletableSource;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.HasUpstreamMaybeSource;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertSame;

public class MaybeToCompletableTest {

    @SuppressWarnings("unchecked")
    @Test
    public void source() {
        Maybe<Integer> source = Maybe.just(1);

        assertSame(source, ((HasUpstreamMaybeSource<Integer>)source.ignoreElement().toMaybe()).source());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.never().ignoreElement().toMaybe());
    }

    @Test
    public void successToComplete() {
        Maybe.just(1)
        .ignoreElement()
        .test()
        .assertResult();
    }

    @Test
    public void doubleSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToCompletable(new Function1<Maybe<Object>, CompletableSource>() {
            @Override
            public CompletableSource invoke(Maybe<Object> m) {
                return m.ignoreElement();
            }
        });
    }
}
