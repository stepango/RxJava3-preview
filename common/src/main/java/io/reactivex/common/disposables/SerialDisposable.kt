/**
 * Copyright (c) 2016-present, RxJava Contributors.

 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.common.disposables

import io.reactivex.common.Disposable
import io.reactivex.common.Disposables
import io.reactivex.common.internal.disposables.DisposableHelper
import java.util.concurrent.atomic.AtomicReference

/**
 * A Disposable container that allows atomically updating/replacing the contained
 * Disposable with another Disposable, disposing the old one when updating plus
 * handling the disposition when the container itself is disposed.
 */
class SerialDisposable : Disposable {
    internal val resource: AtomicReference<Disposable>

    /**
     * Constructs an empty SerialDisposable.
     */
    constructor() {
        this.resource = AtomicReference<Disposable>()
    }

    /**
     * Constructs a SerialDisposable with the given initial Disposable instance.
     * @param initialDisposable the initial Disposable instance to use, null allowed
     */
    constructor(initialDisposable: Disposable) {
        this.resource = AtomicReference(initialDisposable)
    }

    /**
     * Atomically: set the next disposable on this container and dispose the previous
     * one (if any) or dispose next if the container has been disposed.
     * @param next the Disposable to set, may be null
     * *
     * @return true if the operation succeeded, false if the container has been disposed
     * *
     * @see .replace
     */
    fun set(next: Disposable): Boolean = DisposableHelper.set(resource, next)

    /**
     * Atomically: set the next disposable on this container but don't dispose the previous
     * one (if any) or dispose next if the container has been disposed.
     * @param next the Disposable to set, may be null
     * *
     * @return true if the operation succeeded, false if the container has been disposed
     * *
     * @see .set
     */
    fun replace(next: Disposable): Boolean = DisposableHelper.replace(resource, next)

    /**
     * Returns the currently contained Disposable or null if this container is empty.
     * @return the current Disposable, may be null
     */
    fun get(): Disposable {
        val d = resource.get()
        if (d === DisposableHelper.DISPOSED) {
            return Disposables.disposed()
        }
        return d
    }

    override fun dispose() {
        DisposableHelper.dispose(resource)
    }

    override fun isDisposed(): Boolean = DisposableHelper.isDisposed(resource.get())
}
