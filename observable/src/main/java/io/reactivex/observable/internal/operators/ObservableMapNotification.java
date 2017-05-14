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

import java.util.concurrent.Callable;

import io.reactivex.common.Disposable;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import kotlin.jvm.functions.Function1;

public final class ObservableMapNotification<T, R> extends AbstractObservableWithUpstream<T, ObservableSource<? extends R>> {

    final Function1<? super T, ? extends ObservableSource<? extends R>> onNextMapper;
    final Function1<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper;
    final Callable<? extends ObservableSource<? extends R>> onCompleteSupplier;

    public ObservableMapNotification(
            ObservableSource<T> source,
            Function1<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
            Function1<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
            Callable<? extends ObservableSource<? extends R>> onCompleteSupplier) {
        super(source);
        this.onNextMapper = onNextMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }

    @Override
    public void subscribeActual(Observer<? super ObservableSource<? extends R>> t) {
        source.subscribe(new MapNotificationObserver<T, R>(t, onNextMapper, onErrorMapper, onCompleteSupplier));
    }

    static final class MapNotificationObserver<T, R>
    implements Observer<T>, Disposable {
        final Observer<? super ObservableSource<? extends R>> actual;
        final Function1<? super T, ? extends ObservableSource<? extends R>> onNextMapper;
        final Function1<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper;
        final Callable<? extends ObservableSource<? extends R>> onCompleteSupplier;

        Disposable s;

        MapNotificationObserver(Observer<? super ObservableSource<? extends R>> actual,
                                Function1<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
                                Function1<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
                                Callable<? extends ObservableSource<? extends R>> onCompleteSupplier) {
            this.actual = actual;
            this.onNextMapper = onNextMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }


        @Override
        public void dispose() {
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }


        @Override
        public void onNext(T t) {
            ObservableSource<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(onNextMapper.invoke(t), "The onNext Observable returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            actual.onNext(p);
        }

        @Override
        public void onError(Throwable t) {
            ObservableSource<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(onErrorMapper.invoke(t), "The onError Observable returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            actual.onNext(p);
            actual.onComplete();
        }

        @Override
        public void onComplete() {
            ObservableSource<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(onCompleteSupplier.call(), "The onComplete Observable returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            actual.onNext(p);
            actual.onComplete();
        }
    }
}
