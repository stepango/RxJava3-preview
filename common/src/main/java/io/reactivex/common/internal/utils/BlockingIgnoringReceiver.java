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

import java.util.concurrent.CountDownLatch;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/**
 * Stores an incoming Throwable (if any) and counts itself down.
 */
public final class BlockingIgnoringReceiver
extends CountDownLatch
        implements Function1<Throwable, Unit>, Function0<Unit> {
    public Throwable error;

    public BlockingIgnoringReceiver() {
        super(1);
    }

    @Override
    public Unit invoke(Throwable e) {
        error = e;
        countDown();
        return Unit.INSTANCE;
    }

    @Override
    public kotlin.Unit invoke() {
        countDown();
        return Unit.INSTANCE;
    }
}
