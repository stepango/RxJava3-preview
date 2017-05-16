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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import io.reactivex.common.internal.utils.CrashingIterable;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class FlowableZipIterableTest {
    Function2<String, String, String> concat2Strings;
    PublishProcessor<String> s1;
    PublishProcessor<String> s2;
    Flowable<String> zipped;

    Subscriber<String> observer;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = new Function2<String, String, String>() {
            @Override
            public String invoke(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = PublishProcessor.create();
        s2 = PublishProcessor.create();
        zipped = Flowable.zip(s1, s2, concat2Strings);

        observer = TestHelper.mockSubscriber();
        inOrder = inOrder(observer);

        zipped.subscribe(observer);
    }

    Function2<Object, Object, String> zipr2 = new Function2<Object, Object, String>() {

        @Override
        public String invoke(Object t1, Object t2) {
            return "" + t1 + t2;
        }

    };
    Function3<Object, Object, Object, String> zipr3 = new Function3<Object, Object, Object, String>() {

        @Override
        public String invoke(Object t1, Object t2, Object t3) {
            return "" + t1 + t2 + t3;
        }

    };

    @Test
    public void testZipIterableSameSize() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onNext("three-3");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableEmptyFirstSize() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onComplete();

        io.verify(o).onComplete();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableEmptySecond() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList();

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onComplete();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testZipIterableFirstShorter() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableSecondShorter() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableFirstThrows() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();

    }

    @Test
    public void testZipIterableIteratorThrows() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                throw new TestException();
            }
        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any(String.class));

    }

    @Test
    public void testZipIterableHasNextThrows() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    int count;

                    @Override
                    public boolean hasNext() {
                        if (count == 0) {
                            return true;
                        }
                        throw new TestException();
                    }

                    @Override
                    public String next() {
                        count++;
                        return "1";
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                };
            }

        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onError(new TestException());

        io.verify(o).onNext("one-1");
        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();

    }

    @Test
    public void testZipIterableNextThrows() {
        PublishProcessor<String> r1 = PublishProcessor.create();
        /* define a Subscriber to receive aggregated events */
        Subscriber<String> o = TestHelper.mockSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public String next() {
                        throw new TestException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                };
            }

        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onError(new TestException());

        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onComplete();

    }

    Function1<String, kotlin.Unit> printer = new Function1<String, kotlin.Unit>() {
        @Override
        public Unit invoke(String pv) {
            System.out.println(pv);
            return Unit.INSTANCE;
        }
    };

    static final class SquareStr implements Function1<Integer, String> {
        final AtomicInteger counter = new AtomicInteger();
        @Override
        public String invoke(Integer t1) {
            counter.incrementAndGet();
            System.out.println("Omg I'm calculating so hard: " + t1 + "*" + t1 + "=" + (t1 * t1));
            return " " + (t1 * t1);
        }
    }

    @Test
    public void testTake2() {
        Flowable<Integer> o = Flowable.just(1, 2, 3, 4, 5);
        Iterable<String> it = Arrays.asList("a", "b", "c", "d", "e");

        SquareStr squareStr = new SquareStr();

        o.map(squareStr).zipWith(it, concat2Strings).take(2).subscribe(printer);

        assertEquals(2, squareStr.counter.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).zipWith(Arrays.asList(1), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return a + b;
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function1<Flowable<Integer>, Flowable<Object>>() {
            @Override
            public Flowable<Object> invoke(Flowable<Integer> o) {
                return o.zipWith(Arrays.asList(1), new Function2<Integer, Integer, Object>() {
                    @Override
                    public Object invoke(Integer a, Integer b) {
                        return a + b;
                    }
                });
            }
        });
    }

    @Test
    public void iteratorThrows() {
        Flowable.just(1).zipWith(new CrashingIterable(100, 1, 100), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return a + b;
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .zipWith(Arrays.asList(1), new Function2<Integer, Integer, Object>() {
                @Override
                public Object invoke(Integer a, Integer b) {
                    return a + b;
                }
            })
            .test()
            .assertResult(2);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
