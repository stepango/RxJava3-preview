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

public final class ObservableFilter<T> extends AbstractObservableWithUpstream<T, T> {
    final kotlin.jvm.functions.Function1<? super T, Boolean> predicate;

    public ObservableFilter(ObservableSource<T> source, kotlin.jvm.functions.Function1<? super T, Boolean> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        source.subscribe(new FilterObserver<T>(s, predicate));
    }

    static final class FilterObserver<T> extends BasicFuseableObserver<T, T> {
        final kotlin.jvm.functions.Function1<? super T, Boolean> filter;

        FilterObserver(Observer<? super T> actual, kotlin.jvm.functions.Function1<? super T, Boolean> filter) {
            super(actual);
            this.filter = filter;
        }

        @Override
        public void onNext(T t) {
            if (sourceMode == NONE) {
                boolean b;
                try {
                    b = filter.invoke(t);
                } catch (Throwable e) {
                    fail(e);
                    return;
                }
                if (b) {
                    actual.onNext(t);
                }
            } else {
                actual.onNext(null);
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
                if (v == null || filter.invoke(v)) {
                    return v;
                }
            }
        }
    }
}
