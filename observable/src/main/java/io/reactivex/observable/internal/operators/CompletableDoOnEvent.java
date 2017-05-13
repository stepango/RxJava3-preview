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
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.CompletableSource;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class CompletableDoOnEvent extends Completable {
    final CompletableSource source;
    final Function1<? super Throwable, Unit> onEvent;

    public CompletableDoOnEvent(final CompletableSource source, final Function1<? super Throwable, Unit> onEvent) {
        this.source = source;
        this.onEvent = onEvent;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {
        source.subscribe(new DoOnEvent(s));
    }

    final class DoOnEvent implements CompletableObserver {
        private final CompletableObserver observer;

        DoOnEvent(CompletableObserver observer) {
            this.observer = observer;
        }

        @Override
        public void onComplete() {
            try {
                onEvent.invoke(null);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                observer.onError(e);
                return;
            }

            observer.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            try {
                onEvent.invoke(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }

            observer.onError(e);
        }

        @Override
        public void onSubscribe(final Disposable d) {
            observer.onSubscribe(d);
        }
    }
}
