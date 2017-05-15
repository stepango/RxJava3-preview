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
import kotlin.jvm.functions.Function1;

/**
 * Filters each 'rail' of the source ParallelFlowable with a predicate function.
 *
 * @param <T> the input value type
 */
public final class ParallelFilterTry<T> extends ParallelFlowable<T> {

    final ParallelFlowable<T> source;

    final Function1<? super T, Boolean> predicate;

    final Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

    public ParallelFilterTry(ParallelFlowable<T> source, Function1<? super T, Boolean> predicate,
                             Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
        this.source = source;
        this.predicate = predicate;
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
                parents[i] = new ParallelFilterConditionalSubscriber<T>((ConditionalSubscriber<? super T>)a, predicate, errorHandler);
            } else {
                parents[i] = new ParallelFilterSubscriber<T>(a, predicate, errorHandler);
            }
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    abstract static class BaseFilterSubscriber<T> implements ConditionalSubscriber<T>, Subscription {
        final Function1<? super T, Boolean> predicate;

        final Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler;

        Subscription s;

        boolean done;

        BaseFilterSubscriber(Function1<? super T, Boolean> predicate, Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            this.predicate = predicate;
            this.errorHandler = errorHandler;
        }

        @Override
        public final void request(long n) {
            s.request(n);
        }

        @Override
        public final void cancel() {
            s.cancel();
        }

        @Override
        public final void onNext(T t) {
            if (!tryOnNext(t) && !done) {
                s.request(1);
            }
        }
    }

    static final class ParallelFilterSubscriber<T> extends BaseFilterSubscriber<T> {

        final Subscriber<? super T> actual;

        ParallelFilterSubscriber(Subscriber<? super T> actual, Function1<? super T, Boolean> predicate, Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            super(predicate, errorHandler);
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (!done) {
                long retries = 0L;

                for (;;) {
                    boolean b;

                    try {
                        b = predicate.invoke(t);
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

                    if (b) {
                        actual.onNext(t);
                        return true;
                    }
                    return false;
                }
            }
            return false;
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
            if (!done) {
                done = true;
                actual.onComplete();
            }
        }
    }

    static final class ParallelFilterConditionalSubscriber<T> extends BaseFilterSubscriber<T> {

        final ConditionalSubscriber<? super T> actual;

        ParallelFilterConditionalSubscriber(ConditionalSubscriber<? super T> actual,
                                            Function1<? super T, Boolean> predicate,
                                            Function2<? super Long, ? super Throwable, ParallelFailureHandling> errorHandler) {
            super(predicate, errorHandler);
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (!done) {
                long retries = 0L;

                for (;;) {
                    boolean b;

                    try {
                        b = predicate.invoke(t);
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

                    return b && actual.tryOnNext(t);
                }
            }
            return false;
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
            if (!done) {
                done = true;
                actual.onComplete();
            }
        }
    }}
