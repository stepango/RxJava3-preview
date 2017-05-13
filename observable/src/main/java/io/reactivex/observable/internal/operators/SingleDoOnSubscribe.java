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
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import kotlin.jvm.functions.Function1;

/**
 * Calls a callback when the upstream calls onSubscribe with a disposable.
 *
 * @param <T> the value type
 */
public final class SingleDoOnSubscribe<T> extends Single<T> {

    final SingleSource<T> source;

    final Function1<? super Disposable, kotlin.Unit> onSubscribe;

    public SingleDoOnSubscribe(SingleSource<T> source, Function1<? super Disposable, kotlin.Unit> onSubscribe) {
        this.source = source;
        this.onSubscribe = onSubscribe;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {
        source.subscribe(new DoOnSubscribeSingleObserver<T>(s, onSubscribe));
    }

    static final class DoOnSubscribeSingleObserver<T> implements SingleObserver<T> {

        final SingleObserver<? super T> actual;

        final Function1<? super Disposable, kotlin.Unit> onSubscribe;

        boolean done;

        DoOnSubscribeSingleObserver(SingleObserver<? super T> actual, Function1<? super Disposable, kotlin.Unit> onSubscribe) {
            this.actual = actual;
            this.onSubscribe = onSubscribe;
        }

        @Override
        public void onSubscribe(Disposable d) {
            try {
                onSubscribe.invoke(d);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                done = true;
                d.dispose();
                EmptyDisposable.error(ex, actual);
                return;
            }

            actual.onSubscribe(d);
        }

        @Override
        public void onSuccess(T value) {
            if (done) {
                return;
            }
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaCommonPlugins.onError(e);
                return;
            }
            actual.onError(e);
        }
    }

}
