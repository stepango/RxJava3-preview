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
import io.reactivex.common.annotations.Experimental;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Calls a consumer after pushing the current item to the downstream.
 * @param <T> the value type
 * @since 2.0.1 - experimental
 */
@Experimental
public final class MaybeDoAfterSuccess<T> extends AbstractMaybeWithUpstream<T, T> {

    final Function1<? super T, Unit> onAfterSuccess;

    public MaybeDoAfterSuccess(MaybeSource<T> source, Function1<? super T, Unit> onAfterSuccess) {
        super(source);
        this.onAfterSuccess = onAfterSuccess;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> s) {
        source.subscribe(new DoAfterObserver<T>(s, onAfterSuccess));
    }

    static final class DoAfterObserver<T> implements MaybeObserver<T>, Disposable {

        final MaybeObserver<? super T> actual;

        final Function1<? super T, Unit> onAfterSuccess;

        Disposable d;

        DoAfterObserver(MaybeObserver<? super T> actual, Function1<? super T, Unit> onAfterSuccess) {
            this.actual = actual;
            this.onAfterSuccess = onAfterSuccess;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T t) {
            actual.onSuccess(t);

            try {
                onAfterSuccess.invoke(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
             // remember, onSuccess is a terminal event and we can't call onError
                RxJavaCommonPlugins.onError(ex);
            }
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
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
