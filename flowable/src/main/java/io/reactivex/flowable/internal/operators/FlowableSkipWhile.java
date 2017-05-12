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
import io.reactivex.common.exceptions.Exceptions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.SubscriptionHelper;
import kotlin.jvm.functions.Function1;

public final class FlowableSkipWhile<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function1<? super T, Boolean> predicate;

    public FlowableSkipWhile(Flowable<T> source, Function1<? super T, Boolean> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SkipWhileSubscriber<T>(s, predicate));
    }

    static final class SkipWhileSubscriber<T> implements RelaxedSubscriber<T>, Subscription {
        final Subscriber<? super T> actual;
        final Function1<? super T, Boolean> predicate;
        Subscription s;
        boolean notSkipping;

        SkipWhileSubscriber(Subscriber<? super T> actual, Function1<? super T, Boolean> predicate) {
            this.actual = actual;
            this.predicate = predicate;
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
            if (notSkipping) {
                actual.onNext(t);
            } else {
                boolean b;
                try {
                    b = predicate.invoke(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    s.cancel();
                    actual.onError(e);
                    return;
                }
                if (b) {
                    s.request(1);
                } else {
                    notSkipping = true;
                    actual.onNext(t);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
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
