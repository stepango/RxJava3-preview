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
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.flowable.ParallelFailureHandling;
import io.reactivex.flowable.ParallelFlowable;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Calls a Consumer for each upstream value passing by
 * and handles any failure with a handler function.
 *
 * @param <T> the input value type
 * @since 2.0.8 - experimental
 */
public final class ParallelDoOnNextTry<T> extends ParallelFlowable<T> {

    final ParallelFlowable<T> source;

    final Function1<? super T, Unit> onNext;

    final Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

    public ParallelDoOnNextTry(ParallelFlowable<T> source, Function1<? super T, Unit> onNext,
                               Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        this.source = source;
        this.onNext = onNext;
        this.errorHandler = errorHandler;
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
            Subscriber<? super T> a = subscribers[i];
            if (a instanceof ConditionalSubscriber) {
                parents[i] = new ParallelDoOnNextConditionalSubscriber<T>((ConditionalSubscriber<? super T>)a, onNext, errorHandler);
            } else {
                parents[i] = new ParallelDoOnNextSubscriber<T>(a, onNext, errorHandler);
            }
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class ParallelDoOnNextSubscriber<T> implements ConditionalSubscriber<T>, Subscription {

        final Subscriber<? super T> actual;

        final Function1<? super T, Unit> onNext;

        final Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

        Subscription s;

        boolean done;

        ParallelDoOnNextSubscriber(Subscriber<? super T> actual, Function1<? super T, Unit> onNext,
                                   Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.actual = actual;
            this.onNext = onNext;
            this.errorHandler = errorHandler;
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            long retries = 0;

            for (;;) {
                try {
                    onNext.invoke(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);

                    ParallelFailureHandling h;

                    try {
                        h = ObjectHelper.requireNonNull(errorHandler.invoke(++retries, ex), "The errorHandler returned a null item");
                    } catch (Throwable exc) {
                        Exceptions.throwIfFatal(exc);
                        cancel();
                        onError(new CompositeException(ex, exc));
                        return false;
                    }

                    switch (h) {
                    case RETRY:
                        continue;
                    case SKIP:
                        return false;
                    case STOP:
                        cancel();
                        onComplete();
                        return false;
                    default:
                        cancel();
                        onError(ex);
                        return false;
                    }
                }

                actual.onNext(t);
                return true;
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

    }
    static final class ParallelDoOnNextConditionalSubscriber<T> implements ConditionalSubscriber<T>, Subscription {

        final ConditionalSubscriber<? super T> actual;

        final Function1<? super T, Unit> onNext;

        final Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;
        Subscription s;

        boolean done;

        ParallelDoOnNextConditionalSubscriber(ConditionalSubscriber<? super T> actual,
                                              Function1<? super T, Unit> onNext,
                                              Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.actual = actual;
            this.onNext = onNext;
            this.errorHandler = errorHandler;
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t) && !done) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            long retries = 0;

            for (;;) {
                try {
                    onNext.invoke(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);

                    ParallelFailureHandling h;

                    try {
                        h = ObjectHelper.requireNonNull(errorHandler.invoke(++retries, ex), "The errorHandler returned a null item");
                    } catch (Throwable exc) {
                        Exceptions.throwIfFatal(exc);
                        cancel();
                        onError(new CompositeException(ex, exc));
                        return false;
                    }

                    switch (h) {
                    case RETRY:
                        continue;
                    case SKIP:
                        return false;
                    case STOP:
                        cancel();
                        onComplete();
                        return false;
                    default:
                        cancel();
                        onError(ex);
                        return false;
                    }
                }

                return actual.tryOnNext(t);
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

    }
}
