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

import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import kotlin.jvm.functions.Function1;

public class ObservableCountTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).count());

        TestHelper.checkDisposed(Observable.just(1).count().toObservable());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Long>>() {
            @Override
            public ObservableSource<Long> invoke(Observable<Object> o) {
                return o.count().toObservable();
            }
        });

        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function1<Observable<Object>, SingleSource<Long>>() {
            @Override
            public SingleSource<Long> invoke(Observable<Object> o) {
                return o.count();
            }
        });
    }
}
