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
 * Reduce a sequence of values into a single value via an aggregator function and emit the final value or complete
 * if the source is empty.
 *
 * @param <T> the source and result value type
 */
public final class ObservableReduceMaybe<T> extends Maybe<T> {

    final ObservableSource<T> source;

    final Function2<T, T, T> reducer;

    public ObservableReduceMaybe(ObservableSource<T> source, Function2<T, T, T> reducer) {
        this.source = source;
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new ReduceObserver<T>(observer, reducer));
    }

    static final class ReduceObserver<T> implements Observer<T>, Disposable {

        final MaybeObserver<? super T> actual;

        final Function2<T, T, T> reducer;

        boolean done;

        T value;

        Disposable d;

        ReduceObserver(MaybeObserver<? super T> observer, Function2<T, T, T> reducer) {
            this.actual = observer;
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
            if (!done) {
                T v = this.value;

                if (v == null) {
                    this.value = value;
                } else {
                    try {
                        this.value = ObjectHelper.requireNonNull(reducer.invoke(v, value), "The reducer returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        d.dispose();
                        onError(ex);
                    }
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaCommonPlugins.onError(e);
                return;
            }
            done = true;
            value = null;
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            T v = value;
            value = null;
            if (v != null) {
                actual.onSuccess(v);
            } else {
                actual.onComplete();
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
