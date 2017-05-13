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
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class SingleDoOnSuccess<T> extends Single<T> {

    final SingleSource<T> source;

    final Function1<? super T, Unit> onSuccess;

    public SingleDoOnSuccess(SingleSource<T> source, Function1<? super T, Unit> onSuccess) {
        this.source = source;
        this.onSuccess = onSuccess;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {

        source.subscribe(new DoOnSuccess(s));
    }

    final class DoOnSuccess implements SingleObserver<T> {
        private final SingleObserver<? super T> s;

        DoOnSuccess(SingleObserver<? super T> s) {
            this.s = s;
        }

        @Override
        public void onSubscribe(Disposable d) {
            s.onSubscribe(d);
        }

        @Override
        public void onSuccess(T value) {
            try {
                onSuccess.invoke(value);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.onError(ex);
                return;
            }
            s.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            s.onError(e);
        }

    }
}
