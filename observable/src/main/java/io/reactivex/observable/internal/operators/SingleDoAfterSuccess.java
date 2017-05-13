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
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Calls a consumer after pushing the current item to the downstream.
 * @param <T> the value type
 * @since 2.0.1 - experimental
 */
@Experimental
public final class SingleDoAfterSuccess<T> extends Single<T> {

    final SingleSource<T> source;

    final Function1<? super T, Unit> onAfterSuccess;

    public SingleDoAfterSuccess(SingleSource<T> source, Function1<? super T, Unit> onAfterSuccess) {
        this.source = source;
        this.onAfterSuccess = onAfterSuccess;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> s) {
        source.subscribe(new DoAfterObserver<T>(s, onAfterSuccess));
    }

    static final class DoAfterObserver<T> implements SingleObserver<T>, Disposable {

        final SingleObserver<? super T> actual;

        final Function1<? super T, Unit> onAfterSuccess;

        Disposable d;

        DoAfterObserver(SingleObserver<? super T> actual, Function1<? super T, Unit> onAfterSuccess) {
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
        public void dispose() {
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }
    }
}
