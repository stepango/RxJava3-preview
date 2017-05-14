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


import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Scheduler;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.TestScheduler;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableDebounceTest {

    private TestScheduler scheduler;
    private Observer<String> observer;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        observer = TestHelper.mockObserver();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testDebounceWithCompleted() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                publishNext(observer, 100, "one");    // Should be skipped since "two" will arrive before the timeout expires.
                publishNext(observer, 400, "two");    // Should be published since "three" will arrive after the timeout expires.
                publishNext(observer, 900, "three");   // Should be skipped since onComplete will arrive before the timeout expires.
                publishCompleted(observer, 1000);     // Should be published as soon as the timeout expires.
            }
        });

        Observable<String> sampled = source.debounce(400, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        // must go to 800 since it must be 400 after when two is sent, which is at 400
        scheduler.advanceTimeTo(800, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testDebounceNeverEmits() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                // all should be skipped since they are happening faster than the 200ms timeout
                publishNext(observer, 100, "a");    // Should be skipped
                publishNext(observer, 200, "b");    // Should be skipped
                publishNext(observer, 300, "c");    // Should be skipped
                publishNext(observer, 400, "d");    // Should be skipped
                publishNext(observer, 500, "e");    // Should be skipped
                publishNext(observer, 600, "f");    // Should be skipped
                publishNext(observer, 700, "g");    // Should be skipped
                publishNext(observer, 800, "h");    // Should be skipped
                publishCompleted(observer, 900);     // Should be published as soon as the timeout expires.
            }
        });

        Observable<String> sampled = source.debounce(200, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(0)).onNext(anyString());
        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testDebounceWithError() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                Exception error = new TestException();
                publishNext(observer, 100, "one");    // Should be published since "two" will arrive after the timeout expires.
                publishNext(observer, 600, "two");    // Should be skipped since onError will arrive before the timeout expires.
                publishError(observer, 700, error);   // Should be published as soon as the timeout expires.
            }
        });

        Observable<String> sampled = source.debounce(400, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        // 100 + 400 means it triggers at 500
        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        inOrder.verify(observer).onNext("one");
        scheduler.advanceTimeTo(701, TimeUnit.MILLISECONDS);
        inOrder.verify(observer).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    private <T> void publishCompleted(final Observer<T> observer, long delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Observer<T> observer, long delay, final Exception error) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Observer<T> observer, final long delay, final T value) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void debounceSelectorNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> debouncer = PublishSubject.create();
        Function1<Integer, Observable<Integer>> debounceSel = new Function1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> invoke(Integer t1) {
                return debouncer;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.debounce(debounceSel).subscribe(o);

        source.onNext(1);
        debouncer.onNext(1);

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);

        debouncer.onNext(2);

        source.onNext(5);
        source.onComplete();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(4);
        inOrder.verify(o).onNext(5);
        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void debounceSelectorFuncThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        Function1<Integer, Observable<Integer>> debounceSel = new Function1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> invoke(Integer t1) {
                throw new TestException();
            }
        };

        Observer<Object> o = TestHelper.mockObserver();

        source.debounce(debounceSel).subscribe(o);

        source.onNext(1);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void debounceSelectorObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        Function1<Integer, Observable<Integer>> debounceSel = new Function1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> invoke(Integer t1) {
                return Observable.error(new TestException());
            }
        };

        Observer<Object> o = TestHelper.mockObserver();

        source.debounce(debounceSel).subscribe(o);

        source.onNext(1);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
    @Test
    public void debounceTimedLastIsNotLost() {
        PublishSubject<Integer> source = PublishSubject.create();

        Observer<Object> o = TestHelper.mockObserver();

        source.debounce(100, TimeUnit.MILLISECONDS, scheduler).subscribe(o);

        source.onNext(1);
        source.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void debounceSelectorLastIsNotLost() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> debouncer = PublishSubject.create();

        Function1<Integer, Observable<Integer>> debounceSel = new Function1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> invoke(Integer t1) {
                return debouncer;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();

        source.debounce(debounceSel).subscribe(o);

        source.onNext(1);
        source.onComplete();

        debouncer.onComplete();

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void debounceWithTimeBackpressure() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        TestObserver<Integer> observer = new TestObserver<Integer>();
        Observable.merge(
                Observable.just(1),
                Observable.just(2).delay(10, TimeUnit.MILLISECONDS, scheduler)
        ).debounce(20, TimeUnit.MILLISECONDS, scheduler).take(1).subscribe(observer);

        scheduler.advanceTimeBy(30, TimeUnit.MILLISECONDS);

        observer.assertValue(2);
        observer.assertTerminated();
        observer.assertNoErrors();
    }

    @Test
    public void debounceDefault() throws Exception {

        Observable.just(1).debounce(1, TimeUnit.SECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().debounce(1, TimeUnit.SECONDS, new TestScheduler()));

        TestHelper.checkDisposed(PublishSubject.create().debounce(Functions.justFunction(Observable.never())));

        Disposable d = new ObservableDebounceTimed.DebounceEmitter<Integer>(1, 1, null);
        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onComplete();
                    observer.onNext(1);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .debounce(1, TimeUnit.SECONDS, new TestScheduler())
            .test()
            .assertResult();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void badSourceSelector() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Integer>, Object>() {
            @Override
            public Object invoke(Observable<Integer> o) {
                return o.debounce(new Function1<Integer, ObservableSource<Long>>() {
                    @Override
                    public ObservableSource<Long> invoke(Integer v) {
                        return Observable.timer(1, TimeUnit.SECONDS);
                    }
                });
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceObservable(new Function1<Observable<Integer>, Object>() {
            @Override
            public Object invoke(final Observable<Integer> o) {
                return Observable.just(1).debounce(new Function1<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> invoke(Integer v) {
                        return o;
                    }
                });
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void debounceWithEmpty() {
        Observable.just(1).debounce(Functions.justFunction(Observable.empty()))
        .test()
        .assertResult(1);
    }
}
