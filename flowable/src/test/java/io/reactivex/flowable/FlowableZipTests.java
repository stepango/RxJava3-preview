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

import org.junit.Test;
import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.NoSuchElementException;

import io.reactivex.common.functions.BiFunction;
import io.reactivex.flowable.FlowableCovarianceTest.CoolRating;
import io.reactivex.flowable.FlowableCovarianceTest.ExtendedResult;
import io.reactivex.flowable.FlowableCovarianceTest.HorrorMovie;
import io.reactivex.flowable.FlowableCovarianceTest.Media;
import io.reactivex.flowable.FlowableCovarianceTest.Movie;
import io.reactivex.flowable.FlowableCovarianceTest.Rating;
import io.reactivex.flowable.FlowableCovarianceTest.Result;
import io.reactivex.flowable.FlowableEventStream.Event;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class FlowableZipTests {

    @Test
    public void testZipObservableOfObservables() {
        FlowableEventStream.getEventStream("HTTP-ClusterB", 20)
                .groupBy(new Function1<Event, String>() {
                    @Override
                    public String invoke(Event e) {
                        return e.instanceId;
                    }
                })
                // now we have streams of cluster+instanceId
                .flatMap(new Function1<GroupedFlowable<String, Event>, Publisher<HashMap<String, String>>>() {
                    @Override
                    public Publisher<HashMap<String, String>> invoke(final GroupedFlowable<String, Event> ge) {
                            return ge.scan(new HashMap<String, String>(), new BiFunction<HashMap<String, String>, Event, HashMap<String, String>>() {
                                @Override
                                public HashMap<String, String> apply(HashMap<String, String> accum,
                                        Event perInstanceEvent) {
                                    synchronized (accum) {
                                            accum.put("instance", ge.getKey());
                                    }
                                    return accum;
                                }
                            });
                    }
                })
                .take(10)
                .blockingForEach(new Function1<HashMap<String, String>, kotlin.Unit>() {
                    @Override
                    public Unit invoke(HashMap<String, String> v) {
                        synchronized (v) {
                            System.out.println(v);
                        }
                        return Unit.INSTANCE;
                    }
                });

        System.out.println("**** finished");
    }

    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void testCovarianceOfZip() {
        Flowable<HorrorMovie> horrors = Flowable.just(new HorrorMovie());
        Flowable<CoolRating> ratings = Flowable.just(new CoolRating());

        Flowable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).blockingForEach(extendedAction);
        Flowable.<Media, Rating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).blockingForEach(action);

        Flowable.<Movie, CoolRating, Result> zip(horrors, ratings, combine);
    }

    /**
     * Occasionally zip may be invoked with 0 observables. Test that we don't block indefinitely instead
     * of immediately invoking zip with 0 argument.
     *
     * We now expect an NoSuchElementException since last() requires at least one value and nothing will be emitted.
     */
    @Test(expected = NoSuchElementException.class)
    public void nonBlockingObservable() {

        final Object invoked = new Object();

        Collection<Flowable<Object>> observables = Collections.emptyList();

        Flowable<Object> result = Flowable.zip(observables, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] args) {
                System.out.println("received: " + args);
                assertEquals("No argument should have been passed", 0, args.length);
                return invoked;
            }
        });

        assertSame(invoked, result.blockingLast());
    }

    BiFunction<Media, Rating, ExtendedResult> combine = new BiFunction<Media, Rating, ExtendedResult>() {
        @Override
        public ExtendedResult apply(Media m, Rating r) {
                return new ExtendedResult();
        }
    };

    Function1<Result, kotlin.Unit> action = new Function1<Result, kotlin.Unit>() {
        @Override
        public Unit invoke(Result t1) {
            System.out.println("Result: " + t1);
            return Unit.INSTANCE;
        }
    };

    Function1<ExtendedResult, kotlin.Unit> extendedAction = new Function1<ExtendedResult, kotlin.Unit>() {
        @Override
        public Unit invoke(ExtendedResult t1) {
            System.out.println("Result: " + t1);
            return Unit.INSTANCE;
        }
    };


    @Test
    public void zipWithDelayError() {
        Flowable.just(1)
        .zipWith(Flowable.just(2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true)
        .test()
        .assertResult(3);
    }

    @Test
    public void zipWithDelayErrorBufferSize() {
        Flowable.just(1)
        .zipWith(Flowable.just(2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true, 16)
        .test()
        .assertResult(3);
    }
}
