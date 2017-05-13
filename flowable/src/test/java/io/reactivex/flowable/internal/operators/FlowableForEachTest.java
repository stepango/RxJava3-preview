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

package io.reactivex.flowable.internal.operators;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.common.exceptions.TestException;
import io.reactivex.flowable.Flowable;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;

public class FlowableForEachTest {

    @Test
    public void forEachWile() {
        final List<Object> list = new ArrayList<Object>();

        Flowable.range(1, 5)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        })
                .forEachWhile(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v < 3;
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void forEachWileWithError() {
        final List<Object> list = new ArrayList<Object>();

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new TestException()))
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        })
                .forEachWhile(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
                }, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                list.add(100);
                return Unit.INSTANCE;
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

}
