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

import io.reactivex.common.annotations.Experimental;
import io.reactivex.common.annotations.Nullable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.internal.observers.BasicFuseableObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Calls a consumer after pushing the current item to the downstream.
 * @param <T> the value type
 * @since 2.0.1 - experimental
 */
@Experimental
public final class ObservableDoAfterNext<T> extends AbstractObservableWithUpstream<T, T> {

    final Function1<? super T, Unit> onAfterNext;

    public ObservableDoAfterNext(ObservableSource<T> source, Function1<? super T, Unit> onAfterNext) {
        super(source);
        this.onAfterNext = onAfterNext;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        source.subscribe(new DoAfterObserver<T>(s, onAfterNext));
    }

    static final class DoAfterObserver<T> extends BasicFuseableObserver<T, T> {

        final Function1<? super T, Unit> onAfterNext;

        DoAfterObserver(Observer<? super T> actual, Function1<? super T, Unit> onAfterNext) {
            super(actual);
            this.onAfterNext = onAfterNext;
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);

            if (sourceMode == NONE) {
                try {
                    onAfterNext.invoke(t);
                } catch (Throwable ex) {
                    fail(ex);
                }
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            T v = qs.poll();
            if (v != null) {
                onAfterNext.invoke(v);
            }
            return v;
        }
    }
}
