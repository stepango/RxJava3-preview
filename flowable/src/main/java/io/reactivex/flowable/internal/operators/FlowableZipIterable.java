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

import java.util.Iterator;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.Exceptions;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.EmptySubscription;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;

public final class FlowableZipIterable<T, U, V> extends AbstractFlowableWithUpstream<T, V> {
    final Iterable<U> other;
    final Function2<? super T, ? super U, ? extends V> zipper;

    public FlowableZipIterable(
            Flowable<T> source,
            Iterable<U> other, Function2<? super T, ? super U, ? extends V> zipper) {
        super(source);
        this.other = other;
        this.zipper = zipper;
    }

    @Override
    public void subscribeActual(Subscriber<? super V> t) {
        Iterator<U> it;

        try {
            it = ObjectHelper.requireNonNull(other.iterator(), "The iterator returned by other is null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, t);
            return;
        }

        boolean b;

        try {
            b = it.hasNext();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, t);
            return;
        }

        if (!b) {
            EmptySubscription.complete(t);
            return;
        }

        source.subscribe(new ZipIterableSubscriber<T, U, V>(t, it, zipper));
    }

    static final class ZipIterableSubscriber<T, U, V> implements RelaxedSubscriber<T>, Subscription {
        final Subscriber<? super V> actual;
        final Iterator<U> iterator;
        final Function2<? super T, ? super U, ? extends V> zipper;

        Subscription s;

        boolean done;

        ZipIterableSubscriber(Subscriber<? super V> actual, Iterator<U> iterator,
                Function2<? super T, ? super U, ? extends V> zipper) {
            this.actual = actual;
            this.iterator = iterator;
            this.zipper = zipper;
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
            if (done) {
                return;
            }

            U u;

            try {
                u = ObjectHelper.requireNonNull(iterator.next(), "The iterator returned a null value");
            } catch (Throwable e) {
                error(e);
                return;
            }

            V v;
            try {
                v = ObjectHelper.requireNonNull(zipper.invoke(t, u), "The zipper function returned a null value");
            } catch (Throwable e) {
                error(e);
                return;
            }

            actual.onNext(v);

            boolean b;

            try {
                b = iterator.hasNext();
            } catch (Throwable e) {
                error(e);
                return;
            }

            if (!b) {
                done = true;
                s.cancel();
                actual.onComplete();
            }
        }

        void error(Throwable e) {
            Exceptions.throwIfFatal(e);
            done = true;
            s.cancel();
            actual.onError(e);
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
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }

    }
}
