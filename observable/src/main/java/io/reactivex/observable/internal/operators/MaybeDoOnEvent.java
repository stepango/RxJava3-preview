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
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

/**
 * Calls a BiConsumer with the success, error values of the upstream Maybe or with two nulls if
 * the Maybe completed.
 *
 * @param <T> the value type
 */
public final class MaybeDoOnEvent<T> extends AbstractMaybeWithUpstream<T, T> {

    final Function2<? super T, ? super Throwable, Unit> onEvent;

    public MaybeDoOnEvent(MaybeSource<T> source, Function2<? super T, ? super Throwable, Unit> onEvent) {
        super(source);
        this.onEvent = onEvent;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new DoOnEventMaybeObserver<T>(observer, onEvent));
    }

    static final class DoOnEventMaybeObserver<T> implements MaybeObserver<T>, Disposable {
        final MaybeObserver<? super T> actual;

        final Function2<? super T, ? super Throwable, Unit> onEvent;

        Disposable d;

        DoOnEventMaybeObserver(MaybeObserver<? super T> actual, Function2<? super T, ? super Throwable, Unit> onEvent) {
            this.actual = actual;
            this.onEvent = onEvent;
        }

        @Override
        public void dispose() {
            d.dispose();
            d = DisposableHelper.DISPOSED;
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

        @Override
        public void onSuccess(T value) {
            d = DisposableHelper.DISPOSED;

            try {
                onEvent.invoke(value, null);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            d = DisposableHelper.DISPOSED;

            try {
                onEvent.invoke(null, e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }

            actual.onError(e);
        }

        @Override
        public void onComplete() {
            d = DisposableHelper.DISPOSED;

            try {
                onEvent.invoke(null, null);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            actual.onComplete();
        }
    }
}
