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

import hu.akarnokd.reactivestreams.extensions.ConditionalSubscriber;
import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.annotations.Experimental;
import io.reactivex.common.annotations.Nullable;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.functions.Action;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.BasicIntFusedQueueSubscription;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;

/**
 * Execute an action after an onError, onComplete or a cancel event.
 *
 * @param <T> the value type
 * @since 2.0.1 - experimental
 */
@Experimental
public final class FlowableDoFinally<T> extends AbstractFlowableWithUpstream<T, T> {

    final Action onFinally;

    public FlowableDoFinally(Flowable<T> source, Action onFinally) {
        super(source);
        this.onFinally = onFinally;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new DoFinallyConditionalSubscriber<T>((ConditionalSubscriber<? super T>)s, onFinally));
        } else {
            source.subscribe(new DoFinallySubscriber<T>(s, onFinally));
        }
    }

    static final class DoFinallySubscriber<T> extends BasicIntFusedQueueSubscription<T> implements RelaxedSubscriber<T> {

        private static final long serialVersionUID = 4109457741734051389L;

        final Subscriber<? super T> actual;

        final Action onFinally;

        Subscription s;

        FusedQueueSubscription<T> qs;

        boolean syncFused;

        DoFinallySubscriber(Subscriber<? super T> actual, Action onFinally) {
            this.actual = actual;
            this.onFinally = onFinally;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                if (s instanceof FusedQueueSubscription) {
                    this.qs = (FusedQueueSubscription<T>)s;
                }

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
            runFinally();
        }

        @Override
        public void onComplete() {
            actual.onComplete();
            runFinally();
        }

        @Override
        public void cancel() {
            s.cancel();
            runFinally();
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public int requestFusion(int mode) {
            FusedQueueSubscription<T> qs = this.qs;
            if (qs != null && (mode & BOUNDARY) == 0) {
                int m = qs.requestFusion(mode);
                if (m != NONE) {
                    syncFused = m == SYNC;
                }
                return m;
            }
            return NONE;
        }

        @Override
        public void clear() {
            qs.clear();
        }

        @Override
        public boolean isEmpty() {
            return qs.isEmpty();
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            T v = qs.poll();
            if (v == null && syncFused) {
                runFinally();
            }
            return v;
        }

        void runFinally() {
            if (compareAndSet(0, 1)) {
                try {
                    onFinally.invoke();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaCommonPlugins.onError(ex);
                }
            }
        }
    }

    static final class DoFinallyConditionalSubscriber<T> extends BasicIntFusedQueueSubscription<T> implements ConditionalSubscriber<T> {

        private static final long serialVersionUID = 4109457741734051389L;

        final ConditionalSubscriber<? super T> actual;

        final Action onFinally;

        Subscription s;

        FusedQueueSubscription<T> qs;

        boolean syncFused;

        DoFinallyConditionalSubscriber(ConditionalSubscriber<? super T> actual, Action onFinally) {
            this.actual = actual;
            this.onFinally = onFinally;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                if (s instanceof FusedQueueSubscription) {
                    this.qs = (FusedQueueSubscription<T>)s;
                }

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public boolean tryOnNext(T t) {
            return actual.tryOnNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
            runFinally();
        }

        @Override
        public void onComplete() {
            actual.onComplete();
            runFinally();
        }

        @Override
        public void cancel() {
            s.cancel();
            runFinally();
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public int requestFusion(int mode) {
            FusedQueueSubscription<T> qs = this.qs;
            if (qs != null && (mode & BOUNDARY) == 0) {
                int m = qs.requestFusion(mode);
                if (m != NONE) {
                    syncFused = m == SYNC;
                }
                return m;
            }
            return NONE;
        }

        @Override
        public void clear() {
            qs.clear();
        }

        @Override
        public boolean isEmpty() {
            return qs.isEmpty();
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            T v = qs.poll();
            if (v == null && syncFused) {
                runFinally();
            }
            return v;
        }

        void runFinally() {
            if (compareAndSet(0, 1)) {
                try {
                    onFinally.invoke();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaCommonPlugins.onError(ex);
                }
            }
        }
    }
}
