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

import kotlin.jvm.functions.Function2;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.operators.FlowableReduceWith.ReduceWithSubscriber;

public final class FlowableReduceSeed<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final R initialValue;

    final Function2<R, ? super T, R> reducer;

    public FlowableReduceSeed(Flowable<T> source, R initialValue, Function2<R, ? super T, R> reducer) {
        super(source);
        this.initialValue = initialValue;
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new ReduceWithSubscriber<T, R>(s, initialValue, reducer));
    }

}
