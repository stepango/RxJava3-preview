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

package io.reactivex.common.internal.disposables;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import kotlin.jvm.functions.Function0;

/**
 * A disposable container that wraps a Cancellable instance.
 * <p>
 * Watch out for the AtomicReference API leak!
 */
public final class CancellableDisposable extends AtomicReference<Function0>
implements Disposable {


    private static final long serialVersionUID = 5718521705281392066L;

    public CancellableDisposable(Function0 cancellable) {
        super(cancellable);
    }

    @Override
    public boolean isDisposed() {
        return get() == null;
    }

    @Override
    public void dispose() {
        if (get() != null) {
            Function0 c = getAndSet(null);
            if (c != null) {
                try {
                    c.invoke();
                } catch (Exception ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaCommonPlugins.onError(ex);
                }
            }
        }
    }
}
