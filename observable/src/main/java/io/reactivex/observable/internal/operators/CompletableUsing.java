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
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.CompletableSource;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class CompletableUsing<R> extends Completable {

    final Callable<R> resourceSupplier;
    final Function<? super R, ? extends CompletableSource> completableFunction;
    final Function1<? super R, Unit> disposer;
    final boolean eager;

    public CompletableUsing(Callable<R> resourceSupplier,
                            Function<? super R, ? extends CompletableSource> completableFunction, Function1<? super R, Unit> disposer,
                            boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.completableFunction = completableFunction;
        this.disposer = disposer;
        this.eager = eager;
    }


    @Override
    protected void subscribeActual(CompletableObserver observer) {
        R resource;

        try {
            resource = resourceSupplier.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        CompletableSource source;

        try {
            source = ObjectHelper.requireNonNull(completableFunction.apply(resource), "The completableFunction returned a null CompletableSource");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            if (eager) {
                try {
                    disposer.invoke(resource);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    EmptyDisposable.error(new CompositeException(ex, exc), observer);
                    return;
                }
            }

            EmptyDisposable.error(ex, observer);

            if (!eager) {
                try {
                    disposer.invoke(resource);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    RxJavaCommonPlugins.onError(exc);
                }
            }
            return;
        }

        source.subscribe(new UsingObserver<R>(observer, resource, disposer, eager));
    }

    static final class UsingObserver<R>
    extends AtomicReference<Object>
    implements CompletableObserver, Disposable {


        private static final long serialVersionUID = -674404550052917487L;

        final CompletableObserver actual;

        final Function1<? super R, Unit> disposer;

        final boolean eager;

        Disposable d;

        UsingObserver(CompletableObserver actual, R resource, Function1<? super R, Unit> disposer, boolean eager) {
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
                    disposer.invoke((R) resource);
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
        public void onError(Throwable e) {
            d = DisposableHelper.DISPOSED;
            if (eager) {
                Object resource = getAndSet(this);
                if (resource != this) {
                    try {
                        disposer.invoke((R) resource);
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
                        disposer.invoke((R) resource);
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
