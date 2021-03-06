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

package io.reactivex.observable;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Disposable;
import io.reactivex.common.Emitter;
import io.reactivex.common.Notification;
import io.reactivex.common.Scheduler;
import io.reactivex.common.Schedulers;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Verifies the operators handle null values properly by emitting/throwing NullPointerExceptions.
 */
public class ObservableNullTests {

    Observable<Integer> just1 = Observable.just(1);

    //***********************************************************
    // Static methods
    //***********************************************************

    @Test(expected = NullPointerException.class)
    public void ambVarargsNull() {
        Observable.ambArray((Observable<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void ambVarargsOneIsNull() {
        Observable.ambArray(Observable.never(), null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void ambIterableNull() {
        Observable.amb((Iterable<Observable<Object>>)null);
    }

    @Test
    public void ambIterableIteratorNull() {
        Observable.amb(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }).test().assertError(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterableOneIsNull() {
        Observable.amb(Arrays.asList(Observable.never(), null))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestVarargsNull() {
        Observable.combineLatest(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128, (Observable<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestVarargsOneIsNull() {
        Observable.combineLatest(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128, Observable.never(), null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableNull() {
        Observable.combineLatest((Iterable<Observable<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableIteratorNull() {
        Observable.combineLatest(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestIterableOneIsNull() {
        Observable.combineLatest(Arrays.asList(Observable.never(), null), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestVarargsFunctionNull() {
        Observable.combineLatest(null, 128, Observable.never());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestVarargsFunctionReturnsNull() {
        Observable.combineLatest(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }, 128, just1).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestIterableFunctionNull() {
        Observable.combineLatest(Arrays.asList(just1), null, 128);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestIterableFunctionReturnsNull() {
        Observable.combineLatest(Arrays.asList(just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorVarargsNull() {
        Observable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128, (Observable<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorVarargsOneIsNull() {
        Observable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128, Observable.never(), null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableNull() {
        Observable.combineLatestDelayError((Iterable<Observable<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableIteratorNull() {
        Observable.combineLatestDelayError(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableOneIsNull() {
        Observable.combineLatestDelayError(Arrays.asList(Observable.never(), null), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorVarargsFunctionNull() {
        Observable.combineLatestDelayError(null, 128, Observable.never());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorVarargsFunctionReturnsNull() {
        Observable.combineLatestDelayError(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }, 128, just1).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableFunctionNull() {
        Observable.combineLatestDelayError(Arrays.asList(just1), null, 128);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableFunctionReturnsNull() {
        Observable.combineLatestDelayError(Arrays.asList(just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableNull() {
        Observable.concat((Iterable<Observable<Object>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Observable.concat(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void concatIterableOneIsNull() {
        Observable.concat(Arrays.asList(just1, null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatObservableNull() {
        Observable.concat((Observable<Observable<Object>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void concatArrayNull() {
        Observable.concatArray((Observable<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void concatArrayOneIsNull() {
        Observable.concatArray(just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void createNull() {
        Observable.unsafeCreate(null);
    }

    @Test(expected = NullPointerException.class)
    public void deferFunctionNull() {
        Observable.defer(null);
    }

    @Test(expected = NullPointerException.class)
    public void deferFunctionReturnsNull() {
        Observable.defer(new Callable<Observable<Object>>() {
            @Override
            public Observable<Object> call() {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void errorFunctionNull() {
        Observable.error((Callable<Throwable>)null);
    }

    @Test(expected = NullPointerException.class)
    public void errorFunctionReturnsNull() {
        Observable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void errorThrowableNull() {
        Observable.error((Throwable)null);
    }

    @Test(expected = NullPointerException.class)
    public void fromArrayNull() {
        Observable.fromArray((Object[])null);
    }

    @Test(expected = NullPointerException.class)
    public void fromArrayOneIsNull() {
        Observable.fromArray(1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableNull() {
        Observable.fromCallable(null);
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableReturnsNull() {
        Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureNull() {
        Observable.fromFuture(null);
    }

    @Test
    public void fromFutureReturnsNull() {
        FutureTask<Object> f = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        f.run();

        TestObserver<Object> ts = new TestObserver<Object>();
        Observable.fromFuture(f).subscribe(ts);
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedFutureNull() {
        Observable.fromFuture(null, 1, TimeUnit.SECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedUnitNull() {
        Observable.fromFuture(new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null), 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedSchedulerNull() {
        Observable.fromFuture(new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null), 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedReturnsNull() {
        FutureTask<Object> f = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        f.run();
        Observable.fromFuture(f, 1, TimeUnit.SECONDS).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureSchedulerNull() {
        FutureTask<Object> f = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        Observable.fromFuture(f, null);
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableNull() {
        Observable.fromIterable(null);
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableIteratorNull() {
        Observable.fromIterable(new Iterable<Object>() {
            @Override
            public Iterator<Object> iterator() {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableValueNull() {
        Observable.fromIterable(Arrays.asList(1, null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void generateConsumerNull() {
        Observable.generate(null);
    }

    @Test(expected = NullPointerException.class)
    public void generateConsumerEmitsNull() {
        Observable.generate(new Function1<Emitter<Object>, kotlin.Unit>() {
            @Override
            public Unit invoke(Emitter<Object> s) {
                s.onNext(null);
                return Unit.INSTANCE;
            }
        }).blockingLast();
    }

//    @Test(expected = NullPointerException.class)
//    public void generateStateConsumerInitialStateNull() {
//        Function2<Integer, Emitter<Integer>, kotlin.Unit> generator = new Function2<Integer, Emitter<Integer>, kotlin.Unit>() {
//            @Override
//            public Unit invoke(Integer s, Emitter<Integer> o) {
//                o.onNext(1);
//                return Unit.INSTANCE;
//            }
//        };
//        Observable.generate(null, generator);
//    }

    @Test(expected = NullPointerException.class)
    public void generateStateFunctionInitialStateNull() {
        Observable.generate(null, new Function2<Object, Emitter<Object>, Object>() {
            @Override
            public Object invoke(Object s, Emitter<Object> o) { o.onNext(1); return s; }
        });
    }

//    @Test(expected = NullPointerException.class)
//    public void generateStateConsumerNull() {
//        Observable.generate(new Callable<Integer>() {
//            @Override
//            public Integer call() {
//                return 1;
//            }
//        }, (Function2<Integer, Emitter<Object>, kotlin.Unit>) null);
//    }
//
//    @Test
//    public void generateConsumerStateNullAllowed() {
//        Function2<Integer, Emitter<Integer>, kotlin.Unit> generator = new Function2<Integer, Emitter<Integer>, kotlin.Unit>() {
//            @Override
//            public Unit invoke(Integer s, Emitter<Integer> o) {
//                o.onComplete();
//                return Unit.INSTANCE;
//            }
//        };
//        Observable.generate(new Callable<Integer>() {
//            @Override
//            public Integer call() {
//                return null;
//            }
//        }, generator).blockingSubscribe();
//    }

    @Test
    public void generateFunctionStateNullAllowed() {
        Observable.generate(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }, new Function2<Object, Emitter<Object>, Object>() {
            @Override
            public Object invoke(Object s, Emitter<Object> o) { o.onComplete(); return s; }
        }).blockingSubscribe();
    }

//    @Test(expected = NullPointerException.class)
//    public void generateConsumerDisposeNull() {
//        Function2<Integer, Emitter<Integer>, kotlin.Unit> generator = new Function2<Integer, Emitter<Integer>, kotlin.Unit>() {
//            @Override
//            public Unit invoke(Integer s, Emitter<Integer> o) {
//                o.onNext(1);
//                return Unit.INSTANCE;
//            }
//        };
//        Observable.generate(new Callable<Integer>() {
//            @Override
//            public Integer call() {
//                return 1;
//            }
//        }, generator, null);
//    }

    @Test(expected = NullPointerException.class)
    public void generateFunctionDisposeNull() {
        Observable.generate(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function2<Object, Emitter<Object>, Object>() {
            @Override
            public Object invoke(Object s, Emitter<Object> o) { o.onNext(1); return s; }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalUnitNull() {
        Observable.interval(1, null);
    }

    public void intervalSchedulerNull() {
        Observable.interval(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalPeriodUnitNull() {
        Observable.interval(1, 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalPeriodSchedulerNull() {
        Observable.interval(1, 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalRangeUnitNull() {
        Observable.intervalRange(1,1, 1, 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void intervalRangeSchedulerNull() {
        Observable.intervalRange(1, 1, 1, 1, TimeUnit.SECONDS, null);
    }

    @Test
    public void justNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Observable> clazz = Observable.class;
        for (int argCount = 1; argCount < 10; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount];
                Arrays.fill(params, Object.class);

                Object[] values = new Object[argCount];
                Arrays.fill(values, 1);
                values[argNull - 1] = null;

                Method m = clazz.getMethod("just", params);

                try {
                    m.invoke(null, values);
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableNull() {
        Observable.merge((Iterable<Observable<Object>>)null, 128, 128);
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableIteratorNull() {
        Observable.merge(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, 128, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeIterableOneIsNull() {
        Observable.merge(Arrays.asList(just1, null), 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeArrayNull() {
        Observable.mergeArray(128, 128, (Observable<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeArrayOneIsNull() {
        Observable.mergeArray(128, 128, just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableNull() {
        Observable.mergeDelayError((Iterable<Observable<Object>>)null, 128, 128);
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableIteratorNull() {
        Observable.mergeDelayError(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, 128, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableOneIsNull() {
        Observable.mergeDelayError(Arrays.asList(just1, null), 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorArrayNull() {
        Observable.mergeArrayDelayError(128, 128, (Observable<Object>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorArrayOneIsNull() {
        Observable.mergeArrayDelayError(128, 128, just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void sequenceEqualFirstNull() {
        Observable.sequenceEqual(null, just1);
    }

    @Test(expected = NullPointerException.class)
    public void sequenceEqualSecondNull() {
        Observable.sequenceEqual(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void sequenceEqualComparatorNull() {
        Observable.sequenceEqual(just1, just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void switchOnNextNull() {
        Observable.switchOnNext(null);
    }

    @Test(expected = NullPointerException.class)
    public void timerUnitNull() {
        Observable.timer(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timerSchedulerNull() {
        Observable.timer(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void usingResourceSupplierNull() {
        Observable.using(null, new Function1<Object, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Object d) {
                return just1;
            }
        }, new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void usingObservableSupplierNull() {
        Observable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null, new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void usingObservableSupplierReturnsNull() {
        Observable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function1<Object, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Object d) {
                return null;
            }
        }, new Function1<Object, kotlin.Unit>() {
            @Override
            public Unit invoke(Object d) {
                return Unit.INSTANCE;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void usingDisposeNull() {
        Observable.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function1<Object, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Object d) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableNull() {
        Observable.zip((Iterable<Observable<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Observable.zip(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableFunctionNull() {
        Observable.zip(Arrays.asList(just1, just1), null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableFunctionReturnsNull() {
        Observable.zip(Arrays.asList(just1, just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipObservableNull() {
        Observable.zip((Observable<Observable<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipObservableFunctionNull() {
        Observable.zip((Observable.just(just1)), null);
    }

    @Test(expected = NullPointerException.class)
    public void zipObservableFunctionReturnsNull() {
        Observable.zip((Observable.just(just1)), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return null;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2Null() {
        Observable.zipIterable((Iterable<Observable<Object>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return 1;
            }
        }, true, 128);
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2IteratorNull() {
        Observable.zipIterable(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return 1;
            }
        }, true, 128).blockingLast();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterable2FunctionNull() {
        Observable.zipIterable(Arrays.asList(just1, just1), null, true, 128);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterable2FunctionReturnsNull() {
        Observable.zipIterable(Arrays.asList(just1, just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] a) {
                return null;
            }
        }, true, 128).blockingLast();
    }

    //*************************************************************
    // Instance methods
    //*************************************************************

    @Test(expected = NullPointerException.class)
    public void allPredicateNull() {
        just1.all(null);
    }

    @Test(expected = NullPointerException.class)
    public void ambWithNull() {
        just1.ambWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void anyPredicateNull() {
        just1.any(null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferSupplierNull() {
        just1.buffer(1, 1, (Callable<List<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferSupplierReturnsNull() {
        just1.buffer(1, 1, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedUnitNull() {
        just1.buffer(1L, 1L, null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedSchedulerNull() {
        just1.buffer(1L, 1L, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedSupplierNull() {
        just1.buffer(1L, 1L, TimeUnit.SECONDS, Schedulers.single(), null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedSupplierReturnsNull() {
        just1.buffer(1L, 1L, TimeUnit.SECONDS, Schedulers.single(), new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferOpenCloseOpenNull() {
        just1.buffer(null, new Function1<Object, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Object o) {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void bufferOpenCloseCloseNull() {
        just1.buffer(just1, (Function1<Integer, Observable<Object>>) null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferOpenCloseCloseReturnsNull() {
        just1.buffer(just1, new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundaryNull() {
        just1.buffer((Observable<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplierNull() {
        just1.buffer(just1, (Callable<List<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplierReturnsNull() {
        just1.buffer(just1, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplier2Null() {
        just1.buffer((Callable<Observable<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplier2ReturnsNull() {
        just1.buffer(new Callable<Observable<Object>>() {
            @Override
            public Observable<Object> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplier2SupplierNull() {
        just1.buffer(new Callable<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplier2SupplierReturnsNull() {
        just1.buffer(new Callable<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return just1;
            }
        }, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void castNull() {
        just1.cast(null);
    }

    @Test(expected = NullPointerException.class)
    public void collectInitialSupplierNull() {
        just1.collect((Callable<Integer>) null, new Function2<Integer, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer a, Integer b) {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void collectInitialSupplierReturnsNull() {
        just1.collect(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }, new Function2<Object, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Object a, Integer b) {
                return Unit.INSTANCE;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void collectInitialCollectorNull() {
        just1.collect(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void collectIntoInitialNull() {
        just1.collectInto(null, new Function2<Object, Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Object a, Integer b) {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void collectIntoCollectorNull() {
        just1.collectInto(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void composeNull() {
        just1.compose(null);
    }

    @Test(expected = NullPointerException.class)
    public void concatMapNull() {
        just1.concatMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void concatMapReturnsNull() {
        just1.concatMap(new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatMapIterableNull() {
        just1.concatMapIterable(null);
    }

    @Test(expected = NullPointerException.class)
    public void concatMapIterableReturnNull() {
        just1.concatMapIterable(new Function1<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatMapIterableIteratorNull() {
        just1.concatMapIterable(new Function1<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> invoke(Integer v) {
                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        return null;
                    }
                };
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatWithNull() {
        just1.concatWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void containsNull() {
        just1.contains(null);
    }

    @Test(expected = NullPointerException.class)
    public void debounceFunctionNull() {
        just1.debounce(null);
    }

    @Test(expected = NullPointerException.class)
    public void debounceFunctionReturnsNull() {
        just1.debounce(new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void debounceTimedUnitNull() {
        just1.debounce(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void debounceTimedSchedulerNull() {
        just1.debounce(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void defaultIfEmptyNull() {
        just1.defaultIfEmpty(null);
    }

    @Test(expected = NullPointerException.class)
    public void delayWithFunctionNull() {
        just1.delay(null);
    }

    @Test(expected = NullPointerException.class)
    public void delayWithFunctionReturnsNull() {
        just1.delay(new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void delayTimedUnitNull() {
        just1.delay(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delayTimedSchedulerNull() {
        just1.delay(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionTimedUnitNull() {
        just1.delaySubscription(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionTimedSchedulerNull() {
        just1.delaySubscription(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionOtherNull() {
        just1.delaySubscription((Observable<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySubscriptionFunctionNull() {
        just1.delaySubscription((Observable<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void delayBothInitialSupplierNull() {
        just1.delay(null, new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void delayBothInitialSupplierReturnsNull() {
        just1.delay(null, new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void delayBothItemSupplierNull() {
        just1.delay(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delayBothItemSupplierReturnsNull() {
        just1.delay(just1
                , new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void distinctFunctionNull() {
        just1.distinct(null);
    }

    @Test(expected = NullPointerException.class)
    public void distinctSupplierNull() {
        just1.distinct(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void distinctSupplierReturnsNull() {
        just1.distinct(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void distinctFunctionReturnsNull() {
        just1.distinct(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void distinctUntilChangedFunctionNull() {
        just1.distinctUntilChanged((Function1<Object, Object>) null);
    }

    @Test(expected = NullPointerException.class)
    public void distinctUntilChangedBiPredicateNull() {
        just1.distinctUntilChanged((Function2<Object, Object, Boolean>) null);
    }

    @Test
    public void distinctUntilChangedFunctionReturnsNull() {
        Observable.range(1, 2).distinctUntilChanged(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).test().assertResult(1);
    }

    @Test(expected = NullPointerException.class)
    public void doOnDisposeNull() {
        just1.doOnDispose(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnCompleteNull() {
        just1.doOnComplete(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnEachSupplierNull() {
        just1.doOnEach((Function1<Notification<Integer>, kotlin.Unit>) null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnEachSubscriberNull() {
        just1.doOnEach((Observer<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnErrorNull() {
        just1.doOnError(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnLifecycleOnSubscribeNull() {
        just1.doOnLifecycle(null, Functions.EMPTY_ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void doOnLifecycleOnDisposeNull() {
        just1.doOnLifecycle(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                return Unit.INSTANCE;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnNextNull() {
        just1.doOnNext(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnSubscribeNull() {
        just1.doOnSubscribe(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnTerminatedNull() {
        just1.doOnTerminate(null);
    }

    @Test(expected = NullPointerException.class)
    public void elementAtNull() {
        just1.elementAt(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void filterNull() {
        just1.filter(null);
    }

    @Test(expected = NullPointerException.class)
    public void doAfterTerminateNull() {
        just1.doAfterTerminate(null);
    }

    @Test(expected = NullPointerException.class)
    public void firstNull() {
        just1.first(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNull() {
        just1.flatMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapFunctionReturnsNull() {
        just1.flatMap(new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnNextNull() {
        just1.flatMap(null, new Function1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Throwable e) {
                return just1;
            }
        }, new Callable<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnNextReturnsNull() {
        just1.flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return null;
            }
        }, new Function1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Throwable e) {
                return just1;
            }
        }, new Callable<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return just1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnErrorNull() {
        just1.flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        }, null, new Callable<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnErrorReturnsNull() {
        Observable.error(new TestException()).flatMap(new Function1<Object, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Object v) {
                return just1;
            }
        }, new Function1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Throwable e) {
                return null;
            }
        }, new Callable<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return just1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnCompleteNull() {
        just1.flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        }, new Function1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Throwable e) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnCompleteReturnsNull() {
        just1.flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        }, new Function1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Throwable e) {
                return just1;
            }
        }, new Callable<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerMapperNull() {
        just1.flatMap(null, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerMapperReturnsNull() {
        just1.flatMap(new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerCombinerNull() {
        just1.flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerCombinerReturnsNull() {
        just1.flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        }, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperNull() {
        just1.flatMapIterable(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperReturnsNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperIteratorNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> invoke(Integer v) {
                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        return null;
                    }
                };
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperIterableOneNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(1, null);
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableCombinerNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(1);
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableCombinerReturnsNull() {
        just1.flatMapIterable(new Function1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> invoke(Integer v) {
                return Arrays.asList(1);
            }
        }, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void forEachNull() {
        just1.forEach(null);
    }

    @Test(expected = NullPointerException.class)
    public void forEachWhileNull() {
        just1.forEachWhile(null);
    }

    @Test(expected = NullPointerException.class)
    public void forEachWhileOnErrorNull() {
        just1.forEachWhile(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void forEachWhileOnCompleteNull() {
        just1.forEachWhile(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                return Unit.INSTANCE;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void groupByNull() {
        just1.groupBy(null);
    }

    public void groupByKeyNull() {
        just1.groupBy(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void groupByValueNull() {
        just1.groupBy(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void groupByValueReturnsNull() {
        just1.groupBy(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void lastNull() {
        just1.last(null);
    }

    @Test(expected = NullPointerException.class)
    public void liftNull() {
        just1.lift(null);
    }

    @Test(expected = NullPointerException.class)
    public void liftReturnsNull() {
        just1.lift(new ObservableOperator<Object, Integer>() {
            @Override
            public Observer<? super Integer> apply(Observer<? super Object> s) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mapNull() {
        just1.map(null);
    }

    @Test(expected = NullPointerException.class)
    public void mapReturnsNull() {
        just1.map(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mergeWithNull() {
        just1.mergeWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void observeOnNull() {
        just1.observeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void ofTypeNull() {
        just1.ofType(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextFunctionNull() {
        just1.onErrorResumeNext((Function1<Throwable, Observable<Integer>>) null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextFunctionReturnsNull() {
        Observable.error(new TestException()).onErrorResumeNext(new Function1<Throwable, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Throwable e) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextObservableNull() {
        just1.onErrorResumeNext((Observable<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnFunctionNull() {
        just1.onErrorReturn(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnValueNull() {
        just1.onErrorReturnItem(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnFunctionReturnsNull() {
        Observable.error(new TestException()).onErrorReturn(new Function1<Throwable, Object>() {
            @Override
            public Object invoke(Throwable e) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void onExceptionResumeNext() {
        just1.onExceptionResumeNext(null);
    }

    @Test(expected = NullPointerException.class)
    public void publishFunctionNull() {
        just1.publish(null);
    }

    @Test(expected = NullPointerException.class)
    public void publishFunctionReturnsNull() {
        just1.publish(new Function1<Observable<Integer>, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Observable<Integer> v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void reduceFunctionNull() {
        just1.reduce(null);
    }

    @Test(expected = NullPointerException.class)
    public void reduceFunctionReturnsNull() {
        Observable.just(1, 1).reduce(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void reduceSeedNull() {
        just1.reduce(null, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void reduceSeedFunctionNull() {
        just1.reduce(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void reduceSeedFunctionReturnsNull() {
        just1.reduce(1, new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void reduceWithSeedNull() {
        just1.reduceWith(null, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void reduceWithSeedReturnsNull() {
        just1.reduceWith(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void repeatUntilNull() {
        just1.repeatUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenNull() {
        just1.repeatWhen(null);
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenFunctionReturnsNull() {
        just1.repeatWhen(new Function1<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Observable<Object> v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorNull() {
        just1.replay((Function1<Observable<Integer>, Observable<Integer>>) null);
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorReturnsNull() {
        just1.replay(new Function1<Observable<Integer>, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Observable<Integer> o) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedSelectorNull() {
        just1.replay((Function1<Observable<Integer>, Observable<Integer>>) null, 1, 1, TimeUnit.SECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedSelectorReturnsNull() {
        just1.replay(new Function1<Observable<Integer>, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Observable<Integer> v) {
                return null;
            }
        }, 1, 1, TimeUnit.SECONDS).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySchedulerNull() {
        just1.replay((Scheduler)null);
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedUnitNull() {
        just1.replay(new Function1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Observable<Integer> v) {
                return v;
            }
        }, 1, 1, null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedSchedulerNull() {
        just1.replay(new Function1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Observable<Integer> v) {
                return v;
            }
        }, 1, 1, TimeUnit.SECONDS, null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedSelectorNull() {
        just1.replay(null, 1, TimeUnit.SECONDS, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedSelectorReturnsNull() {
        just1.replay(new Function1<Observable<Integer>, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Observable<Integer> v) {
                return null;
            }
        }, 1, TimeUnit.SECONDS, Schedulers.single()).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorTimeBoundedUnitNull() {
        just1.replay(new Function1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Observable<Integer> v) {
                return v;
            }
        }, 1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorTimeBoundedSchedulerNull() {
        just1.replay(new Function1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Observable<Integer> v) {
                return v;
            }
        }, 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeSizeBoundedUnitNull() {
        just1.replay(1, 1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeSizeBoundedSchedulerNull() {
        just1.replay(1, 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void replayBufferSchedulerNull() {
        just1.replay(1, (Scheduler)null);
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedUnitNull() {
        just1.replay(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedSchedulerNull() {
        just1.replay(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void retryFunctionNull() {
        just1.retry((Function2<Integer, Throwable, Boolean>) null);
    }

    @Test(expected = NullPointerException.class)
    public void retryCountFunctionNull() {
        just1.retry(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void retryPredicateNull() {
        just1.retry((Function1<Throwable, Boolean>) null);
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionNull() {
        just1.retryWhen(null);
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionReturnsNull() {
        Observable.error(new TestException()).retryWhen(new Function1<Observable<? extends Throwable>, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Observable<? extends Throwable> f) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void retryUntil() {
        just1.retryUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void safeSubscribeNull() {
        just1.safeSubscribe(null);
    }

    @Test(expected = NullPointerException.class)
    public void sampleUnitNull() {
        just1.sample(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void sampleSchedulerNull() {
        just1.sample(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void sampleObservableNull() {
        just1.sample(null);
    }

    @Test(expected = NullPointerException.class)
    public void scanFunctionNull() {
        just1.scan(null);
    }

    @Test(expected = NullPointerException.class)
    public void scanFunctionReturnsNull() {
        Observable.just(1, 1).scan(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedNull() {
        just1.scan(null, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedFunctionNull() {
        just1.scan(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedFunctionReturnsNull() {
        just1.scan(1, new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierNull() {
        just1.scanWith(null, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierReturnsNull() {
        just1.scanWith(new Callable<Object>() {
            @Override
            public Object call() {
                return null;
            }
        }, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierFunctionNull() {
        just1.scanWith(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierFunctionReturnsNull() {
        just1.scanWith(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void singleNull() {
        just1.single(null);
    }

    @Test(expected = NullPointerException.class)
    public void skipTimedUnitNull() {
        just1.skip(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void skipTimedSchedulerNull() {
        just1.skip(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void skipLastTimedUnitNull() {
        just1.skipLast(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void skipLastTimedSchedulerNull() {
        just1.skipLast(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void skipUntilNull() {
        just1.skipUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void skipWhileNull() {
        just1.skipWhile(null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableNull() {
        just1.startWith((Iterable<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableIteratorNull() {
        just1.startWith(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableOneNull() {
        just1.startWith(Arrays.asList(1, null)).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void startWithSingleNull() {
        just1.startWith((Integer)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithObservableNull() {
        just1.startWith((Observable<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithArrayNull() {
        just1.startWithArray((Integer[])null);
    }

    @Test(expected = NullPointerException.class)
    public void startWithArrayOneNull() {
        just1.startWithArray(1, null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNextNull() {
        just1.subscribe((Function1<Integer, kotlin.Unit>) null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnErrorNull() {
        just1.subscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer e) {
                return Unit.INSTANCE;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnCompleteNull() {
        just1.subscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer e) {
                return Unit.INSTANCE;
            }
        }, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                return Unit.INSTANCE;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnSubscribeNull() {
        just1.subscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer e) {
                return Unit.INSTANCE;
            }
        }, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                return Unit.INSTANCE;
            }
        }, Functions.EMPTY_ACTION, null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeNull() {
        just1.subscribe((Observer<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNull() {
        just1.subscribeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void switchIfEmptyNull() {
        just1.switchIfEmpty(null);
    }

    @Test(expected = NullPointerException.class)
    public void switchMapNull() {
        just1.switchMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void switchMapFunctionReturnsNull() {
        just1.switchMap(new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void takeTimedUnitNull() {
        just1.take(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void takeTimedSchedulerNull() {
        just1.take(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void takeLastTimedUnitNull() {
        just1.takeLast(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void takeLastSizeTimedUnitNull() {
        just1.takeLast(1, 1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void takeLastTimedSchedulerNull() {
        just1.takeLast(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void takeLastSizeTimedSchedulerNull() {
        just1.takeLast(1, 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void takeUntilPredicateNull() {
        just1.takeUntil((Function1<Integer, Boolean>) null);
    }

    @Test(expected = NullPointerException.class)
    public void takeUntilObservableNull() {
        just1.takeUntil((Observable<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void takeWhileNull() {
        just1.takeWhile(null);
    }

    @Test(expected = NullPointerException.class)
    public void throttleFirstUnitNull() {
        just1.throttleFirst(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void throttleFirstSchedulerNull() {
        just1.throttleFirst(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void throttleLastUnitNull() {
        just1.throttleLast(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void throttleLastSchedulerNull() {
        just1.throttleLast(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void throttleWithTimeoutUnitNull() {
        just1.throttleWithTimeout(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void throttleWithTimeoutSchedulerNull() {
        just1.throttleWithTimeout(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeIntervalUnitNull() {
        just1.timeInterval(null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void timeIntervalSchedulerNull() {
        just1.timeInterval(TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorNull() {
        just1.timeout(null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorReturnsNull() {
        just1.timeout(new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorOtherNull() {
        just1.timeout(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutUnitNull() {
        just1.timeout(1, null, Schedulers.single(), just1);
    }

    @Test(expected = NullPointerException.class)
    public void timeouOtherNull() {
        just1.timeout(1, TimeUnit.SECONDS, Schedulers.single(), null);
    }

    @Test(expected = NullPointerException.class)
    public void timeouSchedulerNull() {
        just1.timeout(1, TimeUnit.SECONDS, null, just1);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutFirstNull() {
        just1.timeout((Observable<Integer>) null, new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void timeoutFirstItemNull() {
        just1.timeout(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutFirstItemReturnsNull() {
        Observable.just(1, 1).timeout(Observable.never(), new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timestampUnitNull() {
        just1.timestamp(null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void timestampSchedulerNull() {
        just1.timestamp(TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void toNull() {
        just1.to(null);
    }

    @Test(expected = NullPointerException.class)
    public void toListNull() {
        just1.toList(null);
    }

    @Test(expected = NullPointerException.class)
    public void toListSupplierReturnsNull() {
        just1.toList(new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toSortedListNull() {
        just1.toSortedList(null);
    }

    @Test(expected = NullPointerException.class)
    public void toMapKeyNull() {
        just1.toMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void toMapValueNull() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test
    public void toMapValueSelectorReturnsNull() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toMapMapSupplierNull() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void toMapMapSupplierReturnsNull() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Callable<Map<Object, Object>>() {
            @Override
            public Map<Object, Object> call() {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapKeyNull() {
        just1.toMultimap(null);
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapValueNull() {
        just1.toMultimap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test
    public void toMultiMapValueSelectorReturnsNullAllowed() {
        just1.toMap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapMapSupplierNull() {
        just1.toMultimap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapSupplierReturnsNull() {
        just1.toMultimap(new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Object>() {
            @Override
            public Object invoke(Integer v) {
                return v;
            }
        }, new Callable<Map<Object, Collection<Object>>>() {
            @Override
            public Map<Object, Collection<Object>> call() {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapMapCollectionSupplierNull() {
        just1.toMultimap(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v;
            }
        }, new Callable<Map<Integer, Collection<Integer>>>() {
            @Override
            public Map<Integer, Collection<Integer>> call() {
                return new HashMap<Integer, Collection<Integer>>();
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapCollectionSupplierReturnsNull() {
        just1.toMultimap(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v;
            }
        }, new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer v) {
                return v;
            }
        }, new Callable<Map<Integer, Collection<Integer>>>() {
            @Override
            public Map<Integer, Collection<Integer>> call() {
                return new HashMap<Integer, Collection<Integer>>();
            }
        }, new Function1<Integer, Collection<Integer>>() {
            @Override
            public Collection<Integer> invoke(Integer v) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void unsafeSubscribeNull() {
        just1.subscribe((Observer<Object>)null);
    }

    @Test(expected = NullPointerException.class)
    public void unsubscribeOnNull() {
        just1.unsubscribeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void windowTimedUnitNull() {
        just1.window(1, null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void windowSizeTimedUnitNull() {
        just1.window(1, null, Schedulers.single(), 1);
    }

    @Test(expected = NullPointerException.class)
    public void windowTimedSchedulerNull() {
        just1.window(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void windowSizeTimedSchedulerNull() {
        just1.window(1, TimeUnit.SECONDS, null, 1);
    }

    @Test(expected = NullPointerException.class)
    public void windowBoundaryNull() {
        just1.window((Observable<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void windowOpenCloseOpenNull() {
        just1.window(null, new Function1<Object, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Object v) {
                return just1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void windowOpenCloseCloseNull() {
        just1.window(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void windowOpenCloseCloseReturnsNull() {
        Observable.never().window(just1, new Function1<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void windowBoundarySupplierNull() {
        just1.window((Callable<Observable<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void windowBoundarySupplierReturnsNull() {
        just1.window(new Callable<Observable<Object>>() {
            @Override
            public Observable<Object> call() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromOtherNull() {
        just1.withLatestFrom(null, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromCombinerNull() {
        just1.withLatestFrom(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromCombinerReturnsNull() {
        just1.withLatestFrom(just1, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableNull() {
        just1.zipWith((Iterable<Integer>)null, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableCombinerNull() {
        just1.zipWith(Arrays.asList(1), null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableCombinerReturnsNull() {
        just1.zipWith(Arrays.asList(1), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableIteratorNull() {
        just1.zipWith(new Iterable<Object>() {
            @Override
            public Iterator<Object> iterator() {
                return null;
            }
        }, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableOneIsNull() {
        Observable.just(1, 2).zipWith(Arrays.asList(1, null), new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithObservableNull() {
        just1.zipWith((Observable<Integer>)null, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return 1;
            }
        });
    }


    @Test(expected = NullPointerException.class)
    public void zipWithCombinerNull() {
        just1.zipWith(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithCombinerReturnsNull() {
        just1.zipWith(just1, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingSubscribe();
    }

}
