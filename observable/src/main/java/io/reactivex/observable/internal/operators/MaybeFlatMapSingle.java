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

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import kotlin.jvm.functions.Function1;

/**
 * Maps the success value of the source MaybeSource into a Single.
 * @param <T> the input value type
 * @param <R> the result value type
 */
public final class MaybeFlatMapSingle<T, R> extends Single<R> {

    final MaybeSource<T> source;

    final Function1<? super T, ? extends SingleSource<? extends R>> mapper;

    public MaybeFlatMapSingle(MaybeSource<T> source, Function1<? super T, ? extends SingleSource<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> actual) {
        source.subscribe(new FlatMapMaybeObserver<T, R>(actual, mapper));
    }

    static final class FlatMapMaybeObserver<T, R>
    extends AtomicReference<Disposable>
    implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = 4827726964688405508L;

        final SingleObserver<? super R> actual;

        final Function1<? super T, ? extends SingleSource<? extends R>> mapper;

        FlatMapMaybeObserver(SingleObserver<? super R> actual, Function1<? super T, ? extends SingleSource<? extends R>> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            SingleSource<? extends R> ss;

            try {
                ss = ObjectHelper.requireNonNull(mapper.invoke(value), "The mapper returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                onError(ex);
                return;
            }

            if (!isDisposed()) {
                ss.subscribe(new FlatMapSingleObserver<R>(this, actual));
            }
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            actual.onError(new NoSuchElementException());
        }
    }

    static final class FlatMapSingleObserver<R> implements SingleObserver<R> {

        final AtomicReference<Disposable> parent;

        final SingleObserver<? super R> actual;

        FlatMapSingleObserver(AtomicReference<Disposable> parent, SingleObserver<? super R> actual) {
            this.parent = parent;
            this.actual = actual;
        }

        @Override
        public void onSubscribe(final Disposable d) {
            DisposableHelper.replace(parent, d);
        }

        @Override
        public void onSuccess(final R value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(final Throwable e) {
            actual.onError(e);
        }
    }
}
