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

import io.reactivex.common.annotations.Nullable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.internal.observers.BasicFuseableObserver;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public final class ObservableDistinctUntilChanged<T, K> extends AbstractObservableWithUpstream<T, T> {

    final Function1<? super T, K> keySelector;

    final Function2<? super K, ? super K, Boolean> comparer;

    public ObservableDistinctUntilChanged(ObservableSource<T> source, Function1<? super T, K> keySelector, Function2<? super K, ? super K, Boolean> comparer) {
        super(source);
        this.keySelector = keySelector;
        this.comparer = comparer;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        source.subscribe(new DistinctUntilChangedObserver<T, K>(s, keySelector, comparer));
    }

    static final class DistinctUntilChangedObserver<T, K> extends BasicFuseableObserver<T, T> {

        final Function1<? super T, K> keySelector;

        final Function2<? super K, ? super K, Boolean> comparer;

        K last;

        boolean hasValue;

        DistinctUntilChangedObserver(Observer<? super T> actual,
                                     Function1<? super T, K> keySelector,
                                     Function2<? super K, ? super K, Boolean> comparer) {
            super(actual);
            this.keySelector = keySelector;
            this.comparer = comparer;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (sourceMode != NONE) {
                actual.onNext(t);
                return;
            }

            K key;

            try {
                key = keySelector.invoke(t);
                if (hasValue) {
                    boolean equal = comparer.invoke(last, key);
                    last = key;
                    if (equal) {
                        return;
                    }
                } else {
                    hasValue = true;
                    last = key;
                }
            } catch (Throwable ex) {
               fail(ex);
               return;
            }

            actual.onNext(t);
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
                if (v == null) {
                    return null;
                }
                K key = keySelector.invoke(v);
                if (!hasValue) {
                    hasValue = true;
                    last = key;
                    return v;
                }

                if (!comparer.invoke(last, key)) {
                    last = key;
                    return v;
                }
                last = key;
            }
        }

    }
}
