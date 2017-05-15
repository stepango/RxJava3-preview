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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParallelReduceTest {

    @Test
    public void subscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .reduce(new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                return new ArrayList<Integer>();
            }
        }, new Function2<List<Integer>, Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(List<Integer> a, Integer b) {
                a.add(b);
                return a;
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void initialCrash() {
        Flowable.range(1, 5)
        .parallel()
        .reduce(new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                throw new TestException();
            }
        }, new Function2<List<Integer>, Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(List<Integer> a, Integer b) {
                a.add(b);
                return a;
            }
        })
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void reducerCrash() {
        Flowable.range(1, 5)
        .parallel()
        .reduce(new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                return new ArrayList<Integer>();
            }
        }, new Function2<List<Integer>, Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(List<Integer> a, Integer b) {
                if (b == 3) {
                    throw new TestException();
                }
                a.add(b);
                return a;
            }
        })
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void cancel() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<List<Integer>> ts = pp
        .parallel()
        .reduce(new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                return new ArrayList<Integer>();
            }
        }, new Function2<List<Integer>, Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(List<Integer> a, Integer b) {
                a.add(b);
                return a;
            }
        })
        .sequential()
        .test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void error() {
        Flowable.<Integer>error(new TestException())
        .parallel()
        .reduce(new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                return new ArrayList<Integer>();
            }
        }, new Function2<List<Integer>, Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(List<Integer> a, Integer b) {
                a.add(b);
                return a;
            }
        })
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doubleError() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .reduce(new Callable<List<Object>>() {
                @Override
                public List<Object> call() throws Exception {
                    return new ArrayList<Object>();
                }
            }, new Function2<List<Object>, Object, List<Object>>() {
                @Override
                public List<Object> invoke(List<Object> a, Object b) {
                    a.add(b);
                    return a;
                }
            })
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
}
