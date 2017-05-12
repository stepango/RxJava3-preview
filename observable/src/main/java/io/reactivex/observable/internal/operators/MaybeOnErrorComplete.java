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

/**
 * Emits an onComplete if the source emits an onError and the predicate returns true for
 * that Throwable.
 * 
 * @param <T> the value type
 */
public final class MaybeOnErrorComplete<T> extends AbstractMaybeWithUpstream<T, T> {

    final kotlin.jvm.functions.Function1<? super Throwable, Boolean> predicate;

    public MaybeOnErrorComplete(MaybeSource<T> source,
                                kotlin.jvm.functions.Function1<? super Throwable, Boolean> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new OnErrorCompleteMaybeObserver<T>(observer, predicate));
    }

    static final class OnErrorCompleteMaybeObserver<T> implements MaybeObserver<T>, Disposable {

        final MaybeObserver<? super T> actual;

        final kotlin.jvm.functions.Function1<? super Throwable, Boolean> predicate;

        Disposable d;

        OnErrorCompleteMaybeObserver(MaybeObserver<? super T> actual, kotlin.jvm.functions.Function1<? super Throwable, Boolean> predicate) {
            this.actual = actual;
            this.predicate = predicate;
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
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            boolean b;

            try {
                b = predicate.invoke(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(new CompositeException(e, ex));
                return;
            }

            if (b) {
                actual.onComplete();
            } else {
                actual.onError(e);
            }
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public void dispose() {
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }
    }
}
