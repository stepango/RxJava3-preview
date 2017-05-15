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
package io.reactivex.observable.internal.operators;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Emitter;
import io.reactivex.common.Notification;
import io.reactivex.common.Scheduler;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.common.internal.functions.ObjectHelper;
import io.reactivex.observable.ConnectableObservable;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.RxJavaObservablePlugins;
import io.reactivex.observable.SingleSource;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/**
 * Helper utility class to support Observable with inner classes.
 */
public final class ObservableInternalHelper {

    private ObservableInternalHelper() {
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

    static final class ItemDelayFunction<T, U> implements Function1<T, ObservableSource<T>> {
        final Function1<? super T, ? extends ObservableSource<U>> itemDelay;

        ItemDelayFunction(Function1<? super T, ? extends ObservableSource<U>> itemDelay) {
            this.itemDelay = itemDelay;
        }

        @Override
        public ObservableSource<T> invoke(final T v) {
            return new ObservableTake<U>(itemDelay.invoke(v), 1).map(Functions.justFunction(v)).defaultIfEmpty(v);
        }
    }

    public static <T, U> Function1<T, ObservableSource<T>> itemDelay(final Function1<? super T, ? extends ObservableSource<U>> itemDelay) {
        return new ItemDelayFunction<T, U>(itemDelay);
    }


    static final class ObserverOnNext<T> implements Function1<T, kotlin.Unit> {
        final Observer<T> observer;

        ObserverOnNext(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public Unit invoke(T v) {
            observer.onNext(v);
            return Unit.INSTANCE;
        }
    }

    static final class ObserverOnError<T> implements Function1<Throwable, kotlin.Unit> {
        final Observer<T> observer;

        ObserverOnError(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public Unit invoke(Throwable v) {
            observer.onError(v);
            return Unit.INSTANCE;
        }
    }

    static final class ObserverOnComplete<T> implements Function0 {
        final Observer<T> observer;

        ObserverOnComplete(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public kotlin.Unit invoke() {
            observer.onComplete();
            return Unit.INSTANCE;
        }
    }

    public static <T> Function1<T, kotlin.Unit> observerOnNext(Observer<T> observer) {
        return new ObserverOnNext<T>(observer);
    }

    public static <T> Function1<Throwable, kotlin.Unit> observerOnError(Observer<T> observer) {
        return new ObserverOnError<T>(observer);
    }

    public static <T> Function0 observerOnComplete(Observer<T> observer) {
        return new ObserverOnComplete<T>(observer);
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

    static final class FlatMapWithCombinerOuter<T, R, U> implements Function1<T, ObservableSource<R>> {
        private final Function2<? super T, ? super U, ? extends R> combiner;
        private final Function1<? super T, ? extends ObservableSource<? extends U>> mapper;

        FlatMapWithCombinerOuter(Function2<? super T, ? super U, ? extends R> combiner,
                                 Function1<? super T, ? extends ObservableSource<? extends U>> mapper) {
            this.combiner = combiner;
            this.mapper = mapper;
        }

        @Override
        public ObservableSource<R> invoke(final T t) {
            @SuppressWarnings("unchecked")
            ObservableSource<U> u = (ObservableSource<U>) mapper.invoke(t);
            return new ObservableMap<U, R>(u, new FlatMapWithCombinerInner<U, R, T>(combiner, t));
        }
    }

    public static <T, U, R> Function1<T, ObservableSource<R>> flatMapWithCombiner(
            final Function1<? super T, ? extends ObservableSource<? extends U>> mapper,
                    final Function2<? super T, ? super U, ? extends R> combiner) {
        return new FlatMapWithCombinerOuter<T, R, U>(combiner, mapper);
    }

    static final class FlatMapIntoIterable<T, U> implements Function1<T, ObservableSource<U>> {
        private final Function1<? super T, ? extends Iterable<? extends U>> mapper;

        FlatMapIntoIterable(Function1<? super T, ? extends Iterable<? extends U>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public ObservableSource<U> invoke(T t) {
            return new ObservableFromIterable<U>(mapper.invoke(t));
        }
    }

    public static <T, U> Function1<T, ObservableSource<U>> flatMapIntoIterable(final Function1<? super T, ? extends Iterable<? extends U>> mapper) {
        return new FlatMapIntoIterable<T, U>(mapper);
    }

    enum MapToInt implements Function1<Object, Object> {
        INSTANCE;
        @Override
        public Object invoke(Object t) {
            return 0;
        }
    }

    static final class RepeatWhenOuterHandler
            implements Function1<Observable<Notification<Object>>, ObservableSource<?>> {
        private final Function1<? super Observable<Object>, ? extends ObservableSource<?>> handler;

        RepeatWhenOuterHandler(Function1<? super Observable<Object>, ? extends ObservableSource<?>> handler) {
            this.handler = handler;
        }

        @Override
        public ObservableSource<?> invoke(Observable<Notification<Object>> no) {
            return handler.invoke(no.map(MapToInt.INSTANCE));
        }
    }

    public static Function1<Observable<Notification<Object>>, ObservableSource<?>> repeatWhenHandler(final Function1<? super Observable<Object>, ? extends ObservableSource<?>> handler) {
        return new RepeatWhenOuterHandler(handler);
    }

    public static <T> Callable<ConnectableObservable<T>> replayCallable(final Observable<T> parent) {
        return new ReplayCallable<T>(parent);
    }

    public static <T> Callable<ConnectableObservable<T>> replayCallable(final Observable<T> parent, final int bufferSize) {
        return new BufferedReplayCallable<T>(parent, bufferSize);
    }

    public static <T> Callable<ConnectableObservable<T>> replayCallable(final Observable<T> parent, final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new BufferedTimedReplayCallable<T>(parent, bufferSize, time, unit, scheduler);
    }

    public static <T> Callable<ConnectableObservable<T>> replayCallable(final Observable<T> parent, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new TimedReplayCallable<T>(parent, time, unit, scheduler);
    }

    public static <T, R> Function1<Observable<T>, ObservableSource<R>> replayFunction(final Function1<? super Observable<T>, ? extends ObservableSource<R>> selector, final Scheduler scheduler) {
        return new ReplayFunction<T, R>(selector, scheduler);
    }

    enum ErrorMapper implements Function1<Notification<Object>, Throwable> {
        INSTANCE;

        @Override
        public Throwable invoke(Notification<Object> t) {
            return t.getError();
        }
    }

    enum FilterMapper implements Function1<Notification<Object>, Boolean> {
        INSTANCE;

        @Override
        public Boolean invoke(Notification<Object> t) {
            return t.isOnError();
        }
    }

    static final class RetryWhenInner
            implements Function1<Observable<Notification<Object>>, ObservableSource<?>> {
        private final Function1<? super Observable<Throwable>, ? extends ObservableSource<?>> handler;

        RetryWhenInner(
                Function1<? super Observable<Throwable>, ? extends ObservableSource<?>> handler) {
            this.handler = handler;
        }

        @Override
        public ObservableSource<?> invoke(Observable<Notification<Object>> no) {
            Observable<Throwable> map = no
                    .takeWhile(FilterMapper.INSTANCE)
                    .map(ErrorMapper.INSTANCE);
            return handler.invoke(map);
        }
    }

    public static <T> Function1<Observable<Notification<Object>>, ObservableSource<?>> retryWhenHandler(final Function1<? super Observable<Throwable>, ? extends ObservableSource<?>> handler) {
        return new RetryWhenInner(handler);
    }

    static final class ZipIterableFunction<T, R>
            implements Function1<List<ObservableSource<? extends T>>, ObservableSource<? extends R>> {
        private final Function1<? super Object[], ? extends R> zipper;

        ZipIterableFunction(Function1<? super Object[], ? extends R> zipper) {
            this.zipper = zipper;
        }

        @Override
        public ObservableSource<? extends R> invoke(List<ObservableSource<? extends T>> list) {
            return Observable.zipIterable(list, zipper, false, Observable.bufferSize());
        }
    }

    public static <T, R> Function1<List<ObservableSource<? extends T>>, ObservableSource<? extends R>> zipIterable(final Function1<? super Object[], ? extends R> zipper) {
        return new ZipIterableFunction<T, R>(zipper);
    }

    public static <T, R> Observable<R> switchMapSingle(Observable<T> source, final Function1<? super T, ? extends SingleSource<? extends R>> mapper) {
        return source.switchMap(convertSingleMapperToObservableMapper(mapper), 1);
    }

    public static <T,R> Observable<R> switchMapSingleDelayError(Observable<T> source,
                                                                Function1<? super T, ? extends SingleSource<? extends R>> mapper) {
        return source.switchMapDelayError(convertSingleMapperToObservableMapper(mapper), 1);
    }

    private static <T, R> Function1<T, Observable<R>> convertSingleMapperToObservableMapper(
            final Function1<? super T, ? extends SingleSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return new ObservableMapper<T,R>(mapper);
    }

    static final class ObservableMapper<T, R> implements Function1<T, Observable<R>> {

        final Function1<? super T, ? extends SingleSource<? extends R>> mapper;

        ObservableMapper(Function1<? super T, ? extends SingleSource<? extends R>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Observable<R> invoke(T t) {
            return RxJavaObservablePlugins.onAssembly(new SingleToObservable<R>(
                    ObjectHelper.requireNonNull(mapper.invoke(t), "The mapper returned a null value")));
        }

    }

    static final class ReplayCallable<T> implements Callable<ConnectableObservable<T>> {
        private final Observable<T> parent;

        ReplayCallable(Observable<T> parent) {
            this.parent = parent;
        }

        @Override
        public ConnectableObservable<T> call() {
            return parent.replay();
        }
    }

    static final class BufferedReplayCallable<T> implements Callable<ConnectableObservable<T>> {
        private final Observable<T> parent;
        private final int bufferSize;

        BufferedReplayCallable(Observable<T> parent, int bufferSize) {
            this.parent = parent;
            this.bufferSize = bufferSize;
        }

        @Override
        public ConnectableObservable<T> call() {
            return parent.replay(bufferSize);
        }
    }

    static final class BufferedTimedReplayCallable<T> implements Callable<ConnectableObservable<T>> {
        private final Observable<T> parent;
        private final int bufferSize;
        private final long time;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        BufferedTimedReplayCallable(Observable<T> parent, int bufferSize, long time, TimeUnit unit, Scheduler scheduler) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public ConnectableObservable<T> call() {
            return parent.replay(bufferSize, time, unit, scheduler);
        }
    }

    static final class TimedReplayCallable<T> implements Callable<ConnectableObservable<T>> {
        private final Observable<T> parent;
        private final long time;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        TimedReplayCallable(Observable<T> parent, long time, TimeUnit unit, Scheduler scheduler) {
            this.parent = parent;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public ConnectableObservable<T> call() {
            return parent.replay(time, unit, scheduler);
        }
    }

    static final class ReplayFunction<T, R> implements Function1<Observable<T>, ObservableSource<R>> {
        private final Function1<? super Observable<T>, ? extends ObservableSource<R>> selector;
        private final Scheduler scheduler;

        ReplayFunction(Function1<? super Observable<T>, ? extends ObservableSource<R>> selector, Scheduler scheduler) {
            this.selector = selector;
            this.scheduler = scheduler;
        }

        @Override
        public ObservableSource<R> invoke(Observable<T> t) {
            return Observable.wrap(selector.invoke(t)).observeOn(scheduler);
        }
    }
}
