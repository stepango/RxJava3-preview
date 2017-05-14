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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import kotlin.jvm.functions.Function1;

/**
 * Creates a resource and a dependent Maybe for each incoming Observer and optionally
 * disposes the resource eagerly (before the terminal event is send out).
 *
 * @param <T> the value type
 * @param <D> the resource type
 */
public final class MaybeUsing<T, D> extends Maybe<T> {

    final Callable<? extends D> resourceSupplier;

    final Function1<? super D, ? extends MaybeSource<? extends T>> sourceSupplier;

    final Function1<? super D, kotlin.Unit> resourceDisposer;

    final boolean eager;

    public MaybeUsing(Callable<? extends D> resourceSupplier,
                      Function1<? super D, ? extends MaybeSource<? extends T>> sourceSupplier,
                      Function1<? super D, kotlin.Unit> resourceDisposer,
                      boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.sourceSupplier = sourceSupplier;
        this.resourceDisposer = resourceDisposer;
        this.eager = eager;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        D resource;

        try {
            resource = resourceSupplier.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        MaybeSource<? extends T> source;

        try {
            source = ObjectHelper.requireNonNull(sourceSupplier.invoke(resource), "The sourceSupplier returned a null MaybeSource");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            if (eager) {
                try {
                    resourceDisposer.invoke(resource);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    EmptyDisposable.error(new CompositeException(ex, exc), observer);
                    return;
                }
            }

            EmptyDisposable.error(ex, observer);

            if (!eager) {
                try {
                    resourceDisposer.invoke(resource);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    RxJavaCommonPlugins.onError(exc);
                }
            }
            return;
        }

        source.subscribe(new UsingObserver<T, D>(observer, resource, resourceDisposer, eager));
    }

    static final class UsingObserver<T, D>
    extends AtomicReference<Object>
    implements MaybeObserver<T>, Disposable {


        private static final long serialVersionUID = -674404550052917487L;

        final MaybeObserver<? super T> actual;

        final Function1<? super D, kotlin.Unit> disposer;

        final boolean eager;

        Disposable d;

        UsingObserver(MaybeObserver<? super T> actual, D resource, Function1<? super D, kotlin.Unit> disposer, boolean eager) {
            super(resource);
            this.actual = actual;
            this.disposer = disposer;
            this.eager = eager;
        }

        @Override
        public void dispose() {
            d.dispose();
            d = DisposableHelper.DISPOSED;
            disposeResourceAfter();
        }

        @SuppressWarnings("unchecked")
        void disposeResourceAfter() {
            Object resource = getAndSet(this);
            if (resource != this) {
                try {
                    disposer.invoke((D) resource);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaCommonPlugins.onError(ex);
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSuccess(T value) {
            d = DisposableHelper.DISPOSED;
            if (eager) {
                Object resource = getAndSet(this);
                if (resource != this) {
                    try {
                        disposer.invoke((D) resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        actual.onError(ex);
                        return;
                    }
                } else {
                    return;
                }
            }

            actual.onSuccess(value);

            if (!eager) {
                disposeResourceAfter();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onError(Throwable e) {
            d = DisposableHelper.DISPOSED;
            if (eager) {
                Object resource = getAndSet(this);
                if (resource != this) {
                    try {
                        disposer.invoke((D) resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        e = new CompositeException(e, ex);
                    }
                } else {
                    return;
                }
            }

            actual.onError(e);

            if (!eager) {
                disposeResourceAfter();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onComplete() {
            d = DisposableHelper.DISPOSED;
            if (eager) {
                Object resource = getAndSet(this);
                if (resource != this) {
                    try {
                        disposer.invoke((D) resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        actual.onError(ex);
                        return;
                    }
                } else {
                    return;
                }
            }

            actual.onComplete();

            if (!eager) {
                disposeResourceAfter();
            }
        }
    }
}
