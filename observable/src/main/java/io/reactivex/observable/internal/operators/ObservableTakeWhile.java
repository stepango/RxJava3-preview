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
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;

public final class ObservableTakeWhile<T> extends AbstractObservableWithUpstream<T, T> {
    final kotlin.jvm.functions.Function1<? super T, Boolean> predicate;

    public ObservableTakeWhile(ObservableSource<T> source, kotlin.jvm.functions.Function1<? super T, Boolean> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new TakeWhileObserver<T>(t, predicate));
    }

    static final class TakeWhileObserver<T> implements Observer<T>, Disposable {
        final Observer<? super T> actual;
        final kotlin.jvm.functions.Function1<? super T, Boolean> predicate;

        Disposable s;

        boolean done;

        TakeWhileObserver(Observer<? super T> actual, kotlin.jvm.functions.Function1<? super T, Boolean> predicate) {
            this.actual = actual;
            this.predicate = predicate;
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
            boolean b;
            try {
                b = predicate.invoke(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.dispose();
                onError(e);
                return;
            }

            if (!b) {
                done = true;
                s.dispose();
                actual.onComplete();
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
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }
    }
}
