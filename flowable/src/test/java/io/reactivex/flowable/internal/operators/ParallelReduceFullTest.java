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

import java.io.IOException;
import java.util.List;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.ParallelFlowable;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParallelReduceFullTest {

    @Test
    public void cancel() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .parallel()
        .reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return a + b;
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void error() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Flowable.<Integer>error(new TestException())
            .parallel()
            .reduce(new Function2<Integer, Integer, Integer>() {
                @Override
                public Integer invoke(Integer a, Integer b) {
                    return a + b;
                }
            })
            .test()
            .assertFailure(TestException.class);

            assertTrue(errors.isEmpty());
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void error2() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            ParallelFlowable.fromArray(Flowable.<Integer>error(new IOException()), Flowable.<Integer>error(new TestException()))
            .reduce(new Function2<Integer, Integer, Integer>() {
                @Override
                public Integer invoke(Integer a, Integer b) {
                    return a + b;
                }
            })
            .test()
            .assertFailure(IOException.class);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .parallel()
        .reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return a + b;
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .reduce(new Function2<Object, Object, Object>() {
                @Override
                public Object invoke(Object a, Object b) {
                    return "" + a + b;
                }
            })
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
    public void reducerCrash() {
        Flowable.range(1, 4)
        .parallel(2)
        .reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                if (b == 3) {
                    throw new TestException();
                }
                return a + b;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void reducerCrash2() {
        Flowable.range(1, 4)
        .parallel(2)
        .reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                if (a == 1 + 3) {
                    throw new TestException();
                }
                return a + b;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
}
