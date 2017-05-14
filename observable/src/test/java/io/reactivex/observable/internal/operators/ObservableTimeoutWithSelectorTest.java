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
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ObservableTimeoutWithSelectorTest {
    @Test(timeout = 2000)
    public void testTimeoutSelectorNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.fromIterable(Arrays.asList(100));

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.timeout(timeout, timeoutFunc, other).subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        timeout.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testTimeoutSelectorTimeoutFirst() throws InterruptedException {
        Observable<Integer> source = Observable.<Integer>never();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.fromIterable(Arrays.asList(100));

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.timeout(timeout, timeoutFunc, other).subscribe(o);

        timeout.onNext(1);

        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testTimeoutSelectorFirstThrows() {
        Observable<Integer> source = Observable.<Integer>never();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return timeout;
            }
        };

        Callable<Observable<Integer>> firstTimeoutFunc = new Callable<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                throw new TestException();
            }
        };

        Observable<Integer> other = Observable.fromIterable(Arrays.asList(100));

        Observer<Object> o = TestHelper.mockObserver();

        source.timeout(Observable.defer(firstTimeoutFunc), timeoutFunc, other).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();

    }

    @Test
    public void testTimeoutSelectorSubsequentThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                throw new TestException();
            }
        };

        Observable<Integer> other = Observable.fromIterable(Arrays.asList(100));

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.timeout(timeout, timeoutFunc, other).subscribe(o);

        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();

    }

    @Test
    public void testTimeoutSelectorFirstObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.fromIterable(Arrays.asList(100));

        Observer<Object> o = TestHelper.mockObserver();

        source.timeout(Observable.<Integer> error(new TestException()), timeoutFunc, other).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();

    }

    @Test
    public void testTimeoutSelectorSubsequentObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return Observable.<Integer> error(new TestException());
            }
        };

        Observable<Integer> other = Observable.fromIterable(Arrays.asList(100));

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.timeout(timeout, timeoutFunc, other).subscribe(o);

        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();

    }

    @Test
    public void testTimeoutSelectorWithFirstTimeoutFirstAndNoOtherObservable() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return PublishSubject.create();
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        source.timeout(timeout, timeoutFunc).subscribe(o);

        timeout.onNext(1);

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTimeoutSelectorWithTimeoutFirstAndNoOtherObservable() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return timeout;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        source.timeout(PublishSubject.create(), timeoutFunc).subscribe(o);
        source.onNext(1);

        timeout.onNext(1);

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTimeoutSelectorWithTimeoutAndOnNextRaceCondition() throws InterruptedException {
        // Thread 1                                    Thread 2
        //
        // observer.onNext(1)
        // start timeout
        // unsubscribe timeout in thread 2          start to do some long-time work in "unsubscribe"
        // observer.onNext(2)
        // timeout.onNext(1)
        //                                          "unsubscribe" done
        //
        //
        // In the above case, the timeout operator should ignore "timeout.onNext(1)"
        // since "observer" has already seen 2.
        final CountDownLatch observerReceivedTwo = new CountDownLatch(1);
        final CountDownLatch timeoutEmittedOne = new CountDownLatch(1);
        final CountDownLatch observerCompleted = new CountDownLatch(1);
        final CountDownLatch enteredTimeoutOne = new CountDownLatch(1);
        final AtomicBoolean latchTimeout = new AtomicBoolean(false);

        final Function1<Integer, Observable<Integer>> timeoutFunc = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                if (t1 == 1) {
                    // Force "unsubscribe" run on another thread
                    return Observable.unsafeCreate(new ObservableSource<Integer>() {
                        @Override
                        public void subscribe(Observer<? super Integer> observer) {
                            observer.onSubscribe(Disposables.empty());
                            enteredTimeoutOne.countDown();
                            // force the timeout message be sent after observer.onNext(2)
                            while (true) {
                                try {
                                    if (!observerReceivedTwo.await(30, TimeUnit.SECONDS)) {
                                        // CountDownLatch timeout
                                        // There should be something wrong
                                        latchTimeout.set(true);
                                    }
                                    break;
                                } catch (InterruptedException e) {
                                    // Since we just want to emulate a busy method,
                                    // we ignore the interrupt signal from Scheduler.
                                }
                            }
                            observer.onNext(1);
                            timeoutEmittedOne.countDown();
                        }
                    }).subscribeOn(Schedulers.newThread());
                } else {
                    return PublishSubject.create();
                }
            }
        };

        final Observer<Integer> o = TestHelper.mockObserver();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                observerReceivedTwo.countDown();
                return null;
            }

        }).when(o).onNext(2);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                observerCompleted.countDown();
                return null;
            }

        }).when(o).onComplete();

        final TestObserver<Integer> ts = new TestObserver<Integer>(o);

        new Thread(new Runnable() {

            @Override
            public void run() {
                PublishSubject<Integer> source = PublishSubject.create();
                source.timeout(timeoutFunc, Observable.just(3)).subscribe(ts);
                source.onNext(1); // start timeout
                try {
                    if (!enteredTimeoutOne.await(30, TimeUnit.SECONDS)) {
                        latchTimeout.set(true);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                source.onNext(2); // disable timeout
                try {
                    if (!timeoutEmittedOne.await(30, TimeUnit.SECONDS)) {
                        latchTimeout.set(true);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                source.onComplete();
            }

        }).start();

        if (!observerCompleted.await(30, TimeUnit.SECONDS)) {
            latchTimeout.set(true);
        }

        assertFalse("CoundDownLatch timeout", latchTimeout.get());

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onSubscribe((Disposable)notNull());
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o, never()).onNext(3);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().timeout(Functions.justFunction(Observable.never())));

        TestHelper.checkDisposed(PublishSubject.create().timeout(Functions.justFunction(Observable.never()), Observable.never()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Object> o) {
                return o.timeout(Functions.justFunction(Observable.never()));
            }
        });

        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Object> o) {
                return o.timeout(Functions.justFunction(Observable.never()), Observable.never());
            }
        });
    }

    @Test
    public void empty() {
        Observable.empty()
        .timeout(Functions.justFunction(Observable.never()))
        .test()
        .assertResult();
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .timeout(Functions.justFunction(Observable.never()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyInner() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
        .timeout(Functions.justFunction(Observable.empty()))
        .test();

        ps.onNext(1);

        to.assertFailure(TimeoutException.class, 1);
    }

    @Test
    public void badInnerSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            PublishSubject<Integer> ps = PublishSubject.create();

            TestObserver<Integer> to = ps
            .timeout(Functions.justFunction(new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new TestException("First"));
                    observer.onNext(2);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }))
            .test();

            ps.onNext(1);

            to.assertFailureAndMessage(TestException.class, "First", 1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void badInnerSourceOther() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            PublishSubject<Integer> ps = PublishSubject.create();

            TestObserver<Integer> to = ps
            .timeout(Functions.justFunction(new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new TestException("First"));
                    observer.onNext(2);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }), Observable.just(2))
            .test();

            ps.onNext(1);

            to.assertFailureAndMessage(TestException.class, "First", 1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void withOtherMainError() {
        Observable.error(new TestException())
        .timeout(Functions.justFunction(Observable.never()), Observable.never())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badSourceTimeout() {
        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onNext(1);
                observer.onNext(2);
                observer.onError(new TestException("First"));
                observer.onNext(3);
                observer.onComplete();
                observer.onError(new TestException("Second"));
            }
        }
        .timeout(Functions.justFunction(Observable.never()), Observable.<Integer>never())
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void selectorTake() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
        .timeout(Functions.justFunction(Observable.never()))
        .take(1)
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        to.assertResult(1);
    }

    @Test
    public void selectorFallbackTake() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps
        .timeout(Functions.justFunction(Observable.never()), Observable.just(2))
        .take(1)
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        to.assertResult(1);
    }
}
