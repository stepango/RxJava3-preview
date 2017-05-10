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

package io.reactivex.flowable.internal.subscribers;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Action;
import io.reactivex.common.functions.Consumer;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import kotlin.Unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LambdaSubscriberTest {

    @Test
    public void onSubscribeThrows() {
        final List<Object> received = new ArrayList<Object>();

        LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public kotlin.Unit invoke() {
                received.add(100);
                return Unit.INSTANCE;
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                throw new TestException();
            }
        });

        assertFalse(o.isDisposed());

        Flowable.just(1).subscribe(o);

        assertTrue(received.toString(), received.get(0) instanceof TestException);
        assertEquals(received.toString(), 1, received.size());

        assertTrue(o.isDisposed());
    }

    @Test
    public void onNextThrows() {
        final List<Object> received = new ArrayList<Object>();

        LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                throw new TestException();
            }
        },
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public kotlin.Unit invoke() {
                received.add(100);
                return Unit.INSTANCE;
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                s.request(Long.MAX_VALUE);
            }
        });

        assertFalse(o.isDisposed());

        Flowable.just(1).subscribe(o);

        assertTrue(received.toString(), received.get(0) instanceof TestException);
        assertEquals(received.toString(), 1, received.size());

        assertTrue(o.isDisposed());
    }

    @Test
    public void onErrorThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<Object>();

            LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    received.add(v);
                }
            },
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    throw new TestException("Inner");
                }
            }, new Action() {
                @Override
                public kotlin.Unit invoke() {
                    received.add(100);
                    return Unit.INSTANCE;
                }
            }, new Consumer<Subscription>() {
                @Override
                public void accept(Subscription s) throws Exception {
                    s.request(Long.MAX_VALUE);
                }
            });

            assertFalse(o.isDisposed());

            Flowable.<Integer>error(new TestException("Outer")).subscribe(o);

            assertTrue(received.toString(), received.isEmpty());

            assertTrue(o.isDisposed());

            TestCommonHelper.assertError(errors, 0, CompositeException.class);
            List<Throwable> ce = TestCommonHelper.compositeList(errors.get(0));
            TestCommonHelper.assertError(ce, 0, TestException.class, "Outer");
            TestCommonHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onCompleteThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<Object>();

            LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    received.add(v);
                }
            },
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    received.add(e);
                }
            }, new Action() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            }, new Consumer<Subscription>() {
                @Override
                public void accept(Subscription s) throws Exception {
                    s.request(Long.MAX_VALUE);
                }
            });

            assertFalse(o.isDisposed());

            Flowable.<Integer>empty().subscribe(o);

            assertTrue(received.toString(), received.isEmpty());

            assertTrue(o.isDisposed());

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void badSourceOnSubscribe() {
        Flowable<Integer> source = Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                BooleanSubscription s1 = new BooleanSubscription();
                s.onSubscribe(s1);
                BooleanSubscription s2 = new BooleanSubscription();
                s.onSubscribe(s2);

                assertFalse(s1.isCancelled());
                assertTrue(s2.isCancelled());

                s.onNext(1);
                s.onComplete();
            }
        });

        final List<Object> received = new ArrayList<Object>();

        LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public kotlin.Unit invoke() {
                received.add(100);
                return Unit.INSTANCE;
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                s.request(Long.MAX_VALUE);
            }
        });

        source.subscribe(o);

        assertEquals(Arrays.asList(1, 100), received);
    }
    @Test
    public void badSourceEmitAfterDone() {
        Flowable<Integer> source = Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                BooleanSubscription s1 = new BooleanSubscription();
                s.onSubscribe(s1);

                s.onNext(1);
                s.onComplete();
                s.onNext(2);
                s.onError(new TestException());
                s.onComplete();
            }
        });

        final List<Object> received = new ArrayList<Object>();

        LambdaSubscriber<Object> o = new LambdaSubscriber<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public kotlin.Unit invoke() {
                received.add(100);
                return Unit.INSTANCE;
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                s.request(Long.MAX_VALUE);
            }
        });

        source.subscribe(o);

        assertEquals(Arrays.asList(1, 100), received);
    }

    @Test
    public void onNextThrowsCancelsUpstream() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        final List<Throwable> errors = new ArrayList<Throwable>();

        ps.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                errors.add(e);
            }
        });

        assertTrue("No observers?!", ps.hasSubscribers());
        assertTrue("Has errors already?!", errors.isEmpty());

        ps.onNext(1);

        assertFalse("Has observers?!", ps.hasSubscribers());
        assertFalse("No errors?!", errors.isEmpty());

        assertTrue(errors.toString(), errors.get(0) instanceof TestException);
    }

    @Test
    public void onSubscribeThrowsCancelsUpstream() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        final List<Throwable> errors = new ArrayList<Throwable>();

        ps.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                errors.add(e);
            }
        }, new Action() {
            @Override
            public kotlin.Unit invoke() {
                return Unit.INSTANCE;
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                throw new TestException();
            }
        });

        assertFalse("Has observers?!", ps.hasSubscribers());
        assertFalse("No errors?!", errors.isEmpty());

        assertTrue(errors.toString(), errors.get(0) instanceof TestException);
    }
}
