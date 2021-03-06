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

package io.reactivex.interop.internal.operators;

import org.junit.Test;

import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.interop.TestHelper;

import static io.reactivex.interop.RxJava3Interop.reduceWith;

public class FlowableReduceWithSingleTest {

    @Test
    public void normal() {
        reduceWith(Flowable.range(1, 5)
        , Functions.justCallable(1), new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return a + b;
            }
        })
        .test()
        .assertResult(16);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(reduceWith(Flowable.range(1, 5)
        , Functions.justCallable(1), new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return a + b;
            }
        }));
    }
}
