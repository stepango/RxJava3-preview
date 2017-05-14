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

package io.reactivex.observable.internal.operators;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static io.reactivex.common.internal.utils.TestingHelper.addToList;
import static io.reactivex.common.internal.utils.TestingHelper.biConsumerThrows;
import static io.reactivex.common.internal.utils.TestingHelper.callableListCreator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class ObservableCollectTest {

    @Test
    public void testCollectToListObservable() {
        Observable<List<Integer>> o = Observable.just(1, 2, 3)
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
        }).toObservable();

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
    public void testCollectToStringObservable() {
        String value = Observable.just(1, 2, 3).collect(new Callable<StringBuilder>() {
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
            }).toObservable().blockingLast().toString();

        assertEquals("1-2-3", value);
    }

    @Test
    public void testCollectorFailureDoesNotResultInTwoErrorEmissionsObservable() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
            RxJavaCommonPlugins.setErrorHandler(addToList(list));
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();

            Burst.items(1).error(e2) //
                    .collect(callableListCreator(), biConsumerThrows(e1)) //
                    .toObservable()
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
    public void testCollectorFailureDoesNotResultInErrorAndCompletedEmissionsObservable() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create() //
                .collect(callableListCreator(), biConsumerThrows(e)) //
                .toObservable()
                .test() //
                .assertError(e) //
                .assertNotComplete();
    }

    @Test
    public void testCollectorFailureDoesNotResultInErrorAndOnNextEmissionsObservable() {
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
    public void collectIntoObservable() {
        Observable.just(1, 1, 1, 1, 2)
                .collectInto(new HashSet<Integer>(), new Function2<HashSet<Integer>, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(HashSet<Integer> s, Integer v) {
                s.add(v);
                return Unit.INSTANCE;
            }
        }).toObservable()
        .test()
        .assertResult(new HashSet<Integer>(Arrays.asList(1, 2)));
    }

    @Test
    public void testCollectToList() {
        Single<List<Integer>> o = Observable.just(1, 2, 3)
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

        List<Integer> list =  o.blockingGet();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(2, list.get(1).intValue());
        assertEquals(3, list.get(2).intValue());

        // test multiple subscribe
        List<Integer> list2 =  o.blockingGet();

        assertEquals(3, list2.size());
        assertEquals(1, list2.get(0).intValue());
        assertEquals(2, list2.get(1).intValue());
        assertEquals(3, list2.get(2).intValue());
    }

    @Test
    public void testCollectToString() {
        String value = Observable.just(1, 2, 3).collect(new Callable<StringBuilder>() {
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
            }).blockingGet().toString();

        assertEquals("1-2-3", value);
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
        Observable.just(1, 1, 1, 1, 2)
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
        TestHelper.checkDisposed(Observable.range(1, 3).collect(new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                return new ArrayList<Integer>();
            }
        }, new Function2<List<Integer>, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(List<Integer> a, Integer b) {
                a.add(b);
                return Unit.INSTANCE;
            }
        }));

        TestHelper.checkDisposed(Observable.range(1, 3).collect(new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                return new ArrayList<Integer>();
            }
        }, new Function2<List<Integer>, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(List<Integer> a, Integer b) {
                a.add(b);
                return Unit.INSTANCE;
            }
        }).toObservable());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function1<Observable<Integer>, SingleSource<List<Integer>>>() {
            @Override
            public SingleSource<List<Integer>> invoke(Observable<Integer> o) {
                return o.collect(new Callable<List<Integer>>() {
                    @Override
                    public List<Integer> call() throws Exception {
                        return new ArrayList<Integer>();
                    }
                }, new Function2<List<Integer>, Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(List<Integer> a, Integer b) {
                        a.add(b);
                        return Unit.INSTANCE;
                    }
                });
            }
        });

        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Integer>, ObservableSource<List<Integer>>>() {
            @Override
            public ObservableSource<List<Integer>> invoke(Observable<Integer> o) {
                return o.collect(new Callable<List<Integer>>() {
                    @Override
                    public List<Integer> call() throws Exception {
                        return new ArrayList<Integer>();
                    }
                }, new Function2<List<Integer>, Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(List<Integer> a, Integer b) {
                        a.add(b);
                        return Unit.INSTANCE;
                    }
                }).toObservable();
            }
        });
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Integer>, Object>() {
            @Override
            public Object invoke(Observable<Integer> o) {
                return o.collect(new Callable<List<Integer>>() {
                    @Override
                    public List<Integer> call() throws Exception {
                        return new ArrayList<Integer>();
                    }
                }, new Function2<List<Integer>, Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(List<Integer> a, Integer b) {
                        a.add(b);
                        return Unit.INSTANCE;
                    }
                }).toObservable();
            }
        }, false, 1, 2, Arrays.asList(1));
    }
}
