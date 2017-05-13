/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.flowable;

import org.junit.Ignore;
import org.junit.Test;

import io.reactivex.common.functions.BiFunction;
import io.reactivex.flowable.FlowableCovarianceTest.CoolRating;
import io.reactivex.flowable.FlowableCovarianceTest.ExtendedResult;
import io.reactivex.flowable.FlowableCovarianceTest.HorrorMovie;
import io.reactivex.flowable.FlowableCovarianceTest.Media;
import io.reactivex.flowable.FlowableCovarianceTest.Movie;
import io.reactivex.flowable.FlowableCovarianceTest.Rating;
import io.reactivex.flowable.FlowableCovarianceTest.Result;
import io.reactivex.flowable.processors.BehaviorProcessor;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static io.reactivex.flowable.Flowable.combineLatest;
import static org.junit.Assert.assertNull;

public class FlowableCombineLatestTests {
    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void testCovarianceOfCombineLatest() {
        Flowable<HorrorMovie> horrors = Flowable.just(new HorrorMovie());
        Flowable<CoolRating> ratings = Flowable.just(new CoolRating());

        Flowable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).blockingForEach(extendedAction);
        Flowable.<Media, Rating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).blockingForEach(action);

        Flowable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine);
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

    @Ignore("No longer allowed")
    @Test
    public void testNullEmitting() throws Exception {
        // FIXME this is no longer allowed
        Flowable<Boolean> nullObservable = BehaviorProcessor.createDefault((Boolean) null);
        Flowable<Boolean> nonNullObservable = BehaviorProcessor.createDefault(true);
        Flowable<Boolean> combined =
                combineLatest(nullObservable, nonNullObservable, new BiFunction<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean bool1, Boolean bool2) {
                        return bool1 == null ? null : bool2;
                    }
                });
        combined.subscribe(new Function1<Boolean, kotlin.Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
                assertNull(aBoolean);
                return Unit.INSTANCE;
            }
        });
    }
}
