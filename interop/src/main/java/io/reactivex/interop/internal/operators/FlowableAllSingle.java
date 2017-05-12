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

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.RxJavaFlowablePlugins;
import io.reactivex.flowable.extensions.FuseToFlowable;
import io.reactivex.flowable.internal.operators.FlowableAll;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import kotlin.jvm.functions.Function1;

public final class FlowableAllSingle<T> extends Single<Boolean> implements FuseToFlowable<Boolean> {

    final Flowable<T> source;

    final Function1<? super T, Boolean> predicate;

    public FlowableAllSingle(Flowable<T> source, Function1<? super T, Boolean> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super Boolean> s) {
        source.subscribe(new AllSubscriber<T>(s, predicate));
    }

    @Override
    public Flowable<Boolean> fuseToFlowable() {
        return RxJavaFlowablePlugins.onAssembly(new FlowableAll<T>(source, predicate));
    }

    static final class AllSubscriber<T> implements RelaxedSubscriber<T>, Disposable {

        final SingleObserver<? super Boolean> actual;

        final Function1<? super T, Boolean> predicate;

        Subscription s;

        boolean done;

        AllSubscriber(SingleObserver<? super Boolean> actual, Function1<? super T, Boolean> predicate) {
            this.actual = actual;
            this.predicate = predicate;
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
            boolean b;
            try {
                b = predicate.invoke(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.cancel();
                s = SubscriptionHelper.CANCELLED;
                onError(e);
                return;
            }
            if (!b) {
                done = true;
                s.cancel();
                s = SubscriptionHelper.CANCELLED;
                actual.onSuccess(false);
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

            actual.onSuccess(true);
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
