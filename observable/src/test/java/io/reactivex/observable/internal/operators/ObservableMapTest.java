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

import java.util.HashMap;
import java.util.Map;

import io.reactivex.common.Schedulers;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.QueueDisposable;
import io.reactivex.observable.observers.ObserverFusion;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.UnicastSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableMapTest {

    Observer<String> stringObserver;
    Observer<String> stringObserver2;

    static final Function2<String, Integer, String> APPEND_INDEX = new Function2<String, Integer, String>() {
        @Override
        public String invoke(String value, Integer index) {
            return value + index;
        }
    };

    @Before
    public void before() {
        stringObserver = TestHelper.mockObserver();
        stringObserver2 = TestHelper.mockObserver();
    }

    @Test
    public void testMap() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Observable<Map<String, String>> o = Observable.just(m1, m2);

        Observable<String> m = o.map(new Function1<Map<String, String>, String>() {
            @Override
            public String invoke(Map<String, String> map) {
                return map.get("firstName");
            }
        });

        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onNext("OneFirst");
        verify(stringObserver, times(1)).onNext("TwoFirst");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void testMapMany() {
        /* simulate a top-level async call which returns IDs */
        Observable<Integer> ids = Observable.just(1, 2);

        /* now simulate the behavior to take those IDs and perform nested async calls based on them */
        Observable<String> m = ids.flatMap(new Function1<Integer, Observable<String>>() {

            @Override
            public Observable<String> invoke(Integer id) {
                /* simulate making a nested async call which creates another Observable */
                Observable<Map<String, String>> subObservable = null;
                if (id == 1) {
                    Map<String, String> m1 = getMap("One");
                    Map<String, String> m2 = getMap("Two");
                    subObservable = Observable.just(m1, m2);
                } else {
                    Map<String, String> m3 = getMap("Three");
                    Map<String, String> m4 = getMap("Four");
                    subObservable = Observable.just(m3, m4);
                }

                /* simulate kicking off the async call and performing a select on it to transform the data */
                return subObservable.map(new Function1<Map<String, String>, String>() {
                    @Override
                    public String invoke(Map<String, String> map) {
                        return map.get("firstName");
                    }
                });
            }

        });
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onNext("OneFirst");
        verify(stringObserver, times(1)).onNext("TwoFirst");
        verify(stringObserver, times(1)).onNext("ThreeFirst");
        verify(stringObserver, times(1)).onNext("FourFirst");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void testMapMany2() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Observable<Map<String, String>> observable1 = Observable.just(m1, m2);

        Map<String, String> m3 = getMap("Three");
        Map<String, String> m4 = getMap("Four");
        Observable<Map<String, String>> observable2 = Observable.just(m3, m4);

        Observable<Observable<Map<String, String>>> o = Observable.just(observable1, observable2);

        Observable<String> m = o.flatMap(new Function1<Observable<Map<String, String>>, Observable<String>>() {

            @Override
            public Observable<String> invoke(Observable<Map<String, String>> o) {
                return o.map(new Function1<Map<String, String>, String>() {

                    @Override
                    public String invoke(Map<String, String> map) {
                        return map.get("firstName");
                    }
                });
            }

        });
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onNext("OneFirst");
        verify(stringObserver, times(1)).onNext("TwoFirst");
        verify(stringObserver, times(1)).onNext("ThreeFirst");
        verify(stringObserver, times(1)).onNext("FourFirst");
        verify(stringObserver, times(1)).onComplete();

    }

    @Test
    public void testMapWithError() {
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");
        Observable<String> m = w.map(new Function1<String, String>() {
            @Override
            public String invoke(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                return s;
            }
        }).doOnError(new Function1<Throwable, kotlin.Unit>() {

            @Override
            public Unit invoke(Throwable t1) {
                t1.printStackTrace();
                return Unit.INSTANCE;
            }

        });

        m.subscribe(stringObserver);
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, never()).onNext("two");
        verify(stringObserver, never()).onNext("three");
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onError(any(Throwable.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapWithIssue417() {
        Observable.just(1).observeOn(Schedulers.computation())
                .map(new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                }).blockingSingle();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapWithErrorInFuncAndThreadPoolScheduler() throws InterruptedException {
        // The error will throw in one of threads in the thread pool.
        // If map does not handle it, the error will disappear.
        // so map needs to handle the error by itself.
        Observable<String> m = Observable.just("one")
                .observeOn(Schedulers.computation())
                .map(new Function1<String, String>() {
                    @Override
                    public String invoke(String arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                });

        // block for response, expecting exception thrown
        m.blockingLast();
    }

    /**
     * While mapping over range(1,0).last() we expect NoSuchElementException since the sequence is empty.
     */
    @Test
    public void testErrorPassesThruMap() {
        assertNull(Observable.range(1, 0).lastElement().map(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer i) {
                return i;
            }

        }).blockingGet());
    }

    /**
     * We expect IllegalStateException to pass thru map.
     */
    @Test(expected = IllegalStateException.class)
    public void testErrorPassesThruMap2() {
        Observable.error(new IllegalStateException()).map(new Function1<Object, Object>() {

            @Override
            public Object invoke(Object i) {
                return i;
            }

        }).blockingSingle();
    }

    /**
     * We expect an ArithmeticException exception here because last() emits a single value
     * but then we divide by 0.
     */
    @Test(expected = ArithmeticException.class)
    public void testMapWithErrorInFunc() {
        Observable.range(1, 1).lastElement().map(new Function1<Integer, Integer>() {

            @Override
            public Integer invoke(Integer i) {
                return i / 0;
            }

        }).blockingGet();
    }

    // FIXME RS subscribers can't throw
//    @Test(expected = OnErrorNotImplementedException.class)
//    public void verifyExceptionIsThrownIfThereIsNoExceptionHandler() {
//
//        ObservableSource<Object> creator = new ObservableSource<Object>() {
//
//            @Override
//            public void subscribeActual(Observer<? super Object> observer) {
//                observer.onSubscribe(EmptyDisposable.INSTANCE);
//                observer.onNext("a");
//                observer.onNext("b");
//                observer.onNext("c");
//                observer.onComplete();
//            }
//        };
//
//        Function<Object, Observable<Object>> manyMapper = new Function<Object, Observable<Object>>() {
//
//            @Override
//            public Observable<Object> apply(Object object) {
//                return Observable.just(object);
//            }
//        };
//
//        Function<Object, Object> mapper = new Function<Object, Object>() {
//            private int count = 0;
//
//            @Override
//            public Object apply(Object object) {
//                ++count;
//                if (count > 2) {
//                    throw new RuntimeException();
//                }
//                return object;
//            }
//        };
//
//        Consumer<Object> onNext = new Consumer<Object>() {
//
//            @Override
//            public void accept(Object object) {
//                System.out.println(object.toString());
//            }
//        };
//
//        try {
//            Observable.unsafeCreate(creator).flatMap(manyMapper).map(mapper).subscribe(onNext);
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            throw e;
//        }
//    }

    private static Map<String, String> getMap(String prefix) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("firstName", prefix + "First");
        m.put("lastName", prefix + "Last");
        return m;
    }

    // FIXME RS subscribers can't throw
//    @Test(expected = OnErrorNotImplementedException.class)
//    public void testShouldNotSwallowOnErrorNotImplementedException() {
//        Observable.just("a", "b").flatMap(new Function<String, Observable<String>>() {
//            @Override
//            public Observable<String> apply(String s) {
//                return Observable.just(s + "1", s + "2");
//            }
//        }).flatMap(new Function<String, Observable<String>>() {
//            @Override
//            public Observable<String> apply(String s) {
//                return Observable.error(new Exception("test"));
//            }
//        }).forEach(new Consumer<String>() {
//            @Override
//            public void accept(String s) {
//                System.out.println(s);
//            }
//        });
//    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 5).map(Functions.identity()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Object> o) {
                return o.map(Functions.identity());
            }
        });
    }

    @Test
    public void fusedSync() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        Observable.range(1, 5)
        .map(Functions.<Integer>identity())
        .subscribe(to);

        ObserverFusion.assertFusion(to, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedAsync() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        UnicastSubject<Integer> us = UnicastSubject.create();

        us
        .map(Functions.<Integer>identity())
        .subscribe(to);

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        ObserverFusion.assertFusion(to, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedReject() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY | QueueDisposable.BOUNDARY);

        Observable.range(1, 5)
        .map(Functions.<Integer>identity())
        .subscribe(to);

        ObserverFusion.assertFusion(to, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Object>, Object>() {
            @Override
            public Object invoke(Observable<Object> o) {
                return o.map(Functions.identity());
            }
        }, false, 1, 1, 1);
    }
}
