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

import java.util.Arrays;
import java.util.NoSuchElementException;

import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import io.reactivex.observable.internal.operators.SingleZipArray.ZipCoordinator;
import kotlin.jvm.functions.Function1;

public final class SingleZipIterable<T, R> extends Single<R> {

    final Iterable<? extends SingleSource<? extends T>> sources;

    final Function1<? super Object[], ? extends R> zipper;

    public SingleZipIterable(Iterable<? extends SingleSource<? extends T>> sources, Function1<? super Object[], ? extends R> zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> observer) {
        @SuppressWarnings("unchecked")
        SingleSource<? extends T>[] a = new SingleSource[8];
        int n = 0;

        try {
            for (SingleSource<? extends T> source : sources) {
                if (source == null) {
                    EmptyDisposable.error(new NullPointerException("One of the sources is null"), observer);
                    return;
                }
                if (n == a.length) {
                    a = Arrays.copyOf(a, n + (n >> 2));
                }
                a[n++] = source;
            }
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        if (n == 0) {
            EmptyDisposable.error(new NoSuchElementException(), observer);
            return;
        }

        if (n == 1) {
            a[0].subscribe(new SingleMap.MapSingleObserver<T, R>(observer, new SingletonArrayFunc()));
            return;
        }

        ZipCoordinator<T, R> parent = new ZipCoordinator<T, R>(observer, n, zipper);

        observer.onSubscribe(parent);

        for (int i = 0; i < n; i++) {
            if (parent.isDisposed()) {
                return;
            }

            a[i].subscribe(parent.observers[i]);
        }
    }

    final class SingletonArrayFunc implements Function1<T, R> {
        @Override
        public R invoke(T t) {
            return zipper.invoke(new Object[]{t});
        }
    }
}
