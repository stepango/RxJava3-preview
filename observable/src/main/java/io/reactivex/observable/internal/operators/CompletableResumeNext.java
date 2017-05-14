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
import io.reactivex.common.internal.disposables.SequentialDisposable;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.CompletableSource;
import kotlin.jvm.functions.Function1;

public final class CompletableResumeNext extends Completable {

    final CompletableSource source;

    final Function1<? super Throwable, ? extends CompletableSource> errorMapper;

    public CompletableResumeNext(CompletableSource source,
                                 Function1<? super Throwable, ? extends CompletableSource> errorMapper) {
        this.source = source;
        this.errorMapper = errorMapper;
    }



    @Override
    protected void subscribeActual(final CompletableObserver s) {

        final SequentialDisposable sd = new SequentialDisposable();
        s.onSubscribe(sd);
        source.subscribe(new ResumeNext(s, sd));
    }

    final class ResumeNext implements CompletableObserver {

        final CompletableObserver s;
        final SequentialDisposable sd;

        ResumeNext(CompletableObserver s, SequentialDisposable sd) {
            this.s = s;
            this.sd = sd;
        }

        @Override
        public void onComplete() {
            s.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            CompletableSource c;

            try {
                c = errorMapper.invoke(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.onError(new CompositeException(ex, e));
                return;
            }

            if (c == null) {
                NullPointerException npe = new NullPointerException("The CompletableConsumable returned is null");
                npe.initCause(e);
                s.onError(npe);
                return;
            }

            c.subscribe(new OnErrorObserver());
        }

        @Override
        public void onSubscribe(Disposable d) {
            sd.update(d);
        }

        final class OnErrorObserver implements CompletableObserver {

            @Override
            public void onComplete() {
                s.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }

            @Override
            public void onSubscribe(Disposable d) {
                sd.update(d);
            }

        }
    }
}
