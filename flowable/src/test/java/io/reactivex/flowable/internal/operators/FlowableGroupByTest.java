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
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import hu.akarnokd.reactivestreams.extensions.FusedQueueSubscription;
import io.reactivex.common.Notification;
import io.reactivex.common.Schedulers;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.GroupedFlowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.DefaultSubscriber;
import io.reactivex.flowable.subscribers.SubscriberFusion;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class FlowableGroupByTest {

    final Function1<String, Integer> length = new Function1<String, Integer>() {
        @Override
        public Integer invoke(String s) {
            return s.length();
        }
    };

    @Test
    public void testGroupBy() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five", "six");
        Flowable<GroupedFlowable<Integer, String>> grouped = source.groupBy(length);

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList("one", "two", "six").toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList("four", "five").toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList("three").toArray(), map.get(5).toArray());
    }

    @Test
    public void testGroupByWithElementSelector() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five", "six");
        Flowable<GroupedFlowable<Integer, Integer>> grouped = source.groupBy(length, length);

        Map<Integer, Collection<Integer>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList(3, 3, 3).toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList(4, 4).toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList(5).toArray(), map.get(5).toArray());
    }

    @Test
    public void testGroupByWithElementSelector2() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five", "six");
        Flowable<GroupedFlowable<Integer, Integer>> grouped = source.groupBy(length, length);

        Map<Integer, Collection<Integer>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList(3, 3, 3).toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList(4, 4).toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList(5).toArray(), map.get(5).toArray());
    }

    @Test
    public void testEmpty() {
        Flowable<String> source = Flowable.empty();
        Flowable<GroupedFlowable<Integer, String>> grouped = source.groupBy(length);

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertTrue(map.isEmpty());
    }

    @Test
    public void testError() {
        Flowable<String> sourceStrings = Flowable.just("one", "two", "three", "four", "five", "six");
        Flowable<String> errorSource = Flowable.error(new RuntimeException("forced failure"));
        Flowable<String> source = Flowable.concat(sourceStrings, errorSource);

        Flowable<GroupedFlowable<Integer, String>> grouped = source.groupBy(length);

        final AtomicInteger groupCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        grouped.flatMap(new Function1<GroupedFlowable<Integer, String>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(final GroupedFlowable<Integer, String> o) {
                groupCounter.incrementAndGet();
                return o.map(new Function1<String, String>() {

                    @Override
                    public String invoke(String v) {
                        return "Event => key: " + o.getKey() + " value: " + v;
                    }
                });
            }
        }).subscribe(new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                error.set(e);
            }

            @Override
            public void onNext(String v) {
                eventCounter.incrementAndGet();
                System.out.println(v);

            }
        });

        assertEquals(3, groupCounter.get());
        assertEquals(6, eventCounter.get());
        assertNotNull(error.get());
    }

    private static <K, V> Map<K, Collection<V>> toMap(Flowable<GroupedFlowable<K, V>> observable) {

        final ConcurrentHashMap<K, Collection<V>> result = new ConcurrentHashMap<K, Collection<V>>();

        observable.blockingForEach(new Function1<GroupedFlowable<K, V>, Unit>() {

            @Override
            public Unit invoke(final GroupedFlowable<K, V> o) {
                result.put(o.getKey(), new ConcurrentLinkedQueue<V>());
                o.subscribe(new Function1<V, kotlin.Unit>() {

                    @Override
                    public Unit invoke(V v) {
                        result.get(o.getKey()).add(v);
                        return Unit.INSTANCE;
                    }

                });
                return Unit.INSTANCE;
            }
        });

        return result;
    }

    /**
     * Assert that only a single subscription to a stream occurs and that all events are received.
     *
     * @throws Throwable some method call is declared throws
     */
    @Test
    public void testGroupedEventStream() throws Throwable {

        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger groupCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        final int count = 100;
        final int groupCount = 2;

        Flowable<Event> es = Flowable.unsafeCreate(new Publisher<Event>() {

            @Override
            public void subscribe(final Subscriber<? super Event> observer) {
                observer.onSubscribe(new BooleanSubscription());
                System.out.println("*** Subscribing to EventStream ***");
                subscribeCounter.incrementAndGet();
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        for (int i = 0; i < count; i++) {
                            Event e = new Event();
                            e.source = i % groupCount;
                            e.message = "Event-" + i;
                            observer.onNext(e);
                        }
                        observer.onComplete();
                    }

                }).start();
            }

        });

        es.groupBy(new Function1<Event, Integer>() {

            @Override
            public Integer invoke(Event e) {
                return e.source;
            }
        }).flatMap(new Function1<GroupedFlowable<Integer, Event>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(GroupedFlowable<Integer, Event> eventGroupedFlowable) {
                System.out.println("GroupedFlowable Key: " + eventGroupedFlowable.getKey());
                groupCounter.incrementAndGet();

                return eventGroupedFlowable.map(new Function1<Event, String>() {

                    @Override
                    public String invoke(Event event) {
                        return "Source: " + event.source + "  Message: " + event.message;
                    }
                });

            }
        }).subscribe(new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onNext(String outputMessage) {
                System.out.println(outputMessage);
                eventCounter.incrementAndGet();
            }
        });

        latch.await(5000, TimeUnit.MILLISECONDS);
        assertEquals(1, subscribeCounter.get());
        assertEquals(groupCount, groupCounter.get());
        assertEquals(count, eventCounter.get());

    }

    /*
     * We will only take 1 group with 20 events from it and then unsubscribe.
     */
    @Test
    public void testUnsubscribeOnNestedTakeAndSyncInfiniteStream() throws InterruptedException {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(SYNC_INFINITE_OBSERVABLE_OF_EVENT(2, subscribeCounter, sentEventCounter), subscribeCounter);
        Thread.sleep(500);
        assertEquals(39, sentEventCounter.get());
    }

    /*
     * We will only take 1 group with 20 events from it and then unsubscribe.
     */
    @Test
    public void testUnsubscribeOnNestedTakeAndAsyncInfiniteStream() throws InterruptedException {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(ASYNC_INFINITE_OBSERVABLE_OF_EVENT(2, subscribeCounter, sentEventCounter), subscribeCounter);
        Thread.sleep(500);
        assertEquals(39, sentEventCounter.get());
    }

    private void doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(Flowable<Event> es, AtomicInteger subscribeCounter) throws InterruptedException {
        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicInteger groupCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        es.groupBy(new Function1<Event, Integer>() {

            @Override
            public Integer invoke(Event e) {
                return e.source;
            }
        })
                .take(1) // we want only the first group
                .flatMap(new Function1<GroupedFlowable<Integer, Event>, Flowable<String>>() {

                    @Override
                    public Flowable<String> invoke(GroupedFlowable<Integer, Event> eventGroupedFlowable) {
                        System.out.println("testUnsubscribe => GroupedFlowable Key: " + eventGroupedFlowable.getKey());
                        groupCounter.incrementAndGet();

                        return eventGroupedFlowable
                                .take(20) // limit to only 20 events on this group
                                .map(new Function1<Event, String>() {

                                    @Override
                                    public String invoke(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                }).subscribe(new DefaultSubscriber<String>() {

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void onNext(String outputMessage) {
                        System.out.println(outputMessage);
                        eventCounter.incrementAndGet();
                    }
                });

        if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
            fail("timed out so likely did not unsubscribe correctly");
        }
        assertEquals(1, subscribeCounter.get());
        assertEquals(1, groupCounter.get());
        assertEquals(20, eventCounter.get());
        // sentEvents will go until 'eventCounter' hits 20 and then unsubscribes
        // which means it will also send (but ignore) the 19/20 events for the other group
        // It will not however send all 100 events.
    }

    @Test
    public void testUnsubscribeViaTakeOnGroupThenMergeAndTake() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(new Function1<Event, Integer>() {

                    @Override
                    public Integer invoke(Event e) {
                        return e.source;
                    }
                })
                // take 2 of the 4 groups
                .take(2)
                .flatMap(new Function1<GroupedFlowable<Integer, Event>, Flowable<String>>() {

                    @Override
                    public Flowable<String> invoke(GroupedFlowable<Integer, Event> eventGroupedFlowable) {
                        return eventGroupedFlowable
                                .map(new Function1<Event, String>() {

                                    @Override
                                    public String invoke(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .take(30).subscribe(new Function1<String, kotlin.Unit>() {

                    @Override
                    public Unit invoke(String s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                        return Unit.INSTANCE;
                    }

                });

        assertEquals(30, eventCounter.get());
        // we should send 28 additional events that are filtered out as they are in the groups we skip
        assertEquals(58, sentEventCounter.get());
    }

    @Test
    public void testUnsubscribeViaTakeOnGroupThenTakeOnInner() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(new Function1<Event, Integer>() {

                    @Override
                    public Integer invoke(Event e) {
                        return e.source;
                    }
                })
                // take 2 of the 4 groups
                .take(2)
                .flatMap(new Function1<GroupedFlowable<Integer, Event>, Flowable<String>>() {

                    @Override
                    public Flowable<String> invoke(GroupedFlowable<Integer, Event> eventGroupedFlowable) {
                        int numToTake = 0;
                        if (eventGroupedFlowable.getKey() == 1) {
                            numToTake = 10;
                        } else if (eventGroupedFlowable.getKey() == 2) {
                            numToTake = 5;
                        }
                        return eventGroupedFlowable
                                .take(numToTake)
                                .map(new Function1<Event, String>() {

                                    @Override
                                    public String invoke(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .subscribe(new Function1<String, kotlin.Unit>() {

                    @Override
                    public Unit invoke(String s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                        return Unit.INSTANCE;
                    }

                });

        assertEquals(15, eventCounter.get());
        // we should send 22 additional events that are filtered out as they are skipped while taking the 15 we want
        assertEquals(37, sentEventCounter.get());
    }

    @Test
    public void testStaggeredCompletion() throws InterruptedException {
        final AtomicInteger eventCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        Flowable.range(0, 100)
                .groupBy(new Function1<Integer, Integer>() {

                    @Override
                    public Integer invoke(Integer i) {
                        return i % 2;
                    }
                })
                .flatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<Integer>>() {

                    @Override
                    public Flowable<Integer> invoke(GroupedFlowable<Integer, Integer> group) {
                        if (group.getKey() == 0) {
                            return group.delay(100, TimeUnit.MILLISECONDS).map(new Function1<Integer, Integer>() {
                                @Override
                                public Integer invoke(Integer t) {
                                    return t * 10;
                                }

                            });
                        } else {
                            return group;
                        }
                    }
                })
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onComplete() {
                        System.out.println("=> onComplete");
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void onNext(Integer s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                    }
                });

        if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        assertEquals(100, eventCounter.get());
    }

    @Test(timeout = 1000)
    public void testCompletionIfInnerNotSubscribed() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger eventCounter = new AtomicInteger();
        Flowable.range(0, 100)
                .groupBy(new Function1<Integer, Integer>() {

                    @Override
                    public Integer invoke(Integer i) {
                        return i % 2;
                    }
                })
                .subscribe(new DefaultSubscriber<GroupedFlowable<Integer, Integer>>() {

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void onNext(GroupedFlowable<Integer, Integer> s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                    }
                });
        if (!latch.await(500, TimeUnit.MILLISECONDS)) {
            fail("timed out - never got completion");
        }
        assertEquals(2, eventCounter.get());
    }

    @Test
    public void testIgnoringGroups() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(new Function1<Event, Integer>() {

                    @Override
                    public Integer invoke(Event e) {
                        return e.source;
                    }
                })
                .flatMap(new Function1<GroupedFlowable<Integer, Event>, Flowable<String>>() {

                    @Override
                    public Flowable<String> invoke(GroupedFlowable<Integer, Event> eventGroupedFlowable) {
                        Flowable<Event> eventStream = eventGroupedFlowable;
                        if (eventGroupedFlowable.getKey() >= 2) {
                            // filter these
                            eventStream = eventGroupedFlowable.filter(new kotlin.jvm.functions.Function1<Event, Boolean>() {
                                @Override
                                public Boolean invoke(Event t1) {
                                    return false;
                                }
                            });
                        }

                        return eventStream
                                .map(new Function1<Event, String>() {

                                    @Override
                                    public String invoke(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .take(30).subscribe(new Function1<String, kotlin.Unit>() {

                    @Override
                    public Unit invoke(String s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                        return Unit.INSTANCE;
                    }

                });

        assertEquals(30, eventCounter.get());
        // we should send 30 additional events that are filtered out as they are in the groups we skip
        assertEquals(60, sentEventCounter.get());
    }

    @Test
    public void testFirstGroupsCompleteAndParentSlowToThenEmitFinalGroupsAndThenComplete() throws InterruptedException {
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<String>();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                sub.onSubscribe(new BooleanSubscription());
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                try {
                    first.await();
                } catch (InterruptedException e) {
                    sub.onError(e);
                    return;
                }
                sub.onNext(3);
                sub.onNext(3);
                sub.onComplete();
            }

        }).groupBy(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t) {
                return t;
            }

        }).flatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(final GroupedFlowable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function1<Integer, String>() {

                        @Override
                        public String invoke(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Function0() {

                                @Override
                                public kotlin.Unit invoke() {
                                    first.countDown();
                                    return Unit.INSTANCE;
                                }

                            });
                } else {
                    return group.map(new Function1<Integer, String>() {

                        @Override
                        public String invoke(Integer t1) {
                            return "last group: " + t1;
                        }

                    });
                }
            }

        }).blockingForEach(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String s) {
                results.add(s);
                return Unit.INSTANCE;
            }

        });

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void testFirstGroupsCompleteAndParentSlowToThenEmitFinalGroupsWhichThenSubscribesOnAndDelaysAndThenCompletes() throws InterruptedException {
        System.err.println("----------------------------------------------------------------------------------------------");
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<String>();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                sub.onSubscribe(new BooleanSubscription());
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                try {
                    first.await();
                } catch (InterruptedException e) {
                    sub.onError(e);
                    return;
                }
                sub.onNext(3);
                sub.onNext(3);
                sub.onComplete();
            }

        }).groupBy(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t) {
                return t;
            }

        }).flatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(final GroupedFlowable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function1<Integer, String>() {

                        @Override
                        public String invoke(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Function0() {

                                @Override
                                public kotlin.Unit invoke() {
                                    first.countDown();
                                    return Unit.INSTANCE;
                                }

                            });
                } else {
                    return group.subscribeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Function1<Integer, String>() {

                        @Override
                        public String invoke(Integer t1) {
                            return "last group: " + t1;
                        }

                    }).doOnEach(new Function1<Notification<String>, kotlin.Unit>() {

                        @Override
                        public Unit invoke(Notification<String> t1) {
                            System.err.println("subscribeOn notification => " + t1);
                            return Unit.INSTANCE;
                        }

                    });
                }
            }

        }).doOnEach(new Function1<Notification<String>, kotlin.Unit>() {

            @Override
            public Unit invoke(Notification<String> t1) {
                System.err.println("outer notification => " + t1);
                return Unit.INSTANCE;
            }

        }).blockingForEach(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String s) {
                results.add(s);
                return Unit.INSTANCE;
            }

        });

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void testFirstGroupsCompleteAndParentSlowToThenEmitFinalGroupsWhichThenObservesOnAndDelaysAndThenCompletes() throws InterruptedException {
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<String>();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                sub.onSubscribe(new BooleanSubscription());
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                try {
                    first.await();
                } catch (InterruptedException e) {
                    sub.onError(e);
                    return;
                }
                sub.onNext(3);
                sub.onNext(3);
                sub.onComplete();
            }

        }).groupBy(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t) {
                return t;
            }

        }).flatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(final GroupedFlowable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function1<Integer, String>() {

                        @Override
                        public String invoke(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Function0() {

                                @Override
                                public kotlin.Unit invoke() {
                                    first.countDown();
                                    return Unit.INSTANCE;
                                }

                            });
                } else {
                    return group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Function1<Integer, String>() {

                        @Override
                        public String invoke(Integer t1) {
                            return "last group: " + t1;
                        }

                    });
                }
            }

        }).blockingForEach(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String s) {
                results.add(s);
                return Unit.INSTANCE;
            }

        });

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void testGroupsWithNestedSubscribeOn() throws InterruptedException {
        final ArrayList<String> results = new ArrayList<String>();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                sub.onSubscribe(new BooleanSubscription());
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                sub.onComplete();
            }

        }).groupBy(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t) {
                return t;
            }

        }).flatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(final GroupedFlowable<Integer, Integer> group) {
                return group.subscribeOn(Schedulers.newThread()).map(new Function1<Integer, String>() {

                    @Override
                    public String invoke(Integer t1) {
                        System.out.println("Received: " + t1 + " on group : " + group.getKey());
                        return "first groups: " + t1;
                    }

                });
            }

        }).doOnEach(new Function1<Notification<String>, kotlin.Unit>() {

            @Override
            public Unit invoke(Notification<String> t1) {
                System.out.println("notification => " + t1);
                return Unit.INSTANCE;
            }

        }).blockingForEach(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String s) {
                results.add(s);
                return Unit.INSTANCE;
            }

        });

        System.out.println("Results: " + results);
        assertEquals(4, results.size());
    }

    @Test
    public void testGroupsWithNestedObserveOn() throws InterruptedException {
        final ArrayList<String> results = new ArrayList<String>();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                sub.onSubscribe(new BooleanSubscription());
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                sub.onComplete();
            }

        }).groupBy(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer t) {
                return t;
            }

        }).flatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(final GroupedFlowable<Integer, Integer> group) {
                return group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Function1<Integer, String>() {

                    @Override
                    public String invoke(Integer t1) {
                        return "first groups: " + t1;
                    }

                });
            }

        }).blockingForEach(new Function1<String, kotlin.Unit>() {

            @Override
            public Unit invoke(String s) {
                results.add(s);
                return Unit.INSTANCE;
            }

        });

        System.out.println("Results: " + results);
        assertEquals(4, results.size());
    }

    private static class Event {
        int source;
        String message;

        @Override
        public String toString() {
            return "Event => source: " + source + " message: " + message;
        }
    }

    Flowable<Event> ASYNC_INFINITE_OBSERVABLE_OF_EVENT(final int numGroups, final AtomicInteger subscribeCounter, final AtomicInteger sentEventCounter) {
        return SYNC_INFINITE_OBSERVABLE_OF_EVENT(numGroups, subscribeCounter, sentEventCounter).subscribeOn(Schedulers.newThread());
    }

    Flowable<Event> SYNC_INFINITE_OBSERVABLE_OF_EVENT(final int numGroups, final AtomicInteger subscribeCounter, final AtomicInteger sentEventCounter) {
        return Flowable.unsafeCreate(new Publisher<Event>() {

            @Override
            public void subscribe(final Subscriber<? super Event> op) {
                BooleanSubscription bs = new BooleanSubscription();
                op.onSubscribe(bs);
                subscribeCounter.incrementAndGet();
                int i = 0;
                while (!bs.isCancelled()) {
                    i++;
                    Event e = new Event();
                    e.source = i % numGroups;
                    e.message = "Event-" + i;
                    op.onNext(e);
                    sentEventCounter.incrementAndGet();
                }
                op.onComplete();
            }

        });
    }

    @Test
    public void testGroupByOnAsynchronousSourceAcceptsMultipleSubscriptions() throws InterruptedException {

        // choose an asynchronous source
        Flowable<Long> source = Flowable.interval(10, TimeUnit.MILLISECONDS).take(1);

        // apply groupBy to the source
        Flowable<GroupedFlowable<Boolean, Long>> stream = source.groupBy(IS_EVEN);

        // create two observers
        @SuppressWarnings("unchecked")
        DefaultSubscriber<GroupedFlowable<Boolean, Long>> o1 = mock(DefaultSubscriber.class);
        @SuppressWarnings("unchecked")
        DefaultSubscriber<GroupedFlowable<Boolean, Long>> o2 = mock(DefaultSubscriber.class);

        // subscribe with the observers
        stream.subscribe(o1);
        stream.subscribe(o2);

        // check that subscriptions were successful
        verify(o1, never()).onError(Mockito.<Throwable> any());
        verify(o2, never()).onError(Mockito.<Throwable> any());
    }

    private static Function1<Long, Boolean> IS_EVEN = new Function1<Long, Boolean>() {

        @Override
        public Boolean invoke(Long n) {
            return n % 2 == 0;
        }
    };

    private static Function1<Integer, Boolean> IS_EVEN2 = new Function1<Integer, Boolean>() {

        @Override
        public Boolean invoke(Integer n) {
            return n % 2 == 0;
        }
    };

    @Test
    public void testGroupByBackpressure() throws InterruptedException {

        TestSubscriber<String> ts = new TestSubscriber<String>();

        Flowable.range(1, 4000)
                .groupBy(IS_EVEN2)
                .flatMap(new Function1<GroupedFlowable<Boolean, Integer>, Flowable<String>>() {

                    @Override
                    public Flowable<String> invoke(final GroupedFlowable<Boolean, Integer> g) {
                        return g.observeOn(Schedulers.computation()).map(new Function1<Integer, String>() {

                            @Override
                            public String invoke(Integer l) {
                                if (g.getKey()) {
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                    }
                                    return l + " is even.";
                                } else {
                                    return l + " is odd.";
                                }
                            }

                        });
                    }

                }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    <T, R> Function1<T, R> just(final R value) {
        return new Function1<T, R>() {
            @Override
            public R invoke(T t1) {
                return value;
            }
        };
    }

    <T> Function1<Integer, T> fail(T dummy) {
        return new Function1<Integer, T>() {
            @Override
            public T invoke(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }

    <T, R> Function1<T, R> fail2(R dummy2) {
        return new Function1<T, R>() {
            @Override
            public R invoke(T t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }

    Function1<Integer, Integer> dbl = new Function1<Integer, Integer>() {
        @Override
        public Integer invoke(Integer t1) {
            return t1 * 2;
        }
    };
    Function1<Integer, Integer> identity = new Function1<Integer, Integer>() {
        @Override
        public Integer invoke(Integer v) {
            return v;
        }
    };

    @Test
    public void normalBehavior() {
        Flowable<String> source = Flowable.fromIterable(Arrays.asList(
                "  foo",
                " FoO ",
                "baR  ",
                "foO ",
                " Baz   ",
                "  qux ",
                "   bar",
                " BAR  ",
                "FOO ",
                "baz  ",
                " bAZ ",
                "    fOo    "
                ));

        /*
         * foo FoO foO FOO fOo
         * baR bar BAR
         * Baz baz bAZ
         * qux
         *
         */
        Function1<String, String> keysel = new Function1<String, String>() {
            @Override
            public String invoke(String t1) {
                return t1.trim().toLowerCase();
            }
        };
        Function1<String, String> valuesel = new Function1<String, String>() {
            @Override
            public String invoke(String t1) {
                return t1 + t1;
            }
        };

        Flowable<String> m = source.groupBy(keysel, valuesel)
                .flatMap(new Function1<GroupedFlowable<String, String>, Publisher<String>>() {
            @Override
            public Publisher<String> invoke(final GroupedFlowable<String, String> g) {
                System.out.println("-----------> NEXT: " + g.getKey());
                return g.take(2).map(new Function1<String, String>() {

                    int count;

                    @Override
                    public String invoke(String v) {
                        System.out.println(v);
                        return g.getKey() + "-" + count++;
                    }

                });
            }
        });

        TestSubscriber<String> ts = new TestSubscriber<String>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        System.out.println("ts .get " + ts.values());
        ts.assertNoErrors();
        assertEquals(ts.values(),
                Arrays.asList("foo-0", "foo-1", "bar-0", "foo-0", "baz-0", "qux-0", "bar-1", "bar-0", "foo-1", "baz-1", "baz-0", "foo-0"));

    }

    @Test
    public void keySelectorThrows() {
        Flowable<Integer> source = Flowable.just(0, 1, 2, 3, 4, 5, 6);

        Flowable<Integer> m = source.groupBy(fail(0), dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertNoValues();
    }

    @Test
    public void valueSelectorThrows() {
        Flowable<Integer> source = Flowable.just(0, 1, 2, 3, 4, 5, 6);

        Flowable<Integer> m = source.groupBy(identity, fail(0)).flatMap(FLATTEN_INTEGER);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertNoValues();

    }

    @Test
    public void innerEscapeCompleted() {
        Flowable<Integer> source = Flowable.just(0);

        Flowable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println(ts.values());
    }

    /**
     * Assert we get an IllegalStateException if trying to subscribe to an inner GroupedFlowable more than once.
     */
    @Test
    public void testExceptionIfSubscribeToChildMoreThanOnce() {
        Flowable<Integer> source = Flowable.just(0);

        final AtomicReference<GroupedFlowable<Integer, Integer>> inner = new AtomicReference<GroupedFlowable<Integer, Integer>>();

        Flowable<GroupedFlowable<Integer, Integer>> m = source.groupBy(identity, dbl);

        m.subscribe(new Function1<GroupedFlowable<Integer, Integer>, kotlin.Unit>() {
            @Override
            public Unit invoke(GroupedFlowable<Integer, Integer> t1) {
                inner.set(t1);
                return Unit.INSTANCE;
            }
        });

        inner.get().subscribe();

        @SuppressWarnings("unchecked")
        DefaultSubscriber<Integer> o2 = mock(DefaultSubscriber.class);

        inner.get().subscribe(o2);

        verify(o2, never()).onComplete();
        verify(o2, never()).onNext(anyInt());
        verify(o2).onError(any(IllegalStateException.class));
    }

    @Test
    public void testError2() {
        Flowable<Integer> source = Flowable.concat(Flowable.just(0),
                Flowable.<Integer> error(new TestException("Forced failure")));

        Flowable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertValueCount(1);
    }

    @Test
    public void testgroupByBackpressure() throws InterruptedException {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Flowable.range(1, 4000).groupBy(IS_EVEN2).flatMap(new Function1<GroupedFlowable<Boolean, Integer>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(final GroupedFlowable<Boolean, Integer> g) {
                return g.doOnComplete(new Function0() {

                    @Override
                    public kotlin.Unit invoke() {
                        System.out.println("//////////////////// COMPLETED-A");
                        return Unit.INSTANCE;
                    }

                }).observeOn(Schedulers.computation()).map(new Function1<Integer, String>() {

                    int c;

                    @Override
                    public String invoke(Integer l) {
                        if (g.getKey()) {
                            if (c++ < 400) {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                }
                            }
                            return l + " is even.";
                        } else {
                            return l + " is odd.";
                        }
                    }

                }).doOnComplete(new Function0() {

                    @Override
                    public kotlin.Unit invoke() {
                        System.out.println("//////////////////// COMPLETED-B");
                        return Unit.INSTANCE;
                    }

                });
            }

        }).doOnEach(new Function1<Notification<String>, kotlin.Unit>() {

            @Override
            public Unit invoke(Notification<String> t1) {
                System.out.println("NEXT: " + t1);
                return Unit.INSTANCE;
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    @Test
    public void testgroupByBackpressure2() throws InterruptedException {

        TestSubscriber<String> ts = new TestSubscriber<String>();

        Flowable.range(1, 4000)
                .doOnNext(new Function1<Integer, kotlin.Unit>() {
                @Override
                public Unit invoke(Integer v) {
                    System.out.println("testgroupByBackpressure2 >> " + v);
                    return Unit.INSTANCE;
                }
            })
                .groupBy(IS_EVEN2).flatMap(new Function1<GroupedFlowable<Boolean, Integer>, Flowable<String>>() {

            @Override
            public Flowable<String> invoke(final GroupedFlowable<Boolean, Integer> g) {
                return g.take(2).observeOn(Schedulers.computation()).map(new Function1<Integer, String>() {

                    @Override
                    public String invoke(Integer l) {
                        if (g.getKey()) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                            }
                            return l + " is even.";
                        } else {
                            return l + " is odd.";
                        }
                    }

                });
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    static Function1<GroupedFlowable<Integer, Integer>, Flowable<Integer>> FLATTEN_INTEGER = new Function1<GroupedFlowable<Integer, Integer>, Flowable<Integer>>() {

        @Override
        public Flowable<Integer> invoke(GroupedFlowable<Integer, Integer> t) {
            return t;
        }

    };

    @Test
    public void testGroupByWithNullKey() {
        final String[] key = new String[]{"uninitialized"};
        final List<String> values = new ArrayList<String>();
        Flowable.just("a", "b", "c").groupBy(new Function1<String, String>() {

            @Override
            public String invoke(String value) {
                return null;
            }
        }).subscribe(new Function1<GroupedFlowable<String, String>, Unit>() {

            @Override
            public Unit invoke(GroupedFlowable<String, String> groupedFlowable) {
                key[0] = groupedFlowable.getKey();
                groupedFlowable.subscribe(new Function1<String, kotlin.Unit>() {

                    @Override
                    public Unit invoke(String s) {
                        values.add(s);
                        return Unit.INSTANCE;
                    }
                });
                return Unit.INSTANCE;
            }
        });
        assertEquals(null, key[0]);
        assertEquals(Arrays.asList("a", "b", "c"), values);
    }

    @Test
    public void testGroupByUnsubscribe() {
        final Subscription s = mock(Subscription.class);
        Flowable<Integer> o = Flowable.unsafeCreate(
                new Publisher<Integer>() {
                    @Override
                    public void subscribe(Subscriber<? super Integer> subscriber) {
                        subscriber.onSubscribe(s);
                    }
                }
        );
        TestSubscriber<Object> ts = new TestSubscriber<Object>();

        o.groupBy(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer integer) {
                return null;
            }
        }).subscribe(ts);

        ts.dispose();

        verify(s).cancel();
    }

    @Test
    public void testGroupByShouldPropagateError() {
        final Throwable e = new RuntimeException("Oops");
        final TestSubscriber<Integer> inner1 = new TestSubscriber<Integer>();
        final TestSubscriber<Integer> inner2 = new TestSubscriber<Integer>();

        final TestSubscriber<GroupedFlowable<Integer, Integer>> outer
                = new TestSubscriber<GroupedFlowable<Integer, Integer>>(new DefaultSubscriber<GroupedFlowable<Integer, Integer>>() {

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(GroupedFlowable<Integer, Integer> o) {
                if (o.getKey() == 0) {
                    o.subscribe(inner1);
                } else {
                    o.subscribe(inner2);
                }
            }
        });
        Flowable.unsafeCreate(
                new Publisher<Integer>() {
                    @Override
                    public void subscribe(Subscriber<? super Integer> subscriber) {
                        subscriber.onSubscribe(new BooleanSubscription());
                        subscriber.onNext(0);
                        subscriber.onNext(1);
                        subscriber.onError(e);
                    }
                }
        ).groupBy(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer i) {
                return i % 2;
            }
        }).subscribe(outer);
        assertEquals(Arrays.asList(e), outer.errors());
        assertEquals(Arrays.asList(e), inner1.errors());
        assertEquals(Arrays.asList(e), inner2.errors());
    }

    @Test
    public void testRequestOverflow() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Flowable
                .just(1, 2, 3)
                // group into one group
                .groupBy(new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer t) {
                        return 1;
                    }
                })
                // flatten
                .concatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> invoke(GroupedFlowable<Integer, Integer> g) {
                        return g;
                    }
                })
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(2);
                    }

                    @Override
                    public void onComplete() {
                        completed.set(true);

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        System.out.println(t);
                        //provoke possible request overflow
                        request(Long.MAX_VALUE - 1);
                    }});
        assertTrue(completed.get());
    }

    /**
     * Issue #3425.
     *
     * The problem is that a request of 1 may create a new group, emit to the desired group
     * or emit to a completely different group. In this test, the merge requests N which
     * must be produced by the range, however it will create a bunch of groups before the actual
     * group receives a value.
     */
    @Test
    public void testBackpressureObserveOnOuter() {
        for (int j = 0; j < 1000; j++) {
            Flowable.merge(
                    Flowable.range(0, 500)
                            .groupBy(new Function1<Integer, Object>() {
                        @Override
                        public Object invoke(Integer i) {
                            return i % (Flowable.bufferSize() + 2);
                        }
                    })
                    .observeOn(Schedulers.computation())
            ).blockingLast();
        }
    }

    /**
     * Synchronous verification of issue #3425.
     */
    @Test
    public void testBackpressureInnerDoesntOverflowOuter() {
        TestSubscriber<GroupedFlowable<Integer, Integer>> ts = new TestSubscriber<GroupedFlowable<Integer, Integer>>(0L);

        Flowable.fromArray(1, 2)
                .groupBy(new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer v) {
                        return v;
                    }
                })
                .doOnNext(new Function1<GroupedFlowable<Integer, Integer>, kotlin.Unit>() {
                    @Override
                    public Unit invoke(GroupedFlowable<Integer, Integer> g) {
                        g.subscribe();
                        return Unit.INSTANCE;
                    }
                }) // this will request Long.MAX_VALUE
                .subscribe(ts)
                ;
        ts.request(1);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }

    @Test
    public void testOneGroupInnerRequestsTwiceBuffer() {
        TestSubscriber<Object> ts1 = new TestSubscriber<Object>(0L);
        final TestSubscriber<Object> ts2 = new TestSubscriber<Object>(0L);

        Flowable.range(1, Flowable.bufferSize() * 2)
                .groupBy(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return 1;
            }
        })
                .doOnNext(new Function1<GroupedFlowable<Object, Integer>, Unit>() {
            @Override
            public Unit invoke(GroupedFlowable<Object, Integer> g) {
                g.subscribe(ts2);
                return Unit.INSTANCE;
            }
        })
        .subscribe(ts1);

        ts1.assertNoValues();
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertNoValues();
        ts2.assertNoErrors();
        ts2.assertNotComplete();

        ts1.request(1);

        ts1.assertValueCount(1);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertNoValues();
        ts2.assertNoErrors();
        ts2.assertNotComplete();

        ts2.request(Flowable.bufferSize() * 2);

        ts2.assertValueCount(Flowable.bufferSize() * 2);
        ts2.assertNoErrors();
        ts2.assertComplete();
    }

    @Test
    public void outerInnerFusion() {
        final TestSubscriber<Integer> ts1 = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        final TestSubscriber<GroupedFlowable<Integer, Integer>> ts2 = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        Flowable.range(1, 10).groupBy(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return 1;
            }
        }, new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v + 1;
            }
        })
                .doOnNext(new Function1<GroupedFlowable<Integer, Integer>, kotlin.Unit>() {
            @Override
            public Unit invoke(GroupedFlowable<Integer, Integer> g) {
                g.subscribe(ts1);
                return Unit.INSTANCE;
            }
        })
        .subscribe(ts2);

        ts1
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(FusedQueueSubscription.ASYNC))
        .assertValues(2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        .assertNoErrors()
        .assertComplete();

        ts2
        .assertOf(SubscriberFusion.<GroupedFlowable<Integer, Integer>>assertFusionMode(FusedQueueSubscription.ASYNC))
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();
    }


    @Test
    public void keySelectorAndDelayError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .groupBy(Functions.<Integer>identity(), true)
                .flatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(GroupedFlowable<Integer, Integer> g) {
                return g;
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void keyAndValueSelectorAndDelayError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .groupBy(Functions.<Integer>identity(), Functions.<Integer>identity(), true)
                .flatMap(new Function1<GroupedFlowable<Integer, Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(GroupedFlowable<Integer, Integer> g) {
                return g;
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).groupBy(Functions.justFunction(1)));

        Flowable.just(1)
        .groupBy(Functions.justFunction(1))
                .doOnNext(new Function1<GroupedFlowable<Integer, Integer>, kotlin.Unit>() {
            @Override
            public Unit invoke(GroupedFlowable<Integer, Integer> g) {
                TestHelper.checkDisposed(g);
                return Unit.INSTANCE;
            }
        })
        .test();
    }

    @Test
    public void reentrantComplete() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onComplete();
                }
            }
        };

        Flowable.merge(ps.groupBy(Functions.justFunction(1)))
        .subscribe(to);

        ps.onNext(1);

        to.assertResult(1);
    }

    @Test
    public void reentrantCompleteCancel() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onComplete();
                    dispose();
                }
            }
        };

        Flowable.merge(ps.groupBy(Functions.justFunction(1)))
        .subscribe(to);

        ps.onNext(1);

        to.assertSubscribed().assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void delayErrorSimpleComplete() {
        Flowable.just(1)
        .groupBy(Functions.justFunction(1), true)
        .flatMap(Functions.<Flowable<Integer>>identity())
        .test()
        .assertResult(1);
    }

    @Test
    public void mainFusionRejected() {
        TestSubscriber<Flowable<Integer>> ts = SubscriberFusion.newTest(FusedQueueSubscription.SYNC);

        Flowable.just(1)
        .groupBy(Functions.justFunction(1))
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.NONE)
        .assertValueCount(1)
        .assertComplete()
        .assertNoErrors();
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function1<Flowable<Object>, Object>() {
            @Override
            public Object invoke(Flowable<Object> f) {
                return f.groupBy(Functions.justFunction(1));
            }
        }, false, 1, 1, (Object[])null);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.just(1)
                .groupBy(Functions.justFunction(1)));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function1<Flowable<Object>, Publisher<GroupedFlowable<Integer, Object>>>() {
            @Override
            public Publisher<GroupedFlowable<Integer, Object>> invoke(Flowable<Object> f) {
                return f.groupBy(Functions.justFunction(1));
            }
        });
    }

    @Test
    public void nullKeyTakeInner() {
        Flowable.just(1)
                .groupBy(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        })
                .flatMap(new Function1<GroupedFlowable<Object, Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(GroupedFlowable<Object, Integer> g) {
                return g.take(1);
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void errorFused() {
        TestSubscriber<Object> ts = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        Flowable.error(new TestException())
        .groupBy(Functions.justFunction(1))
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.ASYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void errorFusedDelayed() {
        TestSubscriber<Object> ts = SubscriberFusion.newTest(FusedQueueSubscription.ANY);

        Flowable.error(new TestException())
        .groupBy(Functions.justFunction(1), true)
        .subscribe(ts);

        SubscriberFusion.assertFusion(ts, FusedQueueSubscription.ASYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void groupError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .groupBy(Functions.justFunction(1), true)
                .flatMap(new Function1<GroupedFlowable<Integer, Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(GroupedFlowable<Integer, Integer> g) {
                return g.hide();
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void groupComplete() {
        Flowable.just(1)
        .groupBy(Functions.justFunction(1), true)
                .flatMap(new Function1<GroupedFlowable<Integer, Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(GroupedFlowable<Integer, Integer> g) {
                return g.hide();
            }
        })
        .test()
        .assertResult(1);
    }
}
