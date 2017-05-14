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
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

public class MaybeDetachTest {

    @Test
    public void doubleSubscribe() {

        TestHelper.checkDoubleOnSubscribeMaybe(new Function1<Maybe<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> invoke(Maybe<Object> m) {
                return m.onTerminateDetach();
            }
        });
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().singleElement().onTerminateDetach());
    }

    @Test
    public void onError() {
        Maybe.error(new TestException())
        .onTerminateDetach()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void onComplete() {
        Maybe.empty()
        .onTerminateDetach()
        .test()
        .assertResult();
    }
}
