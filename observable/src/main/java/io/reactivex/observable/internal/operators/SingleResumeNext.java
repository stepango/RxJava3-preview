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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.internal.observers.ResumeSingleObserver;
import kotlin.jvm.functions.Function1;

public final class SingleResumeNext<T> extends Single<T> {
    final SingleSource<? extends T> source;

    final Function1<? super Throwable, ? extends SingleSource<? extends T>> nextFunction;

    public SingleResumeNext(SingleSource<? extends T> source,
                            Function1<? super Throwable, ? extends SingleSource<? extends T>> nextFunction) {
        this.source = source;
        this.nextFunction = nextFunction;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {
        source.subscribe(new ResumeMainSingleObserver<T>(s, nextFunction));
    }

    static final class ResumeMainSingleObserver<T> extends AtomicReference<Disposable>
    implements SingleObserver<T>, Disposable {
        private static final long serialVersionUID = -5314538511045349925L;

        final SingleObserver<? super T> actual;

        final Function1<? super Throwable, ? extends SingleSource<? extends T>> nextFunction;

        ResumeMainSingleObserver(SingleObserver<? super T> actual,
                                 Function1<? super Throwable, ? extends SingleSource<? extends T>> nextFunction) {
            this.actual = actual;
            this.nextFunction = nextFunction;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            SingleSource<? extends T> source;

            try {
                source = ObjectHelper.requireNonNull(nextFunction.invoke(e), "The nextFunction returned a null SingleSource.");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(new CompositeException(e, ex));
                return;
            }

            source.subscribe(new ResumeSingleObserver<T>(this, actual));
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }
}
