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
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.annotations.Nullable;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.common.functions.Consumer;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscribers.BasicFuseableConditionalSubscriber;
import io.reactivex.flowable.internal.subscribers.BasicFuseableSubscriber;
import kotlin.jvm.functions.Function0;

public final class FlowableDoOnEach<T> extends AbstractFlowableWithUpstream<T, T> {
    final Consumer<? super T> onNext;
    final Consumer<? super Throwable> onError;
    final Function0 onComplete;
    final Function0 onAfterTerminate;

    public FlowableDoOnEach(Flowable<T> source, Consumer<? super T> onNext,
                            Consumer<? super Throwable> onError,
                            Function0 onComplete,
                            Function0 onAfterTerminate) {
        super(source);
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onAfterTerminate = onAfterTerminate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new DoOnEachConditionalSubscriber<T>(
                    (ConditionalSubscriber<? super T>)s, onNext, onError, onComplete, onAfterTerminate));
        } else {
            source.subscribe(new DoOnEachSubscriber<T>(
                    s, onNext, onError, onComplete, onAfterTerminate));
        }
    }

    static final class DoOnEachSubscriber<T> extends BasicFuseableSubscriber<T, T> {
        final Consumer<? super T> onNext;
        final Consumer<? super Throwable> onError;
        final Function0 onComplete;
        final Function0 onAfterTerminate;

        DoOnEachSubscriber(
                Subscriber<? super T> actual,
                Consumer<? super T> onNext,
                Consumer<? super Throwable> onError,
                Function0 onComplete,
                Function0 onAfterTerminate) {
            super(actual);
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onAfterTerminate = onAfterTerminate;
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

            try {
                onNext.accept(t);
            } catch (Throwable e) {
                fail(e);
                return;
            }

            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaCommonPlugins.onError(t);
                return;
            }
            done = true;
            boolean relay = true;
            try {
                onError.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(t, e));
                relay = false;
            }
            if (relay) {
                actual.onError(t);
            }

            try {
                onAfterTerminate.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            try {
                onComplete.invoke();
            } catch (Throwable e) {
                fail(e);
                return;
            }

            done = true;
            actual.onComplete();

            try {
                onAfterTerminate.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            T v = qs.poll();

            if (v != null) {
                try {
                    onNext.accept(v);
                } finally {
                    onAfterTerminate.invoke();
                }
            } else {
                if (sourceMode == SYNC) {
                    onComplete.invoke();

                    onAfterTerminate.invoke();
                }
            }
            return v;
        }
    }

    static final class DoOnEachConditionalSubscriber<T> extends BasicFuseableConditionalSubscriber<T, T> {
        final Consumer<? super T> onNext;
        final Consumer<? super Throwable> onError;
        final Function0 onComplete;
        final Function0 onAfterTerminate;

        DoOnEachConditionalSubscriber(
                ConditionalSubscriber<? super T> actual,
                Consumer<? super T> onNext,
                Consumer<? super Throwable> onError,
                Function0 onComplete,
                Function0 onAfterTerminate) {
            super(actual);
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onAfterTerminate = onAfterTerminate;
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

            try {
                onNext.accept(t);
            } catch (Throwable e) {
                fail(e);
                return;
            }

            actual.onNext(t);
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }

            try {
                onNext.accept(t);
            } catch (Throwable e) {
                fail(e);
                return false;
            }

            return actual.tryOnNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaCommonPlugins.onError(t);
                return;
            }
            done = true;
            boolean relay = true;
            try {
                onError.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(t, e));
                relay = false;
            }
            if (relay) {
                actual.onError(t);
            }

            try {
                onAfterTerminate.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            try {
                onComplete.invoke();
            } catch (Throwable e) {
                fail(e);
                return;
            }

            done = true;
            actual.onComplete();

            try {
                onAfterTerminate.invoke();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaCommonPlugins.onError(e);
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            T v = qs.poll();

            if (v != null) {
                try {
                    onNext.accept(v);
                } finally {
                    onAfterTerminate.invoke();
                }
            } else {
                if (sourceMode == SYNC) {
                    onComplete.invoke();

                    onAfterTerminate.invoke();
                }
            }
            return v;
        }
    }
}
