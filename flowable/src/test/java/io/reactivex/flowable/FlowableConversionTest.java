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

package io.reactivex.flowable;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Schedulers;
import io.reactivex.common.internal.utils.ExceptionHelper;
import io.reactivex.flowable.internal.operators.FlowableFilter;
import io.reactivex.flowable.internal.operators.FlowableMap;
import io.reactivex.flowable.subscribers.DefaultSubscriber;
import kotlin.jvm.functions.Function1;

public class FlowableConversionTest {

    public static class Cylon { }

    public static class Jail {
        Object cylon;

        Jail(Object cylon) {
            this.cylon = cylon;
        }
    }

    public static class CylonDetectorObservable<T> {
        protected Publisher<T> onSubscribe;

        public static <T> CylonDetectorObservable<T> create(Publisher<T> onSubscribe) {
            return new CylonDetectorObservable<T>(onSubscribe);
        }

        protected CylonDetectorObservable(Publisher<T> onSubscribe) {
            this.onSubscribe = onSubscribe;
        }

        public void subscribe(Subscriber<T> subscriber) {
            onSubscribe.subscribe(subscriber);
        }

        public <R> CylonDetectorObservable<R> lift(FlowableOperator<? extends R, ? super T> operator) {
            return x(new RobotConversionFunc<T, R>(operator));
        }

        public <R, O> O x(Function1<Publisher<T>, O> operator) {
            try {
                return operator.invoke(onSubscribe);
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        public <R> CylonDetectorObservable<? extends R> compose(Function1<CylonDetectorObservable<? super T>, CylonDetectorObservable<? extends R>> transformer) {
            try {
                return transformer.invoke(this);
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        public final CylonDetectorObservable<T> beep(Function1<? super T, Boolean> predicate) {
            return new CylonDetectorObservable<T>(new FlowableFilter<T>(Flowable.fromPublisher(onSubscribe), predicate));
        }

        public final <R> CylonDetectorObservable<R> boop(Function1<? super T, ? extends R> func) {
            return new CylonDetectorObservable<R>(new FlowableMap<T, R>(Flowable.fromPublisher(onSubscribe), func));
        }

        public CylonDetectorObservable<String> DESTROY() {
            return boop(new Function1<T, String>() {
                @Override
                public String invoke(T t) {
                    Object cylon = ((Jail) t).cylon;
                    throwOutTheAirlock(cylon);
                    if (t instanceof Jail) {
                        String name = cylon.toString();
                        return "Cylon '" + name + "' has been destroyed";
                    }
                    else {
                        return "Cylon 'anonymous' has been destroyed";
                    }
                }});
        }

        private static void throwOutTheAirlock(Object cylon) {
            // ...
        }
    }

    public static class RobotConversionFunc<T, R> implements Function1<Publisher<T>, CylonDetectorObservable<R>> {
        private FlowableOperator<? extends R, ? super T> operator;

        public RobotConversionFunc(FlowableOperator<? extends R, ? super T> operator) {
            this.operator = operator;
        }

        @Override
        public CylonDetectorObservable<R> invoke(final Publisher<T> onSubscribe) {
            return CylonDetectorObservable.create(new Publisher<R>() {
                @Override
                public void subscribe(Subscriber<? super R> o) {
                    try {
                        Subscriber<? super T> st = operator.apply(o);
                        try {
                            onSubscribe.subscribe(st);
                        } catch (Throwable e) {
                            st.onError(e);
                        }
                    } catch (Throwable e) {
                        o.onError(e);
                    }

                }});
        }
    }

    public static class ConvertToCylonDetector<T> implements Function1<Publisher<T>, CylonDetectorObservable<T>> {
        @Override
        public CylonDetectorObservable<T> invoke(final Publisher<T> onSubscribe) {
            return CylonDetectorObservable.create(onSubscribe);
        }
    }

    public static class ConvertToObservable<T> implements Function1<Publisher<T>, Flowable<T>> {
        @Override
        public Flowable<T> invoke(final Publisher<T> onSubscribe) {
            return Flowable.fromPublisher(onSubscribe);
        }
    }

    @Test
    public void testConvertToConcurrentQueue() {
        final AtomicReference<Throwable> thrown = new AtomicReference<Throwable>(null);
        final AtomicBoolean isFinished = new AtomicBoolean(false);
        ConcurrentLinkedQueue<? extends Integer> queue = Flowable.range(0,5)
                .flatMap(new Function1<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> invoke(final Integer i) {
                        return Flowable.range(0, 5)
                                .observeOn(Schedulers.io())
                                .map(new Function1<Integer, Integer>() {
                                    @Override
                                    public Integer invoke(Integer k) {
                                        try {
                                            Thread.sleep(System.currentTimeMillis() % 100);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        return i + k;
                                    }
                                });
                    }
                })
                .to(new Function1<Flowable<Integer>, ConcurrentLinkedQueue<Integer>>() {
                        @Override
                        public ConcurrentLinkedQueue<Integer> invoke(Flowable<Integer> onSubscribe) {
                            final ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<Integer>();
                            onSubscribe.subscribe(new DefaultSubscriber<Integer>() {
                                @Override
                                public void onComplete() {
                                    isFinished.set(true);
                                }

                                @Override
                                public void onError(Throwable e) {
                                    thrown.set(e);
                                }

                                @Override
                                public void onNext(Integer t) {
                                    q.add(t);
                                }});
                            return q;
                        }
                    });

        int x = 0;
        while (!isFinished.get()) {
            Integer i = queue.poll();
            if (i != null) {
                x++;
                System.out.println(x + " item: " + i);
            }
        }
        Assert.assertNull(thrown.get());
    }
}
