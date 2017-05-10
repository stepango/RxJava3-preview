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

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.functions.Consumer;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.CompletableSource;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import kotlin.jvm.functions.Function0;

public final class CompletablePeek extends Completable {

    final CompletableSource source;
    final Consumer<? super Disposable> onSubscribe;
    final Consumer<? super Throwable> onError;
    final Function0 onComplete;
    final Function0 onTerminate;
    final Function0 onAfterTerminate;
    final Function0 onDispose;

    public CompletablePeek(CompletableSource source, Consumer<? super Disposable> onSubscribe,
                           Consumer<? super Throwable> onError,
                           Function0 onComplete,
                           Function0 onTerminate,
                           Function0 onAfterTerminate,
                           Function0 onDispose) {
        this.source = source;
        this.onSubscribe = onSubscribe;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onTerminate = onTerminate;
        this.onAfterTerminate = onAfterTerminate;
        this.onDispose = onDispose;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {

        source.subscribe(new CompletableObserverImplementation(s));
    }

    final class CompletableObserverImplementation implements CompletableObserver, Disposable {

        final CompletableObserver actual;

        Disposable d;

        CompletableObserverImplementation(CompletableObserver actual) {
            this.actual = actual;
        }


        @Override
        public void onSubscribe(final Disposable d) {
            try {
                onSubscribe.accept(d);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                d.dispose();
                this.d = DisposableHelper.DISPOSED;
                EmptyDisposable.error(ex, actual);
                return;
            }
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (d == DisposableHelper.DISPOSED) {
                RxJavaCommonPlugins.onError(e);
                return;
            }
            try {
                onError.accept(e);
                onTerminate.invoke();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }

            actual.onError(e);

            doAfter();
        }

        @Override
        public void onComplete() {
            if (d == DisposableHelper.DISPOSED) {
                return;
            }

            try {
                onComplete.invoke();
                onTerminate.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            actual.onComplete();

            doAfter();
        }

        void doAfter() {
            try {
                onAfterTerminate.invoke();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaCommonPlugins.onError(ex);
            }
        }

        @Override
        public void dispose() {
            try {
                onDispose.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }
    }
}
