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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Emitter;
import io.reactivex.common.Scheduler;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.ConnectableFlowable;
import io.reactivex.flowable.Flowable;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/**
 * Helper utility class to support Flowable with inner classes.
 */
public final class FlowableInternalHelper {

    /** Utility class. */
    private FlowableInternalHelper() {
        throw new IllegalStateException("No instances!");
    }

    static final class SimpleGenerator<T, S> implements Function2<S, Emitter<T>, S> {
        final Function1<Emitter<T>, kotlin.Unit> consumer;

        SimpleGenerator(Function1<Emitter<T>, kotlin.Unit> consumer) {
            this.consumer = consumer;
        }

        @Override
        public S invoke(S t1, Emitter<T> t2) {
            consumer.invoke(t2);
            return t1;
        }
    }

    public static <T, S> Function2<S, Emitter<T>, S> simpleGenerator(Function1<Emitter<T>, kotlin.Unit> consumer) {
        return new SimpleGenerator<T, S>(consumer);
    }

    static final class SimpleBiGenerator<T, S> implements Function2<S, Emitter<T>, S> {
        final Function2<S, Emitter<T>, kotlin.Unit> consumer;

        SimpleBiGenerator(Function2<S, Emitter<T>, kotlin.Unit> consumer) {
            this.consumer = consumer;
        }

        @Override
        public S invoke(S t1, Emitter<T> t2) {
            consumer.invoke(t1, t2);
            return t1;
        }
    }

    public static <T, S> Function2<S, Emitter<T>, S> simpleBiGenerator(Function2<S, Emitter<T>, kotlin.Unit> consumer) {
        return new SimpleBiGenerator<T, S>(consumer);
    }

    static final class ItemDelayFunction<T, U> implements Function1<T, Publisher<T>> {
        final Function1<? super T, ? extends Publisher<U>> itemDelay;

        ItemDelayFunction(Function1<? super T, ? extends Publisher<U>> itemDelay) {
            this.itemDelay = itemDelay;
        }

        @Override
        public Publisher<T> invoke(final T v) {
            return new FlowableTakePublisher<U>(itemDelay.invoke(v), 1).map(Functions.justFunction(v)).defaultIfEmpty(v);
        }
    }

    public static <T, U> Function1<T, Publisher<T>> itemDelay(final Function1<? super T, ? extends Publisher<U>> itemDelay) {
        return new ItemDelayFunction<T, U>(itemDelay);
    }

    static final class SubscriberOnNext<T> implements Function1<T, kotlin.Unit> {
        final Subscriber<T> subscriber;

        SubscriberOnNext(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public Unit invoke(T v) {
            subscriber.onNext(v);
            return Unit.INSTANCE;
        }
    }

    static final class SubscriberOnError<T> implements Function1<Throwable, kotlin.Unit> {
        final Subscriber<T> subscriber;

        SubscriberOnError(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public Unit invoke(Throwable v) {
            subscriber.onError(v);
            return Unit.INSTANCE;
        }
    }

    static final class SubscriberOnComplete<T> implements Function0 {
        final Subscriber<T> subscriber;

        SubscriberOnComplete(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public kotlin.Unit invoke() {
            subscriber.onComplete();
            return Unit.INSTANCE;
        }
    }

    public static <T> Function1<T, kotlin.Unit> subscriberOnNext(Subscriber<T> subscriber) {
        return new SubscriberOnNext<T>(subscriber);
    }

    public static <T> Function1<Throwable, kotlin.Unit> subscriberOnError(Subscriber<T> subscriber) {
        return new SubscriberOnError<T>(subscriber);
    }

    public static <T> Function0 subscriberOnComplete(Subscriber<T> subscriber) {
        return new SubscriberOnComplete<T>(subscriber);
    }

    static final class FlatMapWithCombinerInner<U, R, T> implements Function1<U, R> {
        private final Function2<? super T, ? super U, ? extends R> combiner;
        private final T t;

        FlatMapWithCombinerInner(Function2<? super T, ? super U, ? extends R> combiner, T t) {
            this.combiner = combiner;
            this.t = t;
        }

