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

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.flowable.ParallelFlowable;
import io.reactivex.flowable.internal.subscriptions.EmptySubscription;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/**
 * Execute a Consumer in each 'rail' for the current element passing through.
 *
 * @param <T> the value type
 */
public final class ParallelPeek<T> extends ParallelFlowable<T> {

    final ParallelFlowable<T> source;

    final Function1<? super T, Unit> onNext;
    final Function1<? super T, Unit> onAfterNext;
    final Function1<? super Throwable, Unit> onError;
    final Function0 onComplete;
    final Function0 onAfterTerminated;
    final Function1<? super Subscription, kotlin.Unit> onSubscribe;
    final Function1<Long, Unit> onRequest;
    final Function0 onCancel;

    public ParallelPeek(ParallelFlowable<T> source,
                        Function1<? super T, Unit> onNext,
                        Function1<? super T, Unit> onAfterNext,
                        Function1<? super Throwable, Unit> onError,
                        Function0 onComplete,
                        Function0 onAfterTerminated,
                        Function1<? super Subscription, kotlin.Unit> onSubscribe,
                        Function1<Long, Unit> onRequest,
                        Function0 onCancel
    ) {
        this.source = source;

        this.onNext = ObjectHelper.requireNonNull(onNext, "onNext is null");
        this.onAfterNext = ObjectHelper.requireNonNull(onAfterNext, "onAfterNext is null");
        this.onError = ObjectHelper.requireNonNull(onError, "onError is null");
        this.onComplete = ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        this.onAfterTerminated = ObjectHelper.requireNonNull(onAfterTerminated, "onAfterTerminated is null");
        this.onSubscribe = ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        this.onRequest = ObjectHelper.requireNonNull(onRequest, "onRequest is null");
        this.onCancel = ObjectHelper.requireNonNull(onCancel, "onCancel is null");
    }

    @Override
    public void subscribe(Subscriber<? super T>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;
        @SuppressWarnings("unchecked")
        Subscriber<? super T>[] parents = new Subscriber[n];

        for (int i = 0; i < n; i++) {
            parents[i] = new ParallelPeekSubscriber<T>(subscribers[i], this);
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class ParallelPeekSubscriber<T> implements RelaxedSubscriber<T>, Subscription {

        final Subscriber<? super T> actual;

        final ParallelPeek<T> parent;

        Subscription s;

        boolean done;

        ParallelPeekSubscriber(Subscriber<? super T> actual, ParallelPeek<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            try {
                parent.onRequest.invoke(n);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaCommonPlugins.onError(ex);
            }
            s.request(n);
        }

        @Override
        public void cancel() {
            try {
                parent.onCancel.invoke();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaCommonPlugins.onError(ex);
            }
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                try {
                    parent.onSubscribe.invoke(s);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    s.cancel();
                    actual.onSubscribe(EmptySubscription.INSTANCE);
                    onError(ex);
                    return;
                }

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                try {
                    parent.onNext.invoke(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    onError(ex);
                    return;
                }

                actual.onNext(t);

                try {
                    parent.onAfterNext.invoke(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    onError(ex);
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

            try {
                parent.onError.invoke(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                t = new CompositeException(t, ex);
            }
            actual.onError(t);

            try {
                parent.onAfterTerminated.invoke();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaCommonPlugins.onError(ex);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                try {
                    parent.onComplete.invoke();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    actual.onError(ex);
                    return;
                }
                actual.onComplete();

                try {
                    parent.onAfterTerminated.invoke();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaCommonPlugins.onError(ex);
                }
            }
        }
    }
}
