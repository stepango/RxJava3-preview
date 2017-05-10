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
import io.reactivex.common.exceptions.CompositeException
import io.reactivex.common.exceptions.Exceptions
import io.reactivex.common.internal.disposables.DisposableContainer
import io.reactivex.common.internal.functions.ObjectHelper
import io.reactivex.common.internal.utils.ExceptionHelper
import io.reactivex.common.internal.utils.OpenHashSet

/**
 * A disposable container that can hold onto multiple other disposables and
 * offers O(1) add and removal complexity.
 */
class CompositeDisposable : Disposable, DisposableContainer {

    var resources: OpenHashSet<Disposable>? = null

    @Volatile internal var disposed: Boolean = false

    /**
     * Creates an empty CompositeDisposable.
     */
    constructor()

    /**
     * Creates a CompositeDisposables with the given array of initial elements.
     * @param resources the array of Disposables to start with
     */
    constructor(vararg resources: Disposable) {
        this.resources = OpenHashSet<Disposable>(resources.size + 1)
        for (d in resources) {
            ObjectHelper.requireNonNull(d, "Disposable item is null")
            this.resources!!.add(d)
        }
    }

    /**
     * Creates a CompositeDisposables with the given Iterable sequence of initial elements.
     * @param resources the Iterable sequence of Disposables to start with
     */
    constructor(resources: Iterable<Disposable>) {
        this.resources = OpenHashSet<Disposable>()
        for (d in resources) {
            ObjectHelper.requireNonNull(d, "Disposable item is null")
            this.resources!!.add(d)
        }
    }

    override fun dispose() {
        if (disposed) {
            return
        }
        var set: OpenHashSet<Disposable>? = null
        synchronized(this) {
            if (disposed) {
                return
            }
            disposed = true
            set = resources
            resources = null
        }

        dispose(set)
    }

    override fun isDisposed(): Boolean {
        return disposed
    }

    override fun add(d: Disposable): Boolean {
        if (!disposed) {
            synchronized(this) {
                if (!disposed) {
                    var set = resources
                    if (set == null) {
                        set = OpenHashSet<Disposable>()
                        resources = set
                    }
                    set.add(d)
                    return true
                }
            }
        }
        d.dispose()
        return false
    }

    /**
     * Atomically adds the given array of Disposables to the container or
     * disposes them all if the container has been disposed.
     * @param ds the array of Disposables
     * *
     * @return true if the operation was successful, false if the container has been disposed
     */
    fun addAll(vararg ds: Disposable): Boolean {
        if (!disposed) {
            synchronized(this) {
                if (!disposed) {
                    var set = resources
                    if (set == null) {
                        set = OpenHashSet<Disposable>(ds.size + 1)
                        resources = set
                    }
                    for (d in ds) {
                        ObjectHelper.requireNonNull(d, "d is null")
                        set.add(d)
                    }
                    return true
                }
            }
        }
        for (d in ds) {
            d.dispose()
        }
        return false
    }

    override fun remove(d: Disposable): Boolean {
        if (delete(d)) {
            d.dispose()
            return true
        }
        return false
    }

    override fun delete(d: Disposable): Boolean {
        if (disposed) {
            return false
        }
        synchronized(this) {
            if (disposed) {
                return false
            }

            val set = resources
            if (set == null || !set.remove(d)) {
                return false
            }
        }
        return true
    }

    /**
     * Atomically clears the container, then disposes all the previously contained Disposables.
     */
    fun clear() {
        if (disposed) {
            return
        }
        var set: OpenHashSet<Disposable>? = null
        synchronized(this) {
            if (disposed) {
                return
            }

            set = resources
            resources = null
        }

        dispose(set)
    }

    /**
     * Returns the number of currently held Disposables.
     * @return the number of currently held Disposables
     */
    fun size(): Int {
        if (disposed) {
            return 0
        }
        synchronized(this) {
            if (disposed) {
                return 0
            }
            val set = resources
            return set?.size() ?: 0
        }
    }

    /**
     * Dispose the contents of the OpenHashSet by suppressing non-fatal
     * Throwables till the end.
     * @param set the OpenHashSet to dispose elements of
     */
    internal fun dispose(set: OpenHashSet<Disposable>?) {
        if (set == null) {
            return
        }
        var errors: MutableList<Throwable>? = null
        val array = set.keys()
        for (o in array) {
            if (o is Disposable) {
                try {
                    o.dispose()
                } catch (ex: Throwable) {
                    Exceptions.throwIfFatal(ex)
                    if (errors == null) {
                        errors = ArrayList<Throwable>()
                    }
                    errors.add(ex)
                }

            }
        }
        if (errors != null) {
            if (errors.size == 1) {
                throw ExceptionHelper.wrapOrThrow(errors[0])
            }
            throw CompositeException(errors)
        }
    }
}
