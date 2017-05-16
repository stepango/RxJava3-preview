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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Scheduler;
import io.reactivex.common.Schedulers;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.internal.functions.Functions;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertTrue;

public class SingleNullTests {

    Single<Integer> just1 = Single.just(1);

    Single<Integer> error = Single.error(new TestException());

    @Test(expected = NullPointerException.class)
    public void ambIterableNull() {
        Single.amb((Iterable<Single<Integer>>)null);
    }

    @Test
    public void ambIterableIteratorNull() {
        Single.amb(new Iterable<Single<Object>>() {
            @Override
            public Iterator<Single<Object>> iterator() {
                return null;
            }
        }).test().assertError(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterableOneIsNull() {
        Single.amb(Arrays.asList(null, just1))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void ambArrayNull() {
        Single.ambArray((Single<Integer>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayOneIsNull() {
        Single.ambArray(null, just1)
            .test()
            .assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableNull() {
        Single.concat((Iterable<Single<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Single.concat(new Iterable<Single<Object>>() {
            @Override
            public Iterator<Single<Object>> iterator() {
                return null;
            }
        }).blockingSubscribe();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void concatIterableOneIsNull() {
        Single.concat(Arrays.asList(just1, null)).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatObservableNull() {
        Single.concat((Observable<Single<Integer>>)null);
    }

    @Test
    public void concatNull() throws Exception {
        int maxArgs = 4;

        @SuppressWarnings("rawtypes")
        Class<Single> clazz = Single.class;
        for (int argCount = 2; argCount <= maxArgs; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount];
                Arrays.fill(params, SingleSource.class);

                Object[] values = new Object[argCount];
                Arrays.fill(values, just1);
                values[argNull - 1] = null;

                Method m = clazz.getMethod("concat", params);

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
    public void createNull() {
        Single.unsafeCreate(null);
    }

    @Test(expected = NullPointerException.class)
    public void deferNull() {
        Single.defer(null);
    }

    @Test(expected = NullPointerException.class)
    public void deferReturnsNull() {
        Single.defer(Functions.<Single<Object>>nullSupplier()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void errorSupplierNull() {
        Single.error((Callable<Throwable>)null);
    }

    @Test(expected = NullPointerException.class)
    public void errorSupplierReturnsNull() {
        Single.error(Functions.<Throwable>nullSupplier()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void errorNull() {
        Single.error((Throwable)null);
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableNull() {
        Single.fromCallable(null);
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableReturnsNull() {
        Single.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureNull() {
        Single.fromFuture((Future<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureReturnsNull() {
        FutureTask<Object> f = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        f.run();
        Single.fromFuture(f).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedFutureNull() {
        Single.fromFuture(null, 1, TimeUnit.SECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedUnitNull() {
        Single.fromFuture(new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        }), 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedSchedulerNull() {
        Single.fromFuture(new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        }), 1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedReturnsNull() {
        FutureTask<Object> f = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        f.run();
        Single.fromFuture(f, 1, TimeUnit.SECONDS).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureSchedulerNull() {
        Single.fromFuture(new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        }), null);
    }

    @Test(expected = NullPointerException.class)
    public void justNull() {
        Single.just(null);
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableNull() {
        Single.merge((Iterable<Single<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableIteratorNull() {
        Single.merge(new Iterable<Single<Object>>() {
            @Override
            public Iterator<Single<Object>> iterator() {
                return null;
            }
        }).blockingSubscribe();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeIterableOneIsNull() {
        Single.merge(Arrays.asList(null, just1)).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mergeSingleNull() {
        Single.merge((Single<Single<Integer>>)null);
    }

    @Test
    public void mergeNull() throws Exception {
        int maxArgs = 4;

        @SuppressWarnings("rawtypes")
        Class<Single> clazz = Single.class;
        for (int argCount = 2; argCount <= maxArgs; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount];
                Arrays.fill(params, SingleSource.class);

                Object[] values = new Object[argCount];
                Arrays.fill(values, just1);
                values[argNull - 1] = null;

                Method m = clazz.getMethod("merge", params);

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
    public void timerUnitNull() {
        Single.timer(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timerSchedulerNull() {
        Single.timer(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void equalsFirstNull() {
        Single.equals(null, just1);
    }

    @Test(expected = NullPointerException.class)
    public void equalsSecondNull() {
        Single.equals(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void usingResourceSupplierNull() {
        Single.using(null, new Function1<Object, Single<Integer>>() {
            @Override
            public Single<Integer> invoke(Object d) {
                return just1;
            }
        }, Functions.emptyConsumer());
    }

    @Test(expected = NullPointerException.class)
    public void usingSingleSupplierNull() {
        Single.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null, Functions.emptyConsumer());
    }

    @Test(expected = NullPointerException.class)
    public void usingSingleSupplierReturnsNull() {
        Single.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function1<Object, Single<Object>>() {
            @Override
            public Single<Object> invoke(Object d) {
                return null;
            }
        }, Functions.emptyConsumer()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void usingDisposeNull() {
        Single.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function1<Object, Single<Integer>>() {
            @Override
            public Single<Integer> invoke(Object d) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableNull() {
        Single.zip((Iterable<Single<Integer>>) null, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Single.zip(new Iterable<Single<Object>>() {
            @Override
            public Iterator<Single<Object>> iterator() {
                return null;
            }
        }, new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableOneIsNull() {
        Single.zip(Arrays.asList(null, just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableOneFunctionNull() {
        Single.zip(Arrays.asList(just1, just1), null).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableOneFunctionReturnsNull() {
        Single.zip(Arrays.asList(just1, just1), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Single> clazz = Single.class;
        for (int argCount = 3; argCount < 10; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount + 1];
                Arrays.fill(params, SingleSource.class);
                Class<?> fniClass = Class.forName("kotlin.jvm.functions.Function" + argCount);
                params[argCount] = fniClass;

                Object[] values = new Object[argCount + 1];
                Arrays.fill(values, just1);
                values[argNull - 1] = null;
                values[argCount] = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { fniClass }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object o, Method m, Object[] a) throws Throwable {
                        return 1;
                    }
                });

                Method m = clazz.getMethod("zip", params);

                try {
                    m.invoke(null, values);
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }

                values[argCount] = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { fniClass }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object o, Method m1, Object[] a) throws Throwable {
                        return null;
                    }
                });
                try {
                    ((Single<Object>)m.invoke(null, values)).blockingGet();
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }

            }

            Class<?>[] params = new Class[argCount + 1];
            Arrays.fill(params, SingleSource.class);
            Class<?> fniClass = Class.forName("kotlin.jvm.functions.Function" + argCount);
            params[argCount] = fniClass;

            Object[] values = new Object[argCount + 1];
            Arrays.fill(values, just1);
            values[argCount] = null;

            Method m = clazz.getMethod("zip", params);

            try {
                m.invoke(null, values);
                Assert.fail("No exception for argCount " + argCount + " / zipper function ");
            } catch (InvocationTargetException ex) {
                if (!(ex.getCause() instanceof NullPointerException)) {
                    Assert.fail("Unexpected exception for argCount " + argCount + " / zipper function: " + ex);
                }
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void zip2FirstNull() {
        Single.zip(null, just1, new Function2<Object, Integer, Object>() {
            @Override
            public Object invoke(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zip2SecondNull() {
        Single.zip(just1, null, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zip2ZipperNull() {
        Single.zip(just1, just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void zip2ZipperReturnsdNull() {
        Single.zip(just1, null, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipArrayNull() {
        Single.zipArray(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, (Single<Integer>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableTwoIsNull() {
        Single.zip(Arrays.asList(just1, null), new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        })
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayOneIsNull() {
        Single.zipArray(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return 1;
            }
        }, just1, null)
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayFunctionNull() {
        Single.zipArray(null, just1, just1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayFunctionReturnsNull() {
        Single.zipArray(new Function1<Object[], Object>() {
            @Override
            public Object invoke(Object[] v) {
                return null;
            }
        }, just1, just1).blockingGet();
    }

    //**************************************************
    // Instance methods
    //**************************************************

    @Test(expected = NullPointerException.class)
    public void ambWithNull() {
        just1.ambWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void composeNull() {
        just1.compose(null);
    }

    @Test(expected = NullPointerException.class)
    public void castNull() {
        just1.cast(null);
    }

    @Test(expected = NullPointerException.class)
    public void concatWith() {
        just1.concatWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void delayUnitNull() {
        just1.delay(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySchedulerNull() {
        just1.delay(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnSubscribeNull() {
        just1.doOnSubscribe(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnSuccess() {
        just1.doOnSuccess(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnError() {
        error.doOnError(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnDisposeNull() {
        just1.doOnDispose(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNull() {
        just1.flatMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapFunctionReturnsNull() {
        just1.flatMap(new Function1<Integer, Single<Object>>() {
            @Override
            public Single<Object> invoke(Integer v) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void liftNull() {
        just1.lift(null);
    }

    @Test(expected = NullPointerException.class)
    public void liftFunctionReturnsNull() {
        just1.lift(new SingleOperator<Object, Integer>() {
            @Override
            public SingleObserver<? super Integer> apply(SingleObserver<? super Object> s) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void containsNull() {
        just1.contains(null);
    }

    @Test(expected = NullPointerException.class)
    public void containsComparerNull() {
        just1.contains(1, null);
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
    public void onErrorReturnSupplierNull() {
        just1.onErrorReturn((Function1<Throwable, Integer>) null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnsSupplierReturnsNull() {
        error.onErrorReturn(new Function1<Throwable, Integer>() {
            @Override
            public Integer invoke(Throwable t) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnValueNull() {
        error.onErrorReturnItem(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextSingleNull() {
        error.onErrorResumeNext((Single<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextFunctionNull() {
        error.onErrorResumeNext((Function1<Throwable, Single<Integer>>) null);
    }

    @Test
    public void onErrorResumeNextFunctionReturnsNull() {
        try {
            error.onErrorResumeNext(new Function1<Throwable, Single<Integer>>() {
                @Override
                public Single<Integer> invoke(Throwable e) {
                    return null;
                }
            }).blockingGet();
        } catch (CompositeException ex) {
            assertTrue(ex.toString(), ex.getExceptions().get(1) instanceof NullPointerException);
        }
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenNull() {
        error.repeatWhen(null);
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenFunctionReturnsNull() {
        error.repeatWhen(new Function1<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Observable<Object> v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void repeatUntilNull() {
        error.repeatUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void retryBiPreducateNull() {
        error.retry((Function2<Integer, Throwable, Boolean>) null);
    }

    @Test(expected = NullPointerException.class)
    public void retryPredicateNull() {
        error.retry((Function1<Throwable, Boolean>) null);
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenNull() {
        error.retryWhen(null);
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionReturnsNull() {
        error.retryWhen(new Function1<Observable<? extends Throwable>, Observable<Object>>() {
            @Override
            public Observable<Object> invoke(Observable<? extends Throwable> e) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void subscribeBiConsumerNull() {
        just1.subscribe((Function2<Integer, Throwable, Unit>) null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeConsumerNull() {
        just1.subscribe((Function1<Integer, kotlin.Unit>) null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeSingeSubscriberNull() {
        just1.subscribe((SingleObserver<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnSuccessNull() {
        just1.subscribe(null, new Function1<Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                return Unit.INSTANCE;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnErrorNull() {
        just1.subscribe(new Function1<Integer, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v) {
                return Unit.INSTANCE;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNull() {
        just1.subscribeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutUnitNull() {
        just1.timeout(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSchedulerNull() {
        just1.timeout(1, TimeUnit.SECONDS, (Scheduler)null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutOtherNull() {
        just1.timeout(1, TimeUnit.SECONDS, Schedulers.single(), null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutOther2Null() {
        just1.timeout(1, TimeUnit.SECONDS, (Single<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void toNull() {
        just1.to(null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithNull() {
        just1.zipWith(null, new Function2<Integer, Object, Object>() {
            @Override
            public Object invoke(Integer a, Object b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipWithFunctionNull() {
        just1.zipWith(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithFunctionReturnsNull() {
        just1.zipWith(just1, new Function2<Integer, Integer, Object>() {
            @Override
            public Object invoke(Integer a, Integer b) {
                return null;
            }
        }).blockingGet();
    }
}
