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

package io.reactivex.observable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ObservableWindowTests {

    @Test
    public void testWindow() {
        final ArrayList<List<Integer>> lists = new ArrayList<List<Integer>>();

        Observable.concat(
            Observable.just(1, 2, 3, 4, 5, 6)
            .window(3)
                    .map(new Function1<Observable<Integer>, Observable<List<Integer>>>() {
                @Override
                public Observable<List<Integer>> invoke(Observable<Integer> xs) {
                    return xs.toList().toObservable();
                }
            })
        )
                .blockingForEach(new Function1<List<Integer>, kotlin.Unit>() {
            @Override
            public Unit invoke(List<Integer> xs) {
                lists.add(xs);
                return Unit.INSTANCE;
            }
        });

        assertArrayEquals(lists.get(0).toArray(new Integer[3]), new Integer[] { 1, 2, 3 });
        assertArrayEquals(lists.get(1).toArray(new Integer[3]), new Integer[] { 4, 5, 6 });
        assertEquals(2, lists.size());

    }
}
