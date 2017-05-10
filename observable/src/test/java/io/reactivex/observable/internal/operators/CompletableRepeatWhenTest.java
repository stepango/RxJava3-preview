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

import io.reactivex.common.functions.Function;
import io.reactivex.common.functions.Predicate;
import io.reactivex.observable.Completable;
import io.reactivex.observable.Observable;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertEquals;

public class CompletableRepeatWhenTest {
    @Test
    public void whenCounted() {

        final int[] counter = { 0 };

        Completable.fromAction(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                counter[0]++;
                return Unit.INSTANCE;
            }
        })
        .repeatWhen(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> f) throws Exception {
                final int[] j = { 3 };
                return f.takeWhile(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return j[0]-- != 0;
                    }
                });
            }
        })
        .subscribe();

        assertEquals(4, counter[0]);
    }
}
