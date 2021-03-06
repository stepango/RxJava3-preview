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

import kotlin.jvm.functions.Function2;
import io.reactivex.flowable.FlowableCovarianceTest.HorrorMovie;
import io.reactivex.flowable.FlowableCovarianceTest.Movie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FlowableReduceTests {

    @Test
    public void reduceIntsFlowable() {
        Flowable<Integer> o = Flowable.just(1, 2, 3);
        int value = o.reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer t1, Integer t2) {
                return t1 + t2;
            }
        }).blockingSingle();

        assertEquals(6, value);
    }

    @SuppressWarnings("unused")
    @Test
    public void reduceWithObjectsFlowable() {
        Flowable<Movie> horrorMovies = Flowable.<Movie> just(new HorrorMovie());

        Flowable<Movie> reduceResult = horrorMovies.scan(new Function2<Movie, Movie, Movie>() {
            @Override
            public Movie invoke(Movie t1, Movie t2) {
                return t2;
            }
        }).takeLast(1);

        Flowable<Movie> reduceResult2 = horrorMovies.reduce(new Function2<Movie, Movie, Movie>() {
            @Override
            public Movie invoke(Movie t1, Movie t2) {
                return t2;
            }
        });

        assertNotNull(reduceResult2);
    }

    /**
     * Reduce consumes and produces T so can't do covariance.
     *
     * https://github.com/ReactiveX/RxJava/issues/360#issuecomment-24203016
     */
    @Test
    public void reduceWithCovariantObjectsFlowable() {
        Flowable<Movie> horrorMovies = Flowable.<Movie> just(new HorrorMovie());

        Flowable<Movie> reduceResult2 = horrorMovies.reduce(new Function2<Movie, Movie, Movie>() {
            @Override
            public Movie invoke(Movie t1, Movie t2) {
                return t2;
            }
        });

        assertNotNull(reduceResult2);
    }


    @Test
    public void reduceInts() {
        Flowable<Integer> o = Flowable.just(1, 2, 3);
        int value = o.reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer t1, Integer t2) {
                return t1 + t2;
            }
        }).blockingSingle();

        assertEquals(6, value);
    }

    @SuppressWarnings("unused")
    @Test
    public void reduceWithObjects() {
        Flowable<Movie> horrorMovies = Flowable.<Movie> just(new HorrorMovie());

        Flowable<Movie> reduceResult = horrorMovies.scan(new Function2<Movie, Movie, Movie>() {
            @Override
            public Movie invoke(Movie t1, Movie t2) {
                return t2;
            }
        }).takeLast(1);

        Flowable<Movie> reduceResult2 = horrorMovies.reduce(new Function2<Movie, Movie, Movie>() {
            @Override
            public Movie invoke(Movie t1, Movie t2) {
                return t2;
            }
        });

        assertNotNull(reduceResult2);
    }

    /**
     * Reduce consumes and produces T so can't do covariance.
     *
     * https://github.com/ReactiveX/RxJava/issues/360#issuecomment-24203016
     */
    @Test
    public void reduceWithCovariantObjects() {
        Flowable<Movie> horrorMovies = Flowable.<Movie> just(new HorrorMovie());

        Flowable<Movie> reduceResult2 = horrorMovies.reduce(new Function2<Movie, Movie, Movie>() {
            @Override
            public Movie invoke(Movie t1, Movie t2) {
                return t2;
            }
        });

        assertNotNull(reduceResult2);
    }

    /**
     * Reduce consumes and produces T so can't do covariance.
     *
     * https://github.com/ReactiveX/RxJava/issues/360#issuecomment-24203016
     */
    @Test
    public void reduceCovariance() {
        // must type it to <Movie>
        Flowable<Movie> horrorMovies = Flowable.<Movie> just(new HorrorMovie());
        libraryFunctionActingOnMovieObservables(horrorMovies);
    }

    /*
     * This accepts <Movie> instead of <? super Movie> since `reduce` can't handle covariants
     */
    public void libraryFunctionActingOnMovieObservables(Flowable<Movie> obs) {

        obs.reduce(new Function2<Movie, Movie, Movie>() {
            @Override
            public Movie invoke(Movie t1, Movie t2) {
                return t2;
            }
        });
    }

}
