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

package io.reactivex.observable.internal.observers;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.functions.Consumer;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.Observer;
import kotlin.jvm.functions.Function0;

public final class LambdaObserver<T> extends AtomicReference<Disposable> implements Observer<T>, Disposable {

    private static final long serialVersionUID = -7251123623727029452L;
    final Consumer<? super T> onNext;
    final Consumer<? super Throwable> onError;
    final Function0 onComplete;
    final Consumer<? super Disposable> onSubscribe;

    public LambdaObserver(Consumer<? super T> onNext, Consumer<? super Throwable> onError,
                          Function0 onComplete,
                          Consumer<? super Disposable> onSubscribe) {
        super();
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onSubscribe = onSubscribe;
    }

    @Override
    public void onSubscribe(Disposable s) {
        if (DisposableHelper.setOnce(this, s)) {
            try {
                onSubscribe.accept(this);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.dispose();
                onError(ex);
            }
        }
    }

    @Override
    public void onNext(T t) {
        if (!isDisposed()) {
            try {
                onNext.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                get().dispose();
                onError(e);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (!isDisposed()) {
            lazySet(DisposableHelper.DISPOSED);
            try {
                onError.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(new CompositeException(t, e));
            }
        }
    }

    @Override
    public void onComplete() {
        if (!isDisposed()) {
            lazySet(DisposableHelper.DISPOSED);
            try {
                onComplete.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
        }
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return get() == DisposableHelper.DISPOSED;
    }
}
