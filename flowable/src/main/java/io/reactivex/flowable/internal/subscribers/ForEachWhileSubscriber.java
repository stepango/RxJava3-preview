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

package io.reactivex.flowable.internal.subscribers;

import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public final class ForEachWhileSubscriber<T>
extends AtomicReference<Subscription>
implements RelaxedSubscriber<T>, Disposable {


    private static final long serialVersionUID = -4403180040475402120L;

    final Function1<? super T, Boolean> onNext;

    final Function1<? super Throwable, Unit> onError;

    final Function0 onComplete;

    boolean done;

    public ForEachWhileSubscriber(Function1<? super T, Boolean> onNext,
                                  Function1<? super Throwable, Unit> onError, Function0 onComplete) {
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this, s)) {
            s.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }

        boolean b;
        try {
            b = onNext.invoke(t);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            dispose();
            onError(ex);
            return;
        }

        if (!b) {
            dispose();
            onComplete();
        }
    }

    @Override
    public void onError(Throwable t) {
        if (done) {
            RxJavaCommonPlugins.onError(t);
            return;
        }
        done = true;
        try {
            onError.invoke(t);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaCommonPlugins.onError(new CompositeException(t, ex));
        }
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;
        try {
            onComplete.invoke();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaCommonPlugins.onError(ex);
        }
    }

    @Override
    public void dispose() {
        SubscriptionHelper.cancel(this);
    }

    @Override
    public boolean isDisposed() {
        return SubscriptionHelper.isCancelled(this.get());
    }
}
