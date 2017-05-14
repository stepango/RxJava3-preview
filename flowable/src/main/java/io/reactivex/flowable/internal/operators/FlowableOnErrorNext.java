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

package io.reactivex.flowable.internal.operators;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.SubscriptionArbiter;
import kotlin.jvm.functions.Function1;

public final class FlowableOnErrorNext<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function1<? super Throwable, ? extends Publisher<? extends T>> nextSupplier;
    final boolean allowFatal;

    public FlowableOnErrorNext(Flowable<T> source,
                               Function1<? super Throwable, ? extends Publisher<? extends T>> nextSupplier, boolean allowFatal) {
        super(source);
        this.nextSupplier = nextSupplier;
        this.allowFatal = allowFatal;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        OnErrorNextSubscriber<T> parent = new OnErrorNextSubscriber<T>(s, nextSupplier, allowFatal);
        s.onSubscribe(parent.arbiter);
        source.subscribe(parent);
    }

    static final class OnErrorNextSubscriber<T> implements RelaxedSubscriber<T> {
        final Subscriber<? super T> actual;
        final Function1<? super Throwable, ? extends Publisher<? extends T>> nextSupplier;
        final boolean allowFatal;
        final SubscriptionArbiter arbiter;

        boolean once;

        boolean done;

        OnErrorNextSubscriber(Subscriber<? super T> actual, Function1<? super Throwable, ? extends Publisher<? extends T>> nextSupplier, boolean allowFatal) {
            this.actual = actual;
            this.nextSupplier = nextSupplier;
            this.allowFatal = allowFatal;
            this.arbiter = new SubscriptionArbiter();
        }

        @Override
        public void onSubscribe(Subscription s) {
            arbiter.setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            actual.onNext(t);
            if (!once) {
                arbiter.produced(1L);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (once) {
                if (done) {
                    RxJavaCommonPlugins.onError(t);
                    return;
                }
                actual.onError(t);
                return;
            }
            once = true;

            if (allowFatal && !(t instanceof Exception)) {
                actual.onError(t);
                return;
            }

            Publisher<? extends T> p;

            try {
                p = nextSupplier.invoke(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(t, e));
                return;
            }

            if (p == null) {
                NullPointerException npe = new NullPointerException("Publisher is null");
                npe.initCause(t);
                actual.onError(npe);
                return;
            }

            p.subscribe(this);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            once = true;
            actual.onComplete();
        }
    }
}
