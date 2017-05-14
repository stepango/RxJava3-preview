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

package io.reactivex.common.internal.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import kotlin.jvm.functions.Function1;

public enum ArrayListSupplier implements Callable<List<Object>>, Function1<Object, List<Object>> {
    INSTANCE;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Callable<List<T>> asCallable() {
        return (Callable)INSTANCE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, O> Function1<O, List<T>> asFunction() {
        return (Function1) INSTANCE;
    }

    @Override
    public List<Object> call() throws Exception {
        return new ArrayList<Object>();
    }

    @Override
    public List<Object> invoke(Object o) {
        return new ArrayList<Object>();
    }
}
