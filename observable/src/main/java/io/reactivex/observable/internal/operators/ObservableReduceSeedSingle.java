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

import io.reactivex.common.*;
import io.reactivex.common.exceptions.Exceptions;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.*;

/**
 * Reduce a sequence of values, starting from a seed value and by using
 * an accumulator function and return the last accumulated value.
 *
 * @param <T> the source value type
 * @param <R> the accumulated result type
 */
public final class ObservableReduceSeedSingle<T, R> extends Single<R> {

    final ObservableSource<T> source;

    final R seed;

    final Function2<R, ? super T, R> reducer;

    public ObservableReduceSeedSingle(ObservableSource<T> source, R seed, Function2<R, ? super T, R> reducer) {
        this.source = source;
        this.seed = seed;
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> observer) {
        source.subscribe(new ReduceSeedObserver<T, R>(observer, reducer, seed));
    }

    static final class ReduceSeedObserver<T, R> implements Observer<T>, Disposable {

        final SingleObserver<? super R> actual;

        final Function2<R, ? super T, R> reducer;

        R value;

        Disposable d;

        ReduceSeedObserver(SingleObserver<? super R> actual, Function2<R, ? super T, R> reducer, R value) {
            this.actual = actual;
            this.value = value;
            this.reducer = reducer;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T value) {
            R v = this.value;
            if (v != null) {
                try {
                    this.value = ObjectHelper.requireNonNull(reducer.invoke(v, value), "The reducer returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    d.dispose();
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            R v = value;
            value = null;
            if (v != null) {
                actual.onError(e);
            } else {
                RxJavaCommonPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            R v = value;
            value = null;
            if (v != null) {
                actual.onSuccess(v);
            }
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
