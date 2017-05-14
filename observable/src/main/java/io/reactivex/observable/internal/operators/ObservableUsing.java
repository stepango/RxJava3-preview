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
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import kotlin.jvm.functions.Function1;

public final class ObservableUsing<T, D> extends Observable<T> {
    final Callable<? extends D> resourceSupplier;
    final Function1<? super D, ? extends ObservableSource<? extends T>> sourceSupplier;
    final Function1<? super D, kotlin.Unit> disposer;
    final boolean eager;

    public ObservableUsing(Callable<? extends D> resourceSupplier,
                           Function1<? super D, ? extends ObservableSource<? extends T>> sourceSupplier,
                           Function1<? super D, kotlin.Unit> disposer,
                           boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.sourceSupplier = sourceSupplier;
        this.disposer = disposer;
        this.eager = eager;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        D resource;

        try {
            resource = resourceSupplier.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, s);
            return;
        }

        ObservableSource<? extends T> source;
        try {
            source = sourceSupplier.invoke(resource);
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            try {
                disposer.invoke(resource);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(new CompositeException(e, ex), s);
                return;
            }
            EmptyDisposable.error(e, s);
            return;
        }

        UsingObserver<T, D> us = new UsingObserver<T, D>(s, resource, disposer, eager);

        source.subscribe(us);
    }

    static final class UsingObserver<T, D> extends AtomicBoolean implements Observer<T>, Disposable {

        private static final long serialVersionUID = 5904473792286235046L;

        final Observer<? super T> actual;
        final D resource;
        final Function1<? super D, kotlin.Unit> disposer;
        final boolean eager;

        Disposable s;

        UsingObserver(Observer<? super T> actual, D resource, Function1<? super D, kotlin.Unit> disposer, boolean eager) {
            this.actual = actual;
            this.resource = resource;
            this.disposer = disposer;
            this.eager = eager;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (eager) {
                if (compareAndSet(false, true)) {
                    try {
                        disposer.invoke(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        t = new CompositeException(t, e);
                    }
                }

                s.dispose();
                actual.onError(t);
            } else {
                actual.onError(t);
                s.dispose();
                disposeAfter();
            }
        }

        @Override
        public void onComplete() {
            if (eager) {
                if (compareAndSet(false, true)) {
                    try {
                        disposer.invoke(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        actual.onError(e);
                        return;
                    }
                }

                s.dispose();
                actual.onComplete();
            } else {
                actual.onComplete();
                s.dispose();
                disposeAfter();
            }
        }

        @Override
        public void dispose() {
            disposeAfter();
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return get();
        }

        void disposeAfter() {
            if (compareAndSet(false, true)) {
                try {
                    disposer.invoke(resource);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    // can't call actual.onError unless it is serialized, which is expensive
                    RxJavaCommonPlugins.onError(e);
                }
            }
        }
    }
}
