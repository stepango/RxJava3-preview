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
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public final class ObservableDoOnEach<T> extends AbstractObservableWithUpstream<T, T> {
    final Function1<? super T, Unit> onNext;
    final Function1<? super Throwable, Unit> onError;
    final Function0 onComplete;
    final Function0 onAfterTerminate;

    public ObservableDoOnEach(ObservableSource<T> source, Function1<? super T, Unit> onNext,
                              Function1<? super Throwable, Unit> onError,
                              Function0 onComplete,
                              Function0 onAfterTerminate) {
        super(source);
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onAfterTerminate = onAfterTerminate;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new DoOnEachObserver<T>(t, onNext, onError, onComplete, onAfterTerminate));
    }

    static final class DoOnEachObserver<T> implements Observer<T>, Disposable {
        final Observer<? super T> actual;
        final Function1<? super T, Unit> onNext;
        final Function1<? super Throwable, Unit> onError;
        final Function0 onComplete;
        final Function0 onAfterTerminate;

        Disposable s;

        boolean done;

        DoOnEachObserver(
                Observer<? super T> actual,
                Function1<? super T, Unit> onNext,
                Function1<? super Throwable, Unit> onError,
                Function0 onComplete,
                Function0 onAfterTerminate) {
            this.actual = actual;
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onAfterTerminate = onAfterTerminate;
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
            if (done) {
                return;
            }
            try {
                onNext.invoke(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.dispose();
                onError(e);
                return;
            }

            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaCommonPlugins.onError(t);
                return;
            }
            done = true;
            try {
                onError.invoke(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                t = new CompositeException(t, e);
            }
            actual.onError(t);

            try {
                onAfterTerminate.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            try {
                onComplete.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
                return;
            }

            done = true;
            actual.onComplete();

            try {
                onAfterTerminate.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
        }
    }
}
