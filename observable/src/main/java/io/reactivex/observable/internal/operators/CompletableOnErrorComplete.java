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
import kotlin.jvm.functions.Function1;

public final class CompletableOnErrorComplete extends Completable {

    final CompletableSource source;

    final Function1<? super Throwable, Boolean> predicate;

    public CompletableOnErrorComplete(CompletableSource source, Function1<? super Throwable, Boolean> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {

        source.subscribe(new OnError(s));
    }

    final class OnError implements CompletableObserver {

        private final CompletableObserver s;

        OnError(CompletableObserver s) {
            this.s = s;
        }

        @Override
        public void onComplete() {
            s.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            boolean b;

            try {
                b = predicate.invoke(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.onError(new CompositeException(e, ex));
                return;
            }

            if (b) {
                s.onComplete();
            } else {
                s.onError(e);
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            s.onSubscribe(d);
        }

    }
}
