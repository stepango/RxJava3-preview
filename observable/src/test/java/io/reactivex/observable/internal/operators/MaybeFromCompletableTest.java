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

import io.reactivex.observable.Completable;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.HasUpstreamCompletableSource;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertSame;

public class MaybeFromCompletableTest {
    @Test(expected = NullPointerException.class)
    public void fromCompletableNull() {
        Maybe.fromCompletable(null);
    }

    @Test
    public void fromCompletable() {
        Maybe.fromCompletable(Completable.complete())
            .test()
            .assertResult();
    }

    @Test
    public void fromCompletableError() {
        Maybe.fromCompletable(Completable.error(new UnsupportedOperationException()))
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @Test
    public void source() {
        Completable c = Completable.complete();

        assertSame(c, ((HasUpstreamCompletableSource)Maybe.fromCompletable(c)).source());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.fromCompletable(PublishSubject.create().ignoreElements()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletableToMaybe(new Function1<Completable, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> invoke(Completable v) {
                return Maybe.fromCompletable(v);
            }
        });
    }
}
