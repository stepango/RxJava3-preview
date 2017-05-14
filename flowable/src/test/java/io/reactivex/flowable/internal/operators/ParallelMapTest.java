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

import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParallelMapTest {

    @Test
    public void subscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .map(Functions.identity()));
    }

    @Test
    public void doubleFilter() {
        Flowable.range(1, 10)
        .parallel()
        .map(Functions.<Integer>identity())
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 3 == 0;
            }
        })
        .sequential()
        .test()
        .assertResult(6);
    }

    @Test
    public void doubleFilterAsync() {
        Flowable.range(1, 10)
        .parallel()
        .runOn(Schedulers.computation())
        .map(Functions.<Integer>identity())
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 2 == 0;
            }
        })
                .filter(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return v % 3 == 0;
            }
        })
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(6);
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .map(Functions.<Object>identity())
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertFalse(errors.isEmpty());
            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void doubleError2() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .map(Functions.<Object>identity())
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertFalse(errors.isEmpty());
            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .parallel()
        .map(Functions.<Object>identity())
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapCrash() {
        Flowable.just(1)
        .parallel()
                .map(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                throw new TestException();
            }
        })
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapCrashConditional() {
        Flowable.just(1)
        .parallel()
                .map(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapCrashConditional2() {
        Flowable.just(1)
        .parallel()
        .runOn(Schedulers.computation())
                .map(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }
}
