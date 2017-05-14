/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.flowable.internal.operators;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Arrays;
import java.util.List;

import hu.akarnokd.reactivestreams.extensions.RelaxedSubscriber;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.BiFunction;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableGroupJoinTest {

    Subscriber<Object> observer = TestHelper.mockSubscriber();

    BiFunction<Integer, Integer, Integer> add = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    <T> Function1<Integer, Flowable<T>> just(final Flowable<T> observable) {
        return new Function1<Integer, Flowable<T>>() {
            @Override
            public Flowable<T> invoke(Integer t1) {
                return observable;
            }
        };
    }

    <T, R> Function1<T, Flowable<R>> just2(final Flowable<R> observable) {
        return new Function1<T, Flowable<R>>() {
            @Override
            public Flowable<R> invoke(T t1) {
                return observable;
            }
        };
    }

    BiFunction<Integer, Flowable<Integer>, Flowable<Integer>> add2 = new BiFunction<Integer, Flowable<Integer>, Flowable<Integer>>() {
        @Override
        public Flowable<Integer> apply(final Integer leftValue, Flowable<Integer> rightValues) {
            return rightValues.map(new Function1<Integer, Integer>() {
                @Override
                public Integer invoke(Integer rightValue) {
                    try {
                        return add.apply(leftValue, rightValue);
                    } catch (Exception e) {
                        //TODO checked exceptions
                        if (e instanceof RuntimeException) throw (RuntimeException) e;
                        else throw new RuntimeException(e);
                    }
                }
            });
        }

    };

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void behaveAsJoin() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> m = Flowable.merge(source1.groupJoin(source2,
                just(Flowable.never()),
                just(Flowable.never()), add2));

        m.subscribe(observer);

        source1.onNext(1);
        source1.onNext(2);
        source1.onNext(4);

        source2.onNext(16);
        source2.onNext(32);
        source2.onNext(64);

        source1.onComplete();
        source2.onComplete();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(20);
        verify(observer, times(1)).onNext(33);
        verify(observer, times(1)).onNext(34);
        verify(observer, times(1)).onNext(36);
        verify(observer, times(1)).onNext(65);
        verify(observer, times(1)).onNext(66);
        verify(observer, times(1)).onNext(68);

        verify(observer, times(1)).onComplete(); //Never emitted?
        verify(observer, never()).onError(any(Throwable.class));
    }

    class Person {
        final int id;
        final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    class PersonFruit {
        final int personId;
        final String fruit;

        PersonFruit(int personId, String fruit) {
            this.personId = personId;
            this.fruit = fruit;
        }
    }

    class PPF {
        final Person person;
        final Flowable<PersonFruit> fruits;

        PPF(Person person, Flowable<PersonFruit> fruits) {
            this.person = person;
            this.fruits = fruits;
        }
    }

    @Test
    public void normal1() {
        Flowable<Person> source1 = Flowable.fromIterable(Arrays.asList(
                new Person(1, "Joe"),
                new Person(2, "Mike"),
                new Person(3, "Charlie")
        ));

        Flowable<PersonFruit> source2 = Flowable.fromIterable(Arrays.asList(
                new PersonFruit(1, "Strawberry"),
                new PersonFruit(1, "Apple"),
                new PersonFruit(3, "Peach")
        ));

        Flowable<PPF> q = source1.groupJoin(
                source2,
                just2(Flowable.<Object>never()),
                just2(Flowable.<Object>never()),
                new BiFunction<Person, Flowable<PersonFruit>, PPF>() {
                    @Override
                    public PPF apply(Person t1, Flowable<PersonFruit> t2) {
                        return new PPF(t1, t2);
                    }
                });

        q.subscribe(
                new RelaxedSubscriber<PPF>() {
                    @Override
                    public void onNext(final PPF ppf) {
                        ppf.fruits.filter(new Function1<PersonFruit, Boolean>() {
                            @Override
                            public Boolean invoke(PersonFruit t1) {
                                return ppf.person.id == t1.personId;
                            }
                        }).subscribe(new Function1<PersonFruit, kotlin.Unit>() {
                            @Override
                            public Unit invoke(PersonFruit t1) {
                                observer.onNext(Arrays.asList(ppf.person.name, t1.fruit));
                                return Unit.INSTANCE;
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                }
        );

        verify(observer, times(1)).onNext(Arrays.asList("Joe", "Strawberry"));
        verify(observer, times(1)).onNext(Arrays.asList("Joe", "Apple"));
        verify(observer, times(1)).onNext(Arrays.asList("Charlie", "Peach"));

        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void leftThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Flowable<Integer>> m = source1.groupJoin(source2,
                just(Flowable.never()),
                just(Flowable.never()), add2);

        m.subscribe(observer);

        source2.onNext(1);
        source1.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Flowable<Integer>> m = source1.groupJoin(source2,
                just(Flowable.never()),
                just(Flowable.never()), add2);

        m.subscribe(observer);

        source1.onNext(1);
        source2.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onNext(any(Flowable.class));
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void leftDurationThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> duration1 = Flowable.<Integer>error(new RuntimeException("Forced failure"));

        Flowable<Flowable<Integer>> m = source1.groupJoin(source2,
                just(duration1),
                just(Flowable.never()), add2);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Flowable<Integer> duration1 = Flowable.<Integer>error(new RuntimeException("Forced failure"));

        Flowable<Flowable<Integer>> m = source1.groupJoin(source2,
                just(Flowable.never()),
                just(duration1), add2);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void leftDurationSelectorThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Function1<Integer, Flowable<Integer>> fail = new Function1<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Flowable<Flowable<Integer>> m = source1.groupJoin(source2,
                fail,
                just(Flowable.never()), add2);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationSelectorThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        Function1<Integer, Flowable<Integer>> fail = new Function1<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Flowable<Flowable<Integer>> m = source1.groupJoin(source2,
                just(Flowable.never()),
                fail, add2);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void resultSelectorThrows() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();

        BiFunction<Integer, Flowable<Integer>, Integer> fail = new BiFunction<Integer, Flowable<Integer>, Integer>() {
            @Override
            public Integer apply(Integer t1, Flowable<Integer> t2) {
                throw new RuntimeException("Forced failure");
            }
        };

        Flowable<Integer> m = source1.groupJoin(source2,
                just(Flowable.never()),
                just(Flowable.never()), fail);
        m.subscribe(observer);

        source1.onNext(1);
        source2.onNext(2);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).groupJoin(
                Flowable.just(2),
                new Function1<Integer, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> invoke(Integer left) {
                        return Flowable.never();
                    }
                },
                new Function1<Integer, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> invoke(Integer right) {
                        return Flowable.never();
                    }
                },
                new BiFunction<Integer, Flowable<Integer>, Object>() {
                    @Override
                    public Object apply(Integer r, Flowable<Integer> l) throws Exception {
                        return l;
                    }
                }
        ));
    }

    @Test
    public void innerCompleteLeft() {
        Flowable.just(1)
                .groupJoin(
                        Flowable.just(2),
                        new Function1<Integer, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Integer left) {
                                return Flowable.empty();
                            }
                        },
                        new Function1<Integer, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Integer right) {
                                return Flowable.never();
                            }
                        },
                        new BiFunction<Integer, Flowable<Integer>, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Integer r, Flowable<Integer> l) throws Exception {
                                return l;
                            }
                        }
                )
                .flatMap(Functions.<Flowable<Integer>>identity())
                .test()
                .assertResult();
    }

    @Test
    public void innerErrorLeft() {
        Flowable.just(1)
                .groupJoin(
                        Flowable.just(2),
                        new Function1<Integer, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Integer left) {
                                return Flowable.error(new TestException());
                            }
                        },
                        new Function1<Integer, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Integer right) {
                                return Flowable.never();
                            }
                        },
                        new BiFunction<Integer, Flowable<Integer>, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Integer r, Flowable<Integer> l) throws Exception {
                                return l;
                            }
                        }
                )
                .flatMap(Functions.<Flowable<Integer>>identity())
                .test()
                .assertFailure(TestException.class);
    }

    @Test
    public void innerCompleteRight() {
        Flowable.just(1)
                .groupJoin(
                        Flowable.just(2),
                        new Function1<Integer, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Integer left) {
                                return Flowable.never();
                            }
                        },
                        new Function1<Integer, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Integer right) {
                                return Flowable.empty();
                            }
                        },
                        new BiFunction<Integer, Flowable<Integer>, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Integer r, Flowable<Integer> l) throws Exception {
                                return l;
                            }
                        }
                )
                .flatMap(Functions.<Flowable<Integer>>identity())
                .test()
                .assertResult(2);
    }

    @Test
    public void innerErrorRight() {
        Flowable.just(1)
                .groupJoin(
                        Flowable.just(2),
                        new Function1<Integer, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Integer left) {
                                return Flowable.never();
                            }
                        },
                        new Function1<Integer, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Integer right) {
                                return Flowable.error(new TestException());
                            }
                        },
                        new BiFunction<Integer, Flowable<Integer>, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Integer r, Flowable<Integer> l) throws Exception {
                                return l;
                            }
                        }
                )
                .flatMap(Functions.<Flowable<Integer>>identity())
                .test()
                .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Object> ps1 = PublishProcessor.create();
            final PublishProcessor<Object> ps2 = PublishProcessor.create();

            List<Throwable> errors = TestCommonHelper.trackPluginErrors();

            try {
                TestSubscriber<Flowable<Integer>> to = Flowable.just(1)
                        .groupJoin(
                                Flowable.just(2).concatWith(Flowable.<Integer>never()),
                                new Function1<Integer, Flowable<Object>>() {
                                    @Override
                                    public Flowable<Object> invoke(Integer left) {
                                        return ps1;
                                    }
                                },
                                new Function1<Integer, Flowable<Object>>() {
                                    @Override
                                    public Flowable<Object> invoke(Integer right) {
                                        return ps2;
                                    }
                                },
                                new BiFunction<Integer, Flowable<Integer>, Flowable<Integer>>() {
                                    @Override
                                    public Flowable<Integer> apply(Integer r, Flowable<Integer> l) throws Exception {
                                        return l;
                                    }
                                }
                        )
                        .test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                to.assertError(Throwable.class).assertSubscribed().assertNotComplete().assertValueCount(1);

                Throwable exc = to.errors().get(0);

                if (exc instanceof CompositeException) {
                    List<Throwable> es = TestCommonHelper.compositeList(exc);
                    TestCommonHelper.assertError(es, 0, TestException.class);
                    TestCommonHelper.assertError(es, 1, TestException.class);
                } else {
                    to.assertError(TestException.class);
                }

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @Test
    public void outerErrorRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Object> ps1 = PublishProcessor.create();
            final PublishProcessor<Object> ps2 = PublishProcessor.create();

            List<Throwable> errors = TestCommonHelper.trackPluginErrors();

            try {
                TestSubscriber<Object> to = ps1
                        .groupJoin(
                                ps2,
                                new Function1<Object, Flowable<Object>>() {
                                    @Override
                                    public Flowable<Object> invoke(Object left) {
                                        return Flowable.never();
                                    }
                                },
                                new Function1<Object, Flowable<Object>>() {
                                    @Override
                                    public Flowable<Object> invoke(Object right) {
                                        return Flowable.never();
                                    }
                                },
                                new BiFunction<Object, Flowable<Object>, Flowable<Object>>() {
                                    @Override
                                    public Flowable<Object> apply(Object r, Flowable<Object> l) throws Exception {
                                        return l;
                                    }
                                }
                        )
                        .flatMap(Functions.<Flowable<Object>>identity())
                        .test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                to.assertError(Throwable.class).assertSubscribed().assertNotComplete().assertNoValues();

                Throwable exc = to.errors().get(0);

                if (exc instanceof CompositeException) {
                    List<Throwable> es = TestCommonHelper.compositeList(exc);
                    TestCommonHelper.assertError(es, 0, TestException.class);
                    TestCommonHelper.assertError(es, 1, TestException.class);
                } else {
                    to.assertError(TestException.class);
                }

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @Test
    public void rightEmission() {
        final PublishProcessor<Object> ps1 = PublishProcessor.create();
        final PublishProcessor<Object> ps2 = PublishProcessor.create();

        TestSubscriber<Object> to = ps1
                .groupJoin(
                        ps2,
                        new Function1<Object, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Object left) {
                                return Flowable.never();
                            }
                        },
                        new Function1<Object, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> invoke(Object right) {
                                return Flowable.never();
                            }
                        },
                        new BiFunction<Object, Flowable<Object>, Flowable<Object>>() {
                            @Override
                            public Flowable<Object> apply(Object r, Flowable<Object> l) throws Exception {
                                return l;
                            }
                        }
                )
                .flatMap(Functions.<Flowable<Object>>identity())
                .test();

        ps2.onNext(2);

        ps1.onNext(1);
        ps1.onComplete();

        ps2.onComplete();

        to.assertResult(2);
    }
}
