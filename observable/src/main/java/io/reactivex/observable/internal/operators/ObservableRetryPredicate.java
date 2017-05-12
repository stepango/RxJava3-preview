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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposable;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.disposables.SequentialDisposable;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;

public final class ObservableRetryPredicate<T> extends AbstractObservableWithUpstream<T, T> {
    final kotlin.jvm.functions.Function1<? super Throwable, Boolean> predicate;
    final long count;
    public ObservableRetryPredicate(Observable<T> source,
                                    long count,
                                    kotlin.jvm.functions.Function1<? super Throwable, Boolean> predicate) {
        super(source);
        this.predicate = predicate;
        this.count = count;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        SequentialDisposable sa = new SequentialDisposable();
        s.onSubscribe(sa);

        RepeatObserver<T> rs = new RepeatObserver<T>(s, count, predicate, sa, source);
        rs.subscribeNext();
    }

    static final class RepeatObserver<T> extends AtomicInteger implements Observer<T> {

        private static final long serialVersionUID = -7098360935104053232L;

        final Observer<? super T> actual;
        final SequentialDisposable sa;
        final ObservableSource<? extends T> source;
        final kotlin.jvm.functions.Function1<? super Throwable, Boolean> predicate;
        long remaining;
        RepeatObserver(Observer<? super T> actual, long count,
                       kotlin.jvm.functions.Function1<? super Throwable, Boolean> predicate, SequentialDisposable sa, ObservableSource<? extends T> source) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.predicate = predicate;
            this.remaining = count;
        }

        @Override
        public void onSubscribe(Disposable s) {
            sa.update(s);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        @Override
        public void onError(Throwable t) {
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                remaining = r - 1;
            }
            if (r == 0) {
                actual.onError(t);
            } else {
                boolean b;
                try {
                    b = predicate.invoke(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    actual.onError(new CompositeException(t, e));
                    return;
                }
                if (!b) {
                    actual.onError(t);
                    return;
                }
                subscribeNext();
            }
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    if (sa.isDisposed()) {
                        return;
                    }
                    source.subscribe(this);

                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }
}
