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

import java.util.Arrays;

import io.reactivex.observable.Observable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

public class ObservableFlattenIterableTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().flatMapIterable(new Function1<Object, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Object v) {
                return Arrays.asList(10, 20);
            }
        }));
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Integer>, Object>() {
            @Override
            public Object invoke(Observable<Integer> o) {
                return o.flatMapIterable(new Function1<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> invoke(Object v) {
                        return Arrays.asList(10, 20);
                    }
                });
            }
        }, false, 1, 1, 10, 20);
    }
}
