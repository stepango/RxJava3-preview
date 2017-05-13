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

public final class LambdaSubscriber<T> extends AtomicReference<Subscription> implements RelaxedSubscriber<T>, Subscription, Disposable {

    private static final long serialVersionUID = -7251123623727029452L;
    final Function1<? super T, Unit> onNext;
    final Function1<? super Throwable, Unit> onError;
    final Function0 onComplete;
    final Function1<? super Subscription, kotlin.Unit> onSubscribe;

    public LambdaSubscriber(Function1<? super T, Unit> onNext, Function1<? super Throwable, Unit> onError,
                            Function0 onComplete,
                            Function1<? super Subscription, kotlin.Unit> onSubscribe) {
        super();
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onSubscribe = onSubscribe;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this, s)) {
            try {
                onSubscribe.invoke(this);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                onError(ex);
            }
        }
    }

    @Override
    public void onNext(T t) {
        if (!isDisposed()) {
            try {
                onNext.invoke(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                get().cancel();
                onError(e);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (get() != SubscriptionHelper.CANCELLED) {
            lazySet(SubscriptionHelper.CANCELLED);
            try {
                onError.invoke(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(new CompositeException(t, e));
            }
        } else {
            RxJavaCommonPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (get() != SubscriptionHelper.CANCELLED) {
            lazySet(SubscriptionHelper.CANCELLED);
            try {
                onComplete.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
        }
    }

    @Override
    public void dispose() {
        cancel();
    }

    @Override
    public boolean isDisposed() {
        return get() == SubscriptionHelper.CANCELLED;
    }

    @Override
    public void request(long n) {
        get().request(n);
    }

    @Override
    public void cancel() {
        SubscriptionHelper.cancel(this);
    }
}
