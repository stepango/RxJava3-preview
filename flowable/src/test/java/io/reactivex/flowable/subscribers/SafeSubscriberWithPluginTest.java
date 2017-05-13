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

package io.reactivex.flowable.subscribers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.exceptions.TestException;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.fail;

public class SafeSubscriberWithPluginTest {
    private final class SubscriptionCancelThrows implements Subscription {
        @Override
        public void cancel() {
            throw new RuntimeException();
        }

        @Override
        public void request(long n) {

        }
    }

    @Before
    @After
    public void resetBefore() {
        RxJavaCommonPlugins.reset();
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void testOnCompletedThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new TestException();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        try {
            safe.onComplete();
            fail();
        } catch (RuntimeException e) {
            // FIXME no longer assertable
            // assertTrue(safe.isUnsubscribed());
        }
    }

    @Test
    public void testOnCompletedThrows2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new RuntimeException(new TestException());
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);

        try {
            safe.onComplete();
        } catch (RuntimeException ex) {
            // expected
        }

        // FIXME no longer assertable
        // assertTrue(safe.isUnsubscribed());
    }

    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginException() {
        RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                throw new RuntimeException();
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new TestException();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);

        safe.onComplete();
    }

    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhileOnErrorUnsubscribeThrows() {
        RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
            int calls;
            @Override
            public Unit invoke(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
                return Unit.INSTANCE;
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

        safe.onError(new TestException());
    }

    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhileOnErrorThrowsNotImplAndUnsubscribeThrows() {
        RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
            int calls;
            @Override
            public Unit invoke(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
                return Unit.INSTANCE;
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

        safe.onError(new TestException());
    }

    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhileOnErrorThrows() {
        RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
            int calls;
            @Override
            public Unit invoke(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
                return Unit.INSTANCE;
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);

        safe.onError(new TestException());
    }
    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhileOnErrorThrowsAndUnsubscribeThrows() {
        RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
            int calls;
            @Override
            public Unit invoke(Throwable e) {
                if (++calls == 2) {
                    throw new RuntimeException();
                }
                return Unit.INSTANCE;
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

        safe.onError(new TestException());
    }
    @Test(expected = RuntimeException.class)
    @Ignore("Subscribers can't throw")
    public void testPluginExceptionWhenUnsubscribing2() {
        RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
            int calls;
            @Override
            public Unit invoke(Throwable e) {
                if (++calls == 3) {
                    throw new RuntimeException();
                }
                return Unit.INSTANCE;
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

        safe.onError(new TestException());
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void testPluginErrorHandlerReceivesExceptionWhenUnsubscribeAfterCompletionThrows() {
        final AtomicInteger calls = new AtomicInteger();
        RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                calls.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        final AtomicInteger errorCount = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                errorCount.incrementAndGet();
            }
        };
        final RuntimeException ex = new RuntimeException();
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new Subscription() {
            @Override
            public void cancel() {
                throw ex;
            }

            @Override
            public void request(long n) {

            }
        });

//        try {
//            safe.onComplete();
//            Assert.fail();
//        } catch(UnsubscribeFailedException e) {
//            assertEquals(1, calls.get());
//            assertEquals(0, errors.get());
//        }
    }

    @Test
    @Ignore("Subscribers can't throw")
    public void testPluginErrorHandlerReceivesExceptionFromFailingUnsubscribeAfterCompletionThrows() {
        final AtomicInteger calls = new AtomicInteger();
        RxJavaCommonPlugins.setErrorHandler(new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                    calls.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        final AtomicInteger errorCount = new AtomicInteger();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {

            @Override
            public void onComplete() {
                throw new RuntimeException();
            }

            @Override
            public void onError(Throwable e) {
                errorCount.incrementAndGet();
            }
        };
        SafeSubscriber<Integer> safe = new SafeSubscriber<Integer>(ts);
        safe.onSubscribe(new SubscriptionCancelThrows());

//        try {
//            safe.onComplete();
//            Assert.fail();
//        } catch(UnsubscribeFailedException e) {
//            assertEquals(2, calls.get());
//            assertEquals(0, errors.get());
//        }
    }
}
