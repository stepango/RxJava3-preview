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
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import kotlin.jvm.functions.Function2;

public final class ObservableCollect<T, U> extends AbstractObservableWithUpstream<T, U> {
    final Callable<? extends U> initialSupplier;
    final Function2<? super U, ? super T, kotlin.Unit> collector;

    public ObservableCollect(ObservableSource<T> source,
                             Callable<? extends U> initialSupplier, Function2<? super U, ? super T, kotlin.Unit> collector) {
        super(source);
        this.initialSupplier = initialSupplier;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(Observer<? super U> t) {
        U u;
        try {
            u = ObjectHelper.requireNonNull(initialSupplier.call(), "The initialSupplier returned a null value");
        } catch (Throwable e) {
            EmptyDisposable.error(e, t);
            return;
        }

        source.subscribe(new CollectObserver<T, U>(t, u, collector));

    }

    static final class CollectObserver<T, U> implements Observer<T>, Disposable {
        final Observer<? super U> actual;
        final Function2<? super U, ? super T, kotlin.Unit> collector;
        final U u;

        Disposable s;

        boolean done;

        CollectObserver(Observer<? super U> actual, U u, Function2<? super U, ? super T, kotlin.Unit> collector) {
            this.actual = actual;
            this.collector = collector;
            this.u = u;
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
                collector.invoke(u, t);
            } catch (Throwable e) {
                s.dispose();
                onError(e);
            }
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
            actual.onNext(u);
            actual.onComplete();
        }
    }
}
