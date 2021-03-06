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

import io.reactivex.common.Disposable;
import io.reactivex.common.annotations.NonNull;
import io.reactivex.common.annotations.Nullable;
import kotlin.jvm.functions.Function0;

/**
 * Abstraction over an RxJava {@link MaybeObserver} that allows associating
 * a resource with it.
 * <p>
 * All methods are safe to call from multiple threads.
 * <p>
 * Calling onSuccess, onError or onComplete multiple times has no effect.
 *
 * @param <T> the value type to emit
 */
public interface MaybeEmitter<T> {

    /**
     * Signal a success value.
     * @param t the value, not null
     */
    void onSuccess(@NonNull T t);

    /**
     * Signal an exception.
     * @param t the exception, not null
     */
    void onError(@NonNull Throwable t);

    /**
     * Signal the completion.
     */
    void onComplete();

    /**
     * Sets a Disposable on this emitter; any previous Disposable
     * or Cancellation will be unsubscribed/cancelled.
     * @param s the disposable, null is allowed
     */
    void setDisposable(@Nullable Disposable s);

    /**
     * Sets a Cancellable on this emitter; any previous Disposable
     * or Cancellation will be unsubscribed/cancelled.
     * @param c the cancellable resource, null is allowed
     */
    void setCancellable(@Nullable Function0 c);

    /**
     * Returns true if the downstream cancelled the sequence.
     * @return true if the downstream cancelled the sequence
     */
    boolean isDisposed();
}
