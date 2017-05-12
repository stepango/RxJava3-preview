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
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import kotlin.jvm.functions.Function1;

/**
 * Filters the upstream SingleSource via a predicate, returning the success item or completing if
 * the predicate returns false.
 *
 * @param <T> the upstream value type
 */
public final class MaybeFilterSingle<T> extends Maybe<T> {
    final SingleSource<T> source;

    final Function1<? super T, Boolean> predicate;

    public MaybeFilterSingle(SingleSource<T> source, Function1<? super T, Boolean> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new FilterMaybeObserver<T>(observer, predicate));
    }

    static final class FilterMaybeObserver<T> implements SingleObserver<T>, Disposable {

        final MaybeObserver<? super T> actual;

        final Function1<? super T, Boolean> predicate;

        Disposable d;

        FilterMaybeObserver(MaybeObserver<? super T> actual, Function1<? super T, Boolean> predicate) {
            this.actual = actual;
            this.predicate = predicate;
        }

        @Override
        public void dispose() {
            Disposable d = this.d;
            this.d = DisposableHelper.DISPOSED;
            d.dispose();
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
            boolean b;

            try {
                b = predicate.invoke(value);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            if (b) {
                actual.onSuccess(value);
            } else {
                actual.onComplete();
            }
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
    }

}
