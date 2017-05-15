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
package io.reactivex.observable;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import kotlin.jvm.functions.Function2;
import io.reactivex.observable.ObservableCovarianceTest.CoolRating;
import io.reactivex.observable.ObservableCovarianceTest.ExtendedResult;
import io.reactivex.observable.ObservableCovarianceTest.HorrorMovie;
import io.reactivex.observable.ObservableCovarianceTest.Media;
import io.reactivex.observable.ObservableCovarianceTest.Movie;
import io.reactivex.observable.ObservableCovarianceTest.Rating;
import io.reactivex.observable.ObservableCovarianceTest.Result;
import io.reactivex.observable.subjects.BehaviorSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static io.reactivex.observable.Observable.combineLatest;

public class ObservableCombineLatestTests {
    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void testCovarianceOfCombineLatest() {
        Observable<HorrorMovie> horrors = Observable.just(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.just(new CoolRating());

        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Observable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).blockingForEach(extendedAction);
        Observable.<Media, Rating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Observable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).blockingForEach(action);

        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine);
    }

    Function2<Media, Rating, ExtendedResult> combine = new Function2<Media, Rating, ExtendedResult>() {
        @Override
        public ExtendedResult invoke(Media m, Rating r) {
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
        Observable<Boolean> nullObservable = BehaviorSubject.createDefault((Boolean) null);
        Observable<Boolean> nonNullObservable = BehaviorSubject.createDefault(true);
        Observable<Boolean> combined =
                combineLatest(nullObservable, nonNullObservable, new Function2<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean invoke(Boolean bool1, Boolean bool2) {
                        return bool1 == null ? null : bool2;
                    }
                });
        combined.subscribe(new Function1<Boolean, kotlin.Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
                Assert.assertNull(aBoolean);
                return Unit.INSTANCE;
            }
        });
    }
}
