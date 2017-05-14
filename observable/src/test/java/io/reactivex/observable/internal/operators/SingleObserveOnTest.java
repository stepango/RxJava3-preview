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

import java.util.concurrent.TimeUnit;

import io.reactivex.common.Schedulers;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import kotlin.jvm.functions.Function1;

public class SingleObserveOnTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).observeOn(Schedulers.single()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function1<Single<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> invoke(Single<Object> s) {
                return s.observeOn(Schedulers.single());
            }
        });
    }

    @Test
    public void error() {
        Single.error(new TestException())
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }
}
