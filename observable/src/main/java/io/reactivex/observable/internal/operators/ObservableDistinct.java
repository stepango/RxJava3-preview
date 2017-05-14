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

import java.util.Collection;
import java.util.concurrent.Callable;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.annotations.Nullable;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import io.reactivex.observable.internal.observers.BasicFuseableObserver;
import kotlin.jvm.functions.Function1;

public final class ObservableDistinct<T, K> extends AbstractObservableWithUpstream<T, T> {

    final Function1<? super T, K> keySelector;

    final Callable<? extends Collection<? super K>> collectionSupplier;

    public ObservableDistinct(ObservableSource<T> source, Function1<? super T, K> keySelector, Callable<? extends Collection<? super K>> collectionSupplier) {
        super(source);
        this.keySelector = keySelector;
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        Collection<? super K> collection;

        try {
            collection = ObjectHelper.requireNonNull(collectionSupplier.call(), "The collectionSupplier returned a null collection. Null values are generally not allowed in 2.x operators and sources.");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        source.subscribe(new DistinctObserver<T, K>(observer, keySelector, collection));
    }

    static final class DistinctObserver<T, K> extends BasicFuseableObserver<T, T> {

        final Collection<? super K> collection;

        final Function1<? super T, K> keySelector;

        DistinctObserver(Observer<? super T> actual, Function1<? super T, K> keySelector, Collection<? super K> collection) {
            super(actual);
            this.keySelector = keySelector;
            this.collection = collection;
        }

        @Override
        public void onNext(T value) {
            if (done) {
                return;
            }
            if (sourceMode == NONE) {
                K key;
                boolean b;

                try {
                    key = ObjectHelper.requireNonNull(keySelector.invoke(value), "The keySelector returned a null key");
                    b = collection.add(key);
                } catch (Throwable ex) {
                    fail(ex);
                    return;
                }

                if (b) {
                    actual.onNext(value);
                }
            } else {
                actual.onNext(null);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaCommonPlugins.onError(e);
            } else {
                done = true;
                collection.clear();
                actual.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                collection.clear();
                actual.onComplete();
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            for (;;) {
                T v = qs.poll();

                if (v == null || collection.add(ObjectHelper.requireNonNull(keySelector.invoke(v), "The keySelector returned a null key"))) {
                    return v;
                }
            }
        }

        @Override
        public void clear() {
            collection.clear();
            super.clear();
        }
    }
}
