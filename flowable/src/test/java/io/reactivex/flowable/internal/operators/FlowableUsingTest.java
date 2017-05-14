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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowableUsingTest {

    private interface Resource {
        String getTextFromWeb();

        void dispose();
    }

    private static class DisposeAction implements Function1<Resource, kotlin.Unit> {

        @Override
        public Unit invoke(Resource r) {
            r.dispose();
            return Unit.INSTANCE;
        }

    }

    private final Function1<Disposable, kotlin.Unit> disposeSubscription = new Function1<Disposable, kotlin.Unit>() {

        @Override
        public Unit invoke(Disposable s) {
            s.dispose();
            return Unit.INSTANCE;
        }

    };

    @Test
    public void testUsing() {
        performTestUsing(false);
    }

    @Test
    public void testUsingEagerly() {
        performTestUsing(true);
    }

    private void performTestUsing(boolean disposeEagerly) {
        final Resource resource = mock(Resource.class);
        when(resource.getTextFromWeb()).thenReturn("Hello world!");

        Callable<Resource> resourceFactory = new Callable<Resource>() {
            @Override
            public Resource call() {
                return resource;
            }
        };

        Function1<Resource, Flowable<String>> observableFactory = new Function1<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> invoke(Resource res) {
                return Flowable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();

        // The resouce should be closed
        verify(resource, times(1)).dispose();
    }

    @Test
    public void testUsingWithSubscribingTwice() {
        performTestUsingWithSubscribingTwice(false);
    }

    @Test
    public void testUsingWithSubscribingTwiceDisposeEagerly() {
        performTestUsingWithSubscribingTwice(true);
    }

    private void performTestUsingWithSubscribingTwice(boolean disposeEagerly) {
        // When subscribe is called, a new resource should be created.
        Callable<Resource> resourceFactory = new Callable<Resource>() {
            @Override
            public Resource call() {
                return new Resource() {

                    boolean first = true;

                    @Override
                    public String getTextFromWeb() {
                        if (first) {
                            first = false;
                            return "Hello world!";
                        }
                        return "Nothing";
                    }

                    @Override
                    public void dispose() {
                        // do nothing
                    }

                };
            }
        };

        Function1<Resource, Flowable<String>> observableFactory = new Function1<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> invoke(Resource res) {
                    return Flowable.fromArray(res.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), disposeEagerly);
        observable.subscribe(observer);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onComplete();

        inOrder.verify(observer, times(1)).onNext("Hello");
        inOrder.verify(observer, times(1)).onNext("world!");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = TestException.class)
    public void testUsingWithResourceFactoryError() {
        performTestUsingWithResourceFactoryError(false);
    }

    @Test(expected = TestException.class)
    public void testUsingWithResourceFactoryErrorDisposeEagerly() {
        performTestUsingWithResourceFactoryError(true);
    }

    private void performTestUsingWithResourceFactoryError(boolean disposeEagerly) {
        Callable<Disposable> resourceFactory = new Callable<Disposable>() {
            @Override
            public Disposable call() {
                throw new TestException();
            }
        };

        Function1<Disposable, Flowable<Integer>> observableFactory = new Function1<Disposable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Disposable s) {
                return Flowable.empty();
            }
        };

        Flowable.using(resourceFactory, observableFactory, disposeSubscription)
        .blockingLast();
    }

    @Test
    public void testUsingWithFlowableFactoryError() {
        performTestUsingWithFlowableFactoryError(false);
    }

    @Test
    public void testUsingWithFlowableFactoryErrorDisposeEagerly() {
        performTestUsingWithFlowableFactoryError(true);
    }

    private void performTestUsingWithFlowableFactoryError(boolean disposeEagerly) {
        final Runnable unsubscribe = mock(Runnable.class);
        Callable<Disposable> resourceFactory = new Callable<Disposable>() {
            @Override
            public Disposable call() {
                return Disposables.fromRunnable(unsubscribe);
            }
        };

        Function1<Disposable, Flowable<Integer>> observableFactory = new Function1<Disposable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Disposable subscription) {
                throw new TestException();
            }
        };

        try {
            Flowable.using(resourceFactory, observableFactory, disposeSubscription).blockingLast();
            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe, times(1)).run();
        }
    }

    @Test
    @Ignore("subscribe() can't throw")
    public void testUsingWithFlowableFactoryErrorInOnSubscribe() {
        performTestUsingWithFlowableFactoryErrorInOnSubscribe(false);
    }

    @Test
    @Ignore("subscribe() can't throw")
    public void testUsingWithFlowableFactoryErrorInOnSubscribeDisposeEagerly() {
        performTestUsingWithFlowableFactoryErrorInOnSubscribe(true);
    }

    private void performTestUsingWithFlowableFactoryErrorInOnSubscribe(boolean disposeEagerly) {
        final Runnable unsubscribe = mock(Runnable.class);
        Callable<Disposable> resourceFactory = new Callable<Disposable>() {
            @Override
            public Disposable call() {
                return Disposables.fromRunnable(unsubscribe);
            }
        };

        Function1<Disposable, Flowable<Integer>> observableFactory = new Function1<Disposable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> invoke(Disposable subscription) {
                return Flowable.unsafeCreate(new Publisher<Integer>() {
                    @Override
                    public void subscribe(Subscriber<? super Integer> t1) {
                        throw new TestException();
                    }
                });
            }
        };

        try {
            Flowable
            .using(resourceFactory, observableFactory, disposeSubscription, disposeEagerly)
            .blockingLast();

            fail("Should throw a TestException when the observableFactory throws it");
        } catch (TestException e) {
            // Make sure that unsubscribe is called so that users can close
            // the resource if some error happens.
            verify(unsubscribe, times(1)).run();
        }
    }

    @Test
    public void testUsingDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Callable<Resource> resourceFactory = createResourceFactory(events);
        final Function0 completion = createOnCompletedAction(events);
        final Function0 unsub = createUnsubAction(events);

        Function1<Resource, Flowable<String>> observableFactory = new Function1<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> invoke(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnComplete(completion);

        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("disposed", "completed"), events);

    }

    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeCompletion() {
        final List<String> events = new ArrayList<String>();
        Callable<Resource> resourceFactory = createResourceFactory(events);
        final Function0 completion = createOnCompletedAction(events);
        final Function0 unsub = createUnsubAction(events);

        Function1<Resource, Flowable<String>> observableFactory = new Function1<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> invoke(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnComplete(completion);

        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("completed", "disposed"), events);

    }



    @Test
    public void testUsingDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        Callable<Resource> resourceFactory = createResourceFactory(events);
        final Function1<Throwable, kotlin.Unit> onError = createOnErrorAction(events);
        final Function0 unsub = createUnsubAction(events);

        Function1<Resource, Flowable<String>> observableFactory = new Function1<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> invoke(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Flowable.<String>error(new RuntimeException()));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), true)
        .doOnCancel(unsub)
        .doOnError(onError);

        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("disposed", "error"), events);

    }

    @Test
    public void testUsingDoesNotDisposesEagerlyBeforeError() {
        final List<String> events = new ArrayList<String>();
        final Callable<Resource> resourceFactory = createResourceFactory(events);
        final Function1<Throwable, kotlin.Unit> onError = createOnErrorAction(events);
        final Function0 unsub = createUnsubAction(events);

        Function1<Resource, Flowable<String>> observableFactory = new Function1<Resource, Flowable<String>>() {
            @Override
            public Flowable<String> invoke(Resource resource) {
                return Flowable.fromArray(resource.getTextFromWeb().split(" "))
                        .concatWith(Flowable.<String>error(new RuntimeException()));
            }
        };

        Subscriber<String> observer = TestHelper.mockSubscriber();

        Flowable<String> observable = Flowable.using(resourceFactory, observableFactory,
                new DisposeAction(), false)
        .doOnCancel(unsub)
        .doOnError(onError);

        observable.safeSubscribe(observer);

        assertEquals(Arrays.asList("error", "disposed"), events);
    }

    private static Function0 createUnsubAction(final List<String> events) {
        return new Function0() {
            @Override
            public kotlin.Unit invoke() {
                events.add("unsub");
                return Unit.INSTANCE;
            }
        };
    }

    private static Function1<Throwable, kotlin.Unit> createOnErrorAction(final List<String> events) {
        return new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable t) {
                events.add("error");
                return Unit.INSTANCE;
            }
        };
    }

    private static Callable<Resource> createResourceFactory(final List<String> events) {
        return new Callable<Resource>() {
            @Override
            public Resource call() {
                return new Resource() {

                    @Override
                    public String getTextFromWeb() {
                        return "hello world";
                    }

                    @Override
                    public void dispose() {
                        events.add("disposed");
                    }
                };
            }
        };
    }

    private static Function0 createOnCompletedAction(final List<String> events) {
        return new Function0() {
            @Override
            public kotlin.Unit invoke() {
                events.add("completed");
                return Unit.INSTANCE;
            }
        };
    }

    @Test
    public void factoryThrows() {

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger count = new AtomicInteger();

        Flowable.<Integer, Integer>using(
                new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        return 1;
                    }
                },
                new Function1<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> invoke(Integer v) {
                        throw new TestException("forced failure");
                    }
                },
                new Function1<Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Integer c) {
                        count.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                }
        )
        .subscribe(ts);

        ts.assertError(TestException.class);

        Assert.assertEquals(1, count.get());
    }

    @Test
    public void nonEagerTermination() {

        TestSubscriber<Integer> ts = TestSubscriber.create();

        final AtomicInteger count = new AtomicInteger();

        Flowable.<Integer, Integer>using(
                new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        return 1;
                    }
                },
                new Function1<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> invoke(Integer v) {
                        return Flowable.just(v);
                    }
                },
                new Function1<Integer, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Integer c) {
                        count.incrementAndGet();
                        return Unit.INSTANCE;
                    }
                }, false
        )
        .subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertEquals(1, count.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.using(
                new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return 1;
                    }
                },
                new Function1<Object, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> invoke(Object v) {
                        return Flowable.never();
                    }
                },
                Functions.emptyConsumer()
        ));
    }

    @Test
    public void supplierDisposerCrash() {
        TestSubscriber<Object> to = Flowable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function1<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> invoke(Object v) {
                throw new TestException("First");
            }
        }, new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object e) {
                throw new TestException("Second");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        TestCommonHelper.assertError(errors, 0, TestException.class, "First");
        TestCommonHelper.assertError(errors, 1, TestException.class, "Second");
    }

    @Test
    public void eagerOnErrorDisposerCrash() {
        TestSubscriber<Object> to = Flowable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function1<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> invoke(Object v) {
                return Flowable.error(new TestException("First"));
            }
        }, new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object e) {
                throw new TestException("Second");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        TestCommonHelper.assertError(errors, 0, TestException.class, "First");
        TestCommonHelper.assertError(errors, 1, TestException.class, "Second");
    }

    @Test
    public void eagerOnCompleteDisposerCrash() {
        Flowable.using(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        }, new Function1<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> invoke(Object v) {
                return Flowable.empty();
            }
        }, new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object e) {
                throw new TestException("Second");
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "Second");
    }

    @Test
    public void nonEagerDisposerCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Flowable.using(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            }, new Function1<Object, Flowable<Object>>() {
                @Override
                public Flowable<Object> invoke(Object v) {
                    return Flowable.empty();
                }
            }, new Function1<Object, kotlin.Unit>() {
                @Override
                public Unit invoke(Object e) {
                    throw new TestException("Second");
                }
            }, false)
            .test()
            .assertResult();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
