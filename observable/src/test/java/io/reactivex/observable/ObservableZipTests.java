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

package io.reactivex.observable;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.NoSuchElementException;

import io.reactivex.common.functions.BiFunction;
import io.reactivex.observable.ObservableCovarianceTest.CoolRating;
import io.reactivex.observable.ObservableCovarianceTest.ExtendedResult;
import io.reactivex.observable.ObservableCovarianceTest.HorrorMovie;
import io.reactivex.observable.ObservableCovarianceTest.Media;
import io.reactivex.observable.ObservableCovarianceTest.Movie;
import io.reactivex.observable.ObservableCovarianceTest.Rating;
import io.reactivex.observable.ObservableCovarianceTest.Result;
import io.reactivex.observable.ObservableEventStream.Event;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertSame;

public class ObservableZipTests {

    @Test
    public void testZipObservableOfObservables() throws Exception {
        ObservableEventStream.getEventStream("HTTP-ClusterB", 20)
                .groupBy(new Function1<Event, String>() {
                    @Override
                    public String invoke(Event e) {
                        return e.instanceId;
                    }
                })
                // now we have streams of cluster+instanceId
                .flatMap(new Function1<GroupedObservable<String, Event>, Observable<HashMap<String, String>>>() {
                    @Override
                    public Observable<HashMap<String, String>> invoke(final GroupedObservable<String, Event> ge) {
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
                .blockingForEach(new Function1<Object, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Object pv) {
                        synchronized (pv) {
                            System.out.println(pv);
                        }
                        return Unit.INSTANCE;
                    }
                });

        System.out.println("**** finished");

        Thread.sleep(200); // make sure the event streams receive their interrupt
    }

    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void testCovarianceOfZip() {
        Observable<HorrorMovie> horrors = Observable.just(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.just(new CoolRating());

        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Observable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).blockingForEach(extendedAction);
        Observable.<Media, Rating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Observable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).blockingForEach(action);

        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine);
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

        Collection<Observable<Object>> observables = Collections.emptyList();

        Observable<Object> result = Observable.zip(observables, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] args) {
                System.out.println("received: " + args);
                Assert.assertEquals("No argument should have been passed", 0, args.length);
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
        Observable.just(1)
        .zipWith(Observable.just(2), new BiFunction<Integer, Integer, Integer>() {
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
        Observable.just(1)
        .zipWith(Observable.just(2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true, 16)
        .test()
        .assertResult(3);
    }

}
