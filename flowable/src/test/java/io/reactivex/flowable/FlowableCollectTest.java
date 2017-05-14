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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.internal.functions.Functions;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static io.reactivex.common.internal.utils.TestingHelper.addToList;
import static io.reactivex.common.internal.utils.TestingHelper.biConsumerThrows;
import static io.reactivex.common.internal.utils.TestingHelper.callableListCreator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class FlowableCollectTest {

    @Test
    public void testCollectToListFlowable() {
        Flowable<List<Integer>> o = Flowable.just(1, 2, 3)
        .collect(new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() {
                return new ArrayList<Integer>();
            }
        }, new Function2<List<Integer>, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(List<Integer> list, Integer v) {
                list.add(v);
                return Unit.INSTANCE;
            }
        });

        List<Integer> list =  o.blockingLast();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(2, list.get(1).intValue());
        assertEquals(3, list.get(2).intValue());

        // test multiple subscribe
        List<Integer> list2 =  o.blockingLast();

        assertEquals(3, list2.size());
        assertEquals(1, list2.get(0).intValue());
        assertEquals(2, list2.get(1).intValue());
        assertEquals(3, list2.get(2).intValue());
    }

    @Test
    public void testCollectToStringFlowable() {
        String value = Flowable.just(1, 2, 3)
            .collect(
                new Callable<StringBuilder>() {
                    @Override
                    public StringBuilder call() {
                        return new StringBuilder();
                    }
                },
                    new Function2<StringBuilder, Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(StringBuilder sb, Integer v) {
                    if (sb.length() > 0) {
                        sb.append("-");
                    }
                    sb.append(v);
                        return Unit.INSTANCE;
                    }
            }).blockingLast().toString();

        assertEquals("1-2-3", value);
    }


    @Test
    public void testFactoryFailureResultsInErrorEmissionFlowable() {
        final RuntimeException e = new RuntimeException();
        Flowable.just(1).collect(new Callable<List<Integer>>() {

            @Override
            public List<Integer> call() throws Exception {
                throw e;
            }
        }, new Function2<List<Integer>, Integer, kotlin.Unit>() {

            @Override
            public Unit invoke(List<Integer> list, Integer t) {
                list.add(t);
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertNoValues()
        .assertError(e)
        .assertNotComplete();
    }

    @Test
    public void testCollectorFailureDoesNotResultInTwoErrorEmissionsFlowable() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
            RxJavaCommonPlugins.setErrorHandler(addToList(list));
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();

            Burst.items(1).error(e2) //
                    .collect(callableListCreator(), biConsumerThrows(e1))
                    .test() //
                    .assertError(e1) //
                    .assertNotComplete();

            assertEquals(1, list.size());
            assertEquals(e2, list.get(0).getCause());
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void testCollectorFailureDoesNotResultInErrorAndCompletedEmissionsFlowable() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create() //
                .collect(callableListCreator(), biConsumerThrows(e)) //
                .test() //
                .assertError(e) //
                .assertNotComplete();
    }

    @Test
    public void testCollectorFailureDoesNotResultInErrorAndOnNextEmissionsFlowable() {
        final RuntimeException e = new RuntimeException();
        final AtomicBoolean added = new AtomicBoolean();
        Function2<Object, Integer, kotlin.Unit> throwOnFirstOnly = new Function2<Object, Integer, kotlin.Unit>() {

            boolean once = true;

            @Override
            public Unit invoke(Object o, Integer t) {
                if (once) {
                    once = false;
                    throw e;
                } else {
                    added.set(true);
                }
                return Unit.INSTANCE;
            }
        };
        Burst.items(1, 2).create() //
                .collect(callableListCreator(), throwOnFirstOnly)//
                .test() //
                .assertError(e) //
                .assertNoValues() //
                .assertNotComplete();
        assertFalse(added.get());
    }


    @SuppressWarnings("unchecked")
    @Test
    public void collectIntoFlowable() {
        Flowable.just(1, 1, 1, 1, 2)
                .collectInto(new HashSet<Integer>(), new Function2<HashSet<Integer>, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(HashSet<Integer> s, Integer v) {
                s.add(v);
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertResult(new HashSet<Integer>(Arrays.asList(1, 2)));
    }

    @Test
    public void testFactoryFailureResultsInErrorEmission() {
        final RuntimeException e = new RuntimeException();
        Flowable.just(1).collect(new Callable<List<Integer>>() {

            @Override
            public List<Integer> call() throws Exception {
                throw e;
            }
        }, new Function2<List<Integer>, Integer, kotlin.Unit>() {

            @Override
            public Unit invoke(List<Integer> list, Integer t) {
                list.add(t);
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertNoValues()
        .assertError(e)
        .assertNotComplete();
    }

    @Test
    public void testCollectorFailureDoesNotResultInTwoErrorEmissions() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
            RxJavaCommonPlugins.setErrorHandler(addToList(list));
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();

            Burst.items(1).error(e2) //
                    .collect(callableListCreator(), biConsumerThrows(e1)) //
                    .test() //
                    .assertError(e1) //
                    .assertNotComplete();

            assertEquals(1, list.size());
            assertEquals(e2, list.get(0).getCause());
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void testCollectorFailureDoesNotResultInErrorAndCompletedEmissions() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create() //
                .collect(callableListCreator(), biConsumerThrows(e)) //
                .test() //
                .assertError(e) //
                .assertNotComplete();
    }

    @Test
    public void testCollectorFailureDoesNotResultInErrorAndOnNextEmissions() {
        final RuntimeException e = new RuntimeException();
        final AtomicBoolean added = new AtomicBoolean();
        Function2<Object, Integer, kotlin.Unit> throwOnFirstOnly = new Function2<Object, Integer, kotlin.Unit>() {

            boolean once = true;

            @Override
            public Unit invoke(Object o, Integer t) {
                if (once) {
                    once = false;
                    throw e;
                } else {
                    added.set(true);
                }
                return Unit.INSTANCE;
            }
        };
        Burst.items(1, 2).create() //
                .collect(callableListCreator(), throwOnFirstOnly)//
                .test() //
                .assertError(e) //
                .assertNoValues() //
                .assertNotComplete();
        assertFalse(added.get());
    }


    @SuppressWarnings("unchecked")
    @Test
    public void collectInto() {
        Flowable.just(1, 1, 1, 1, 2)
                .collectInto(new HashSet<Integer>(), new Function2<HashSet<Integer>, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(HashSet<Integer> s, Integer v) {
                s.add(v);
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertResult(new HashSet<Integer>(Arrays.asList(1, 2)));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1, 2)
                .collect(Functions.justCallable(new ArrayList<Integer>()), new Function2<ArrayList<Integer>, Integer, kotlin.Unit>() {
                @Override
                public Unit invoke(ArrayList<Integer> a, Integer b) {
                    a.add(b);
                    return Unit.INSTANCE;
                }
            }));

        TestHelper.checkDisposed(Flowable.just(1, 2)
                .collect(Functions.justCallable(new ArrayList<Integer>()), new Function2<ArrayList<Integer>, Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(ArrayList<Integer> a, Integer b) {
                        a.add(b);
                        return Unit.INSTANCE;
                    }
                }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function1<Flowable<Integer>, Flowable<ArrayList<Integer>>>() {
            @Override
            public Flowable<ArrayList<Integer>> invoke(Flowable<Integer> f) {
                return f.collect(Functions.justCallable(new ArrayList<Integer>()),
                        new Function2<ArrayList<Integer>, Integer, kotlin.Unit>() {
                            @Override
                            public Unit invoke(ArrayList<Integer> a, Integer b) {
                                a.add(b);
                                return Unit.INSTANCE;
                            }
                        });
            }
        });
    }
}
