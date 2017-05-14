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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import hu.akarnokd.reactivestreams.extensions.FusedQueue;
import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.exceptions.MissingBackpressureException;
import io.reactivex.common.internal.utils.AtomicThrowable;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.queues.SpscArrayQueue;
import io.reactivex.flowable.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import kotlin.jvm.functions.Function2;

public final class FlowableSequenceEqual<T> extends Flowable<Boolean> {
    final Publisher<? extends T> first;
    final Publisher<? extends T> second;
    final Function2<? super T, ? super T, Boolean> comparer;
    final int prefetch;

    public FlowableSequenceEqual(Publisher<? extends T> first, Publisher<? extends T> second,
                                 Function2<? super T, ? super T, Boolean> comparer, int prefetch) {
        this.first = first;
        this.second = second;
        this.comparer = comparer;
        this.prefetch = prefetch;
    }

    @Override
    public void subscribeActual(Subscriber<? super Boolean> s) {
        EqualCoordinator<T> parent = new EqualCoordinator<T>(s, prefetch, comparer);
        s.onSubscribe(parent);
        parent.subscribe(first, second);
    }

    /**
     * Provides callbacks for the EqualSubscribers.
     */
    public interface EqualCoordinatorHelper {

        void drain();

        void innerError(Throwable ex);
    }

    public static final class EqualCoordinator<T> extends DeferredScalarSubscription<Boolean>
    implements EqualCoordinatorHelper {

        private static final long serialVersionUID = -6178010334400373240L;

        final Function2<? super T, ? super T, Boolean> comparer;

        final EqualSubscriber<T> first;

        final EqualSubscriber<T> second;

        final AtomicThrowable error;

        final AtomicInteger wip;

        T v1;

        T v2;

        public EqualCoordinator(Subscriber<? super Boolean> actual, int prefetch, Function2<? super T, ? super T, Boolean> comparer) {
            super(actual);
            this.comparer = comparer;
            this.wip = new AtomicInteger();
            this.first = new EqualSubscriber<T>(this, prefetch);
            this.second = new EqualSubscriber<T>(this, prefetch);
            this.error = new AtomicThrowable();
        }

        void subscribe(Publisher<? extends T> source1, Publisher<? extends T> source2) {
            source1.subscribe(first);
            source2.subscribe(second);
        }

        @Override
        public void cancel() {
            super.cancel();
            first.cancel();
            second.cancel();
            if (wip.getAndIncrement() == 0) {
                first.clear();
                second.clear();
            }
        }

        void cancelAndClear() {
            first.cancel();
            first.clear();
            second.cancel();
            second.clear();
        }

        @Override
        public void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            for (;;) {
                FusedQueue<T> q1 = first.queue;
                FusedQueue<T> q2 = second.queue;

                if (q1 != null && q2 != null) {
                    for (;;) {
                        if (isCancelled()) {
                            first.clear();
                            second.clear();
                            return;
                        }

                        Throwable ex = error.get();
                        if (ex != null) {
                            cancelAndClear();

                            actual.onError(error.terminate());
                            return;
                        }

                        boolean d1 = first.done;

                        T a = v1;
                        if (a == null) {
                            try {
                                a = q1.poll();
                            } catch (Throwable exc) {
                                Exceptions.throwIfFatal(exc);
                                cancelAndClear();
                                error.addThrowable(exc);
                                actual.onError(error.terminate());
                                return;
                            }
                            v1 = a;
                        }
                        boolean e1 = a == null;

                        boolean d2 = second.done;
                        T b = v2;
                        if (b == null) {
                            try {
                                b = q2.poll();
                            } catch (Throwable exc) {
                                Exceptions.throwIfFatal(exc);
                                cancelAndClear();
                                error.addThrowable(exc);
                                actual.onError(error.terminate());
                                return;
                            }
                            v2 = b;
                        }

                        boolean e2 = b == null;

                        if (d1 && d2 && e1 && e2) {
                            complete(true);
                            return;
                        }
                        if ((d1 && d2) && (e1 != e2)) {
                            cancelAndClear();
                            complete(false);
                            return;
                        }

                        if (e1 || e2) {
                            break;
                        }

                        boolean c;

                        try {
                            c = comparer.invoke(a, b);
                        } catch (Throwable exc) {
                            Exceptions.throwIfFatal(exc);
                            cancelAndClear();
                            error.addThrowable(exc);
                            actual.onError(error.terminate());
                            return;
                        }

                        if (!c) {
                            cancelAndClear();
                            complete(false);
                            return;
                        }

                        v1 = null;
                        v2 = null;

                        first.request();
                        second.request();
                    }

                } else {
                    if (isCancelled()) {
                        first.clear();
                        second.clear();
                        return;
                    }

                    Throwable ex = error.get();
                    if (ex != null) {
                        cancelAndClear();

                        actual.onError(error.terminate());
                        return;
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void innerError(Throwable t) {
            if (error.addThrowable(t)) {
                drain();
            } else {
                RxJavaCommonPlugins.onError(t);
            }
        }
    }

    public static final class EqualSubscriber<T>
    extends AtomicReference<Subscription>
    implements RelaxedSubscriber<T> {

        private static final long serialVersionUID = 4804128302091633067L;

        final EqualCoordinatorHelper parent;

        final int prefetch;

        final int limit;

        long produced;

        public volatile FusedQueue<T> queue;

        public volatile boolean done;

        int sourceMode;

        public EqualSubscriber(EqualCoordinatorHelper parent, int prefetch) {
            this.parent = parent;
            this.limit = prefetch - (prefetch >> 2);
            this.prefetch = prefetch;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                if (s instanceof FusedQueueSubscription) {
                    @SuppressWarnings("unchecked")
                    FusedQueueSubscription<T> qs = (FusedQueueSubscription<T>) s;

                    int m = qs.requestFusion(FusedQueueSubscription.ANY);
                    if (m == FusedQueueSubscription.SYNC) {
                        sourceMode = m;
                        queue = qs;
                        done = true;
                        parent.drain();
                        return;
                    }
                    if (m == FusedQueueSubscription.ASYNC) {
                        sourceMode = m;
                        queue = qs;
                        s.request(prefetch);
                        return;
                    }
                }

                queue = new SpscArrayQueue<T>(prefetch);

                s.request(prefetch);
            }
        }

        @Override
        public void onNext(T t) {
            if (sourceMode == FusedQueueSubscription.NONE) {
                if (!queue.offer(t)) {
                    onError(new MissingBackpressureException());
                    return;
                }
            }
            parent.drain();
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(t);
        }

        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }

        public void request() {
            if (sourceMode != FusedQueueSubscription.SYNC) {
                long p = produced + 1;
                if (p >= limit) {
                    produced = 0;
                    get().request(p);
                } else {
                    produced = p;
                }
            }
        }

        public void cancel() {
            SubscriptionHelper.cancel(this);
        }

        public void clear() {
            FusedQueue<T> sq = queue;
            if (sq != null) {
                sq.clear();
            }
        }
    }
}
