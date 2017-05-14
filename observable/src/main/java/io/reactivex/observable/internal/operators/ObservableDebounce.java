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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.DisposableHelper;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.observers.DisposableObserver;
import io.reactivex.observable.observers.SerializedObserver;
import kotlin.jvm.functions.Function1;

public final class ObservableDebounce<T, U> extends AbstractObservableWithUpstream<T, T> {
    final Function1<? super T, ? extends ObservableSource<U>> debounceSelector;

    public ObservableDebounce(ObservableSource<T> source, Function1<? super T, ? extends ObservableSource<U>> debounceSelector) {
        super(source);
        this.debounceSelector = debounceSelector;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new DebounceObserver<T, U>(new SerializedObserver<T>(t), debounceSelector));
    }

    static final class DebounceObserver<T, U>
    implements Observer<T>, Disposable {
        final Observer<? super T> actual;
        final Function1<? super T, ? extends ObservableSource<U>> debounceSelector;

        Disposable s;

        final AtomicReference<Disposable> debouncer = new AtomicReference<Disposable>();

        volatile long index;

        boolean done;

        DebounceObserver(Observer<? super T> actual,
                         Function1<? super T, ? extends ObservableSource<U>> debounceSelector) {
            this.actual = actual;
            this.debounceSelector = debounceSelector;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            long idx = index + 1;
            index = idx;

            Disposable d = debouncer.get();
            if (d != null) {
                d.dispose();
            }

            ObservableSource<U> p;

            try {
                p = ObjectHelper.requireNonNull(debounceSelector.invoke(t), "The Observable supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                dispose();
                actual.onError(e);
                return;
            }

            DebounceInnerObserver<T, U> dis = new DebounceInnerObserver<T, U>(this, idx, t);

            if (debouncer.compareAndSet(d, dis)) {
                p.subscribe(dis);
            }
        }

        @Override
        public void onError(Throwable t) {
            DisposableHelper.dispose(debouncer);
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            Disposable d = debouncer.get();
            if (d != DisposableHelper.DISPOSED) {
                @SuppressWarnings("unchecked")
                DebounceInnerObserver<T, U> dis = (DebounceInnerObserver<T, U>)d;
                dis.emit();
                DisposableHelper.dispose(debouncer);
                actual.onComplete();
            }
        }

        @Override
        public void dispose() {
            s.dispose();
            DisposableHelper.dispose(debouncer);
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }

        void emit(long idx, T value) {
            if (idx == index) {
                actual.onNext(value);
            }
        }

        static final class DebounceInnerObserver<T, U> extends DisposableObserver<U> {
            final DebounceObserver<T, U> parent;
            final long index;
            final T value;

            boolean done;

            final AtomicBoolean once = new AtomicBoolean();

            DebounceInnerObserver(DebounceObserver<T, U> parent, long index, T value) {
                this.parent = parent;
                this.index = index;
                this.value = value;
            }

            @Override
            public void onNext(U t) {
                if (done) {
                    return;
                }
                done = true;
                dispose();
                emit();
            }

            void emit() {
                if (once.compareAndSet(false, true)) {
                    parent.emit(index, value);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaCommonPlugins.onError(t);
                    return;
                }
                done = true;
                parent.onError(t);
            }

            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                emit();
            }
        }
    }
}
