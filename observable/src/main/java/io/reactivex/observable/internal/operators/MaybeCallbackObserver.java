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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.MaybeObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/**
 * MaybeObserver that delegates the onSuccess, onError and onComplete method calls to callbacks.
 *
 * @param <T> the value type
 */
public final class MaybeCallbackObserver<T>
extends AtomicReference<Disposable>
implements MaybeObserver<T>, Disposable {


    private static final long serialVersionUID = -6076952298809384986L;

    final Function1<? super T, Unit> onSuccess;

    final Function1<? super Throwable, Unit> onError;

    final Function0 onComplete;

    public MaybeCallbackObserver(Function1<? super T, Unit> onSuccess, Function1<? super Throwable, Unit> onError,
                                 Function0 onComplete) {
        super();
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
    }

    @Override
    public void onSuccess(T value) {
        lazySet(DisposableHelper.DISPOSED);
        try {
            onSuccess.invoke(value);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaCommonPlugins.onError(ex);
        }
    }

    @Override
    public void onError(Throwable e) {
        lazySet(DisposableHelper.DISPOSED);
        try {
            onError.invoke(e);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaCommonPlugins.onError(new CompositeException(e, ex));
        }
    }

    @Override
    public void onComplete() {
        lazySet(DisposableHelper.DISPOSED);
        try {
            onComplete.invoke();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaCommonPlugins.onError(ex);
        }
    }


}
