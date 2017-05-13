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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import io.reactivex.flowable.internal.utils.BackpressureHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class FlowableOnBackpressureDrop<T> extends AbstractFlowableWithUpstream<T, T> implements Function1<T, kotlin.Unit> {

    final Function1<? super T, Unit> onDrop;

    public FlowableOnBackpressureDrop(Flowable<T> source) {
        super(source);
        this.onDrop = this;
    }

    public FlowableOnBackpressureDrop(Flowable<T> source, Function1<? super T, Unit> onDrop) {
        super(source);
        this.onDrop = onDrop;
    }

    @Override
    public Unit invoke(T t) {
        // deliberately ignoring
        return Unit.INSTANCE;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        this.source.subscribe(new BackpressureDropSubscriber<T>(s, onDrop));
    }

    static final class BackpressureDropSubscriber<T>
    extends AtomicLong implements RelaxedSubscriber<T>, Subscription {

        private static final long serialVersionUID = -6246093802440953054L;

        final Subscriber<? super T> actual;
        final Function1<? super T, Unit> onDrop;

        Subscription s;

        boolean done;

        BackpressureDropSubscriber(Subscriber<? super T> actual, Function1<? super T, Unit> onDrop) {
            this.actual = actual;
            this.onDrop = onDrop;
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
            long r = get();
            if (r != 0L) {
                actual.onNext(t);
                BackpressureHelper.produced(this, 1);
            } else {
                try {
                    onDrop.invoke(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    onError(e);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaCommonPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(this, n);
            }
        }

        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