        @Override
        public R invoke(U w) {
            try {
                return combiner.invoke(t, w);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class FlatMapWithCombinerOuter<T, R, U> implements Function1<T, Publisher<R>> {
        private final Function2<? super T, ? super U, ? extends R> combiner;
        private final Function1<? super T, ? extends Publisher<? extends U>> mapper;

        FlatMapWithCombinerOuter(Function2<? super T, ? super U, ? extends R> combiner,
                                 Function1<? super T, ? extends Publisher<? extends U>> mapper) {
            this.combiner = combiner;
            this.mapper = mapper;
        }

        @Override
        public Publisher<R> invoke(final T t) {
            @SuppressWarnings("unchecked")
            Publisher<U> u = (Publisher<U>) mapper.invoke(t);
            return new FlowableMapPublisher<U, R>(u, new FlatMapWithCombinerInner<U, R, T>(combiner, t));
        }
    }

    public static <T, U, R> Function1<T, Publisher<R>> flatMapWithCombiner(
            final Function1<? super T, ? extends Publisher<? extends U>> mapper,
                    final Function2<? super T, ? super U, ? extends R> combiner) {
        return new FlatMapWithCombinerOuter<T, R, U>(combiner, mapper);
    }

    static final class FlatMapIntoIterable<T, U> implements Function1<T, Publisher<U>> {
        private final Function1<? super T, ? extends Iterable<? extends U>> mapper;

        FlatMapIntoIterable(Function1<? super T, ? extends Iterable<? extends U>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Publisher<U> invoke(T t) {
            return new FlowableFromIterable<U>(mapper.invoke(t));
        }
    }

    public static <T, U> Function1<T, Publisher<U>> flatMapIntoIterable(final Function1<? super T, ? extends Iterable<? extends U>> mapper) {
        return new FlatMapIntoIterable<T, U>(mapper);
    }

    public static <T> Callable<ConnectableFlowable<T>> replayCallable(final Flowable<T> parent) {
        return new ReplayCallable<T>(parent);
    }

    public static <T> Callable<ConnectableFlowable<T>> replayCallable(final Flowable<T> parent, final int bufferSize) {
        return new BufferedReplayCallable<T>(parent, bufferSize);
    }

    public static <T> Callable<ConnectableFlowable<T>> replayCallable(final Flowable<T> parent, final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new BufferedTimedReplay<T>(parent, bufferSize, time, unit, scheduler);
    }

    public static <T> Callable<ConnectableFlowable<T>> replayCallable(final Flowable<T> parent, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new TimedReplay<T>(parent, time, unit, scheduler);
    }

    public static <T, R> Function1<Flowable<T>, Publisher<R>> replayFunction(final Function1<? super Flowable<T>, ? extends Publisher<R>> selector, final Scheduler scheduler) {
        return new ReplayFunction<T, R>(selector, scheduler);
    }

    public enum RequestMax implements Function1<Subscription, kotlin.Unit> {
        INSTANCE;
        @Override
        public Unit invoke(Subscription t) {
            t.request(Long.MAX_VALUE);
            return Unit.INSTANCE;
        }
    }

    static final class ZipIterableFunction<T, R>
            implements Function1<List<Publisher<? extends T>>, Publisher<? extends R>> {
        private final Function1<? super Object[], ? extends R> zipper;

        ZipIterableFunction(Function1<? super Object[], ? extends R> zipper) {
            this.zipper = zipper;
        }

        @Override
        public Publisher<? extends R> invoke(List<Publisher<? extends T>> list) {
            return Flowable.zipIterable(list, zipper, false, Flowable.bufferSize());
        }
    }

    public static <T, R> Function1<List<Publisher<? extends T>>, Publisher<? extends R>> zipIterable(final Function1<? super Object[], ? extends R> zipper) {
        return new ZipIterableFunction<T, R>(zipper);
    }

    static final class ReplayCallable<T> implements Callable<ConnectableFlowable<T>> {
        private final Flowable<T> parent;

        ReplayCallable(Flowable<T> parent) {
            this.parent = parent;
        }

        @Override
        public ConnectableFlowable<T> call() {
            return parent.replay();
        }
    }

    static final class BufferedReplayCallable<T> implements Callable<ConnectableFlowable<T>> {
        private final Flowable<T> parent;
        private final int bufferSize;

        BufferedReplayCallable(Flowable<T> parent, int bufferSize) {
            this.parent = parent;
            this.bufferSize = bufferSize;
        }

        @Override
        public ConnectableFlowable<T> call() {
            return parent.replay(bufferSize);
        }
    }

    static final class BufferedTimedReplay<T> implements Callable<ConnectableFlowable<T>> {
        private final Flowable<T> parent;
        private final int bufferSize;
        private final long time;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        BufferedTimedReplay(Flowable<T> parent, int bufferSize, long time, TimeUnit unit, Scheduler scheduler) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public ConnectableFlowable<T> call() {
            return parent.replay(bufferSize, time, unit, scheduler);
        }
    }

    static final class TimedReplay<T> implements Callable<ConnectableFlowable<T>> {
        private final Flowable<T> parent;
        private final long time;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        TimedReplay(Flowable<T> parent, long time, TimeUnit unit, Scheduler scheduler) {
            this.parent = parent;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public ConnectableFlowable<T> call() {
            return parent.replay(time, unit, scheduler);
        }
    }

    static final class ReplayFunction<T, R> implements Function1<Flowable<T>, Publisher<R>> {
        private final Function1<? super Flowable<T>, ? extends Publisher<R>> selector;
        private final Scheduler scheduler;

        ReplayFunction(Function1<? super Flowable<T>, ? extends Publisher<R>> selector, Scheduler scheduler) {
            this.selector = selector;
            this.scheduler = scheduler;
        }

        @Override
        public Publisher<R> invoke(Flowable<T> t) {
            return Flowable.fromPublisher(selector.invoke(t)).observeOn(scheduler);
        }
    }
}
