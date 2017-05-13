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
package io.reactivex.interop.internal.operators;

import org.reactivestreams.Subscription;

import java.util.concurrent.Callable;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.RxJavaFlowablePlugins;
import io.reactivex.flowable.extensions.FuseToFlowable;
import io.reactivex.flowable.internal.operators.FlowableCollect;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.internal.disposables.EmptyDisposable;
import kotlin.jvm.functions.Function2;

public final class FlowableCollectSingle<T, U> extends Single<U> implements FuseToFlowable<U> {

    final Flowable<T> source;

    final Callable<? extends U> initialSupplier;
    final Function2<? super U, ? super T, kotlin.Unit> collector;

    public FlowableCollectSingle(Flowable<T> source, Callable<? extends U> initialSupplier, Function2<? super U, ? super T, kotlin.Unit> collector) {
        this.source = source;
        this.initialSupplier = initialSupplier;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super U> s) {
        U u;
        try {
            u = ObjectHelper.requireNonNull(initialSupplier.call(), "The initialSupplier returned a null value");
        } catch (Throwable e) {
            EmptyDisposable.error(e, s);
            return;
        }

        source.subscribe(new CollectSubscriber<T, U>(s, u, collector));
    }

    @Override
    public Flowable<U> fuseToFlowable() {
        return RxJavaFlowablePlugins.onAssembly(new FlowableCollect<T, U>(source, initialSupplier, collector));
    }

    static final class CollectSubscriber<T, U> implements RelaxedSubscriber<T>, Disposable {

        final SingleObserver<? super U> actual;

        final Function2<? super U, ? super T, kotlin.Unit> collector;

        final U u;

        Subscription s;

        boolean done;

        CollectSubscriber(SingleObserver<? super U> actual, U u, Function2<? super U, ? super T, kotlin.Unit> collector) {
            this.actual = actual;
            this.collector = collector;
            this.u = u;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            try {
                collector.invoke(u, t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.cancel();
                onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaCommonPlugins.onError(t);
                return;
            }
            done = true;
            s = SubscriptionHelper.CANCELLED;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            s = SubscriptionHelper.CANCELLED;
            actual.onSuccess(u);
        }

        @Override
        public void dispose() {
            s.cancel();
            s = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return s == SubscriptionHelper.CANCELLED;
        }
    }
}
