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
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class SingleDoOnError<T> extends Single<T> {

    final SingleSource<T> source;

    final Function1<? super Throwable, Unit> onError;

    public SingleDoOnError(SingleSource<T> source, Function1<? super Throwable, Unit> onError) {
        this.source = source;
        this.onError = onError;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {

        source.subscribe(new DoOnError(s));
    }

    final class DoOnError implements SingleObserver<T> {
        private final SingleObserver<? super T> s;

        DoOnError(SingleObserver<? super T> s) {
            this.s = s;
        }

        @Override
        public void onSubscribe(Disposable d) {
            s.onSubscribe(d);
        }

        @Override
        public void onSuccess(T value) {
            s.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            try {
                onError.invoke(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }
            s.onError(e);
        }

    }
}
