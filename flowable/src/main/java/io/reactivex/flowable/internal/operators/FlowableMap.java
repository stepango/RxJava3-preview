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
import io.reactivex.common.annotations.Nullable;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscribers.BasicFuseableConditionalSubscriber;
import io.reactivex.flowable.internal.subscribers.BasicFuseableSubscriber;
import kotlin.jvm.functions.Function1;

public final class FlowableMap<T, U> extends AbstractFlowableWithUpstream<T, U> {
    final Function1<? super T, ? extends U> mapper;

    public FlowableMap(Flowable<T> source, Function1<? super T, ? extends U> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new MapConditionalSubscriber<T, U>((ConditionalSubscriber<? super U>)s, mapper));
        } else {
            source.subscribe(new MapSubscriber<T, U>(s, mapper));
        }
    }

    static final class MapSubscriber<T, U> extends BasicFuseableSubscriber<T, U> {
        final Function1<? super T, ? extends U> mapper;

        MapSubscriber(Subscriber<? super U> actual, Function1<? super T, ? extends U> mapper) {
            super(actual);
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                actual.onNext(null);
                return;
            }

            U v;

            try {
                v = ObjectHelper.requireNonNull(mapper.invoke(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            actual.onNext(v);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public U poll() throws Throwable {
            T t = qs.poll();
            return t != null ? ObjectHelper.<U>requireNonNull(mapper.invoke(t), "The mapper function returned a null value.") : null;
        }
    }

    static final class MapConditionalSubscriber<T, U> extends BasicFuseableConditionalSubscriber<T, U> {
        final Function1<? super T, ? extends U> mapper;

        MapConditionalSubscriber(ConditionalSubscriber<? super U> actual, Function1<? super T, ? extends U> function) {
            super(actual);
            this.mapper = function;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                actual.onNext(null);
                return;
            }

            U v;

            try {
                v = ObjectHelper.requireNonNull(mapper.invoke(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            actual.onNext(v);
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }

            U v;

            try {
                v = ObjectHelper.requireNonNull(mapper.invoke(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return true;
            }
            return actual.tryOnNext(v);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public U poll() throws Throwable {
            T t = qs.poll();
            return t != null ? ObjectHelper.<U>requireNonNull(mapper.invoke(t), "The mapper function returned a null value.") : null;
        }
    }

}
