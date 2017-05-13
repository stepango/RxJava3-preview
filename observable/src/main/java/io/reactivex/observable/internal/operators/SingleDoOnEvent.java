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
import kotlin.jvm.functions.Function2;

public final class SingleDoOnEvent<T> extends Single<T> {
    final SingleSource<T> source;

    final Function2<? super T, ? super Throwable, Unit> onEvent;

    public SingleDoOnEvent(SingleSource<T> source, Function2<? super T, ? super Throwable, Unit> onEvent) {
        this.source = source;
        this.onEvent = onEvent;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {

        source.subscribe(new DoOnEvent(s));
    }

    final class DoOnEvent implements SingleObserver<T> {
        private final SingleObserver<? super T> s;

        DoOnEvent(SingleObserver<? super T> s) {
            this.s = s;
        }

        @Override
        public void onSubscribe(Disposable d) {
            s.onSubscribe(d);
        }

        @Override
        public void onSuccess(T value) {
            try {
                onEvent.invoke(value, null);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.onError(ex);
                return;
            }

            s.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            try {
                onEvent.invoke(null, e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }
            s.onError(e);
        }
    }
}
