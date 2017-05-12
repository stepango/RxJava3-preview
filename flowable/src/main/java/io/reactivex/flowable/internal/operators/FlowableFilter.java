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

import hu.akarnokd.reactivestreams.extensions.ConditionalSubscriber;
import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import io.reactivex.common.annotations.Nullable;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscribers.BasicFuseableConditionalSubscriber;
import io.reactivex.flowable.internal.subscribers.BasicFuseableSubscriber;
import kotlin.jvm.functions.Function1;

public final class FlowableFilter<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function1<? super T, Boolean> predicate;

    public FlowableFilter(Flowable<T> source, Function1<? super T, Boolean> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new FilterConditionalSubscriber<T>(
                    (ConditionalSubscriber<? super T>)s, predicate));
        } else {
            source.subscribe(new FilterSubscriber<T>(s, predicate));
        }
    }

    static final class FilterSubscriber<T> extends BasicFuseableSubscriber<T, T>
    implements ConditionalSubscriber<T> {
        final Function1<? super T, Boolean> filter;

        FilterSubscriber(Subscriber<? super T> actual, Function1<? super T, Boolean> filter) {
            super(actual);
            this.filter = filter;
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
            if (sourceMode != NONE) {
                actual.onNext(null);
                return true;
            }
            boolean b;
            try {
                b = filter.invoke(t);
            } catch (Throwable e) {
                fail(e);
                return true;
            }
            if (b) {
                actual.onNext(t);
            }
            return b;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            FusedQueueSubscription<T> qs = this.qs;
            Function1<? super T, Boolean> f = filter;

            for (;;) {
                T t = qs.poll();
                if (t == null) {
                    return null;
                }

                if (f.invoke(t)) {
                    return t;
                }

                if (sourceMode == ASYNC) {
                    qs.request(1);
                }
            }
        }


    }

    static final class FilterConditionalSubscriber<T> extends BasicFuseableConditionalSubscriber<T, T> {
        final Function1<? super T, Boolean> filter;

        FilterConditionalSubscriber(ConditionalSubscriber<? super T> actual, Function1<? super T, Boolean> filter) {
            super(actual);
            this.filter = filter;
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

            if (sourceMode != NONE) {
                return actual.tryOnNext(null);
            }

            boolean b;
            try {
                b = filter.invoke(t);
            } catch (Throwable e) {
                fail(e);
                return true;
            }
            return b && actual.tryOnNext(t);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            FusedQueueSubscription<T> qs = this.qs;
            Function1<? super T, Boolean> f = filter;

            for (;;) {
                T t = qs.poll();
                if (t == null) {
                    return null;
                }

                if (f.invoke(t)) {
                    return t;
                }

                if (sourceMode == ASYNC) {
                    qs.request(1);
                }
            }
        }
    }
}
