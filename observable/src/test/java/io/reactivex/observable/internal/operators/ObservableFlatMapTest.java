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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.BiFunction;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableFlatMapTest {
    @Test
    public void testNormal() {
        Observer<Object> o = TestHelper.mockObserver();

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Function1<Integer, List<Integer>> func = new Function1<Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(Integer t1) {
                return list;
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        for (Integer s : source) {
            for (Integer v : list) {
                verify(o).onNext(s | v);
            }
        }
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testCollectionFunctionThrows() {
        Observer<Object> o = TestHelper.mockObserver();

        Function1<Integer, List<Integer>> func = new Function1<Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(Integer t1) {
                throw new TestException();
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testResultFunctionThrows() {
        Observer<Object> o = TestHelper.mockObserver();

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Function1<Integer, List<Integer>> func = new Function1<Integer, List<Integer>>() {
            @Override
            public List<Integer> invoke(Integer t1) {
                return list;
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testMergeError() {
        Observer<Object> o = TestHelper.mockObserver();

        Function1<Integer, Observable<Integer>> func = new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return Observable.error(new TestException());
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMap(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    <T, R> Function1<T, R> just(final R value) {
        return new Function1<T, R>() {

            @Override
            public R invoke(T t1) {
                return value;
            }
        };
    }

    <R> Callable<R> just0(final R value) {
        return new Callable<R>() {

            @Override
            public R call() {
                return value;
            }
        };
    }

    @Test
    public void testFlatMapTransformsNormal() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), just(onError), just0(onComplete)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onComplete();

        verify(o, never()).onNext(5);
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFlatMapTransformsException() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.concat(
                Observable.fromIterable(Arrays.asList(10, 20, 30)),
                Observable.<Integer> error(new RuntimeException("Forced failure!"))
                );


        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), just(onError), just0(onComplete)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(5);
        verify(o).onComplete();
        verify(o, never()).onNext(4);

        verify(o, never()).onError(any(Throwable.class));
    }

    <R> Callable<R> funcThrow0(R r) {
        return new Callable<R>() {
            @Override
            public R call() {
                throw new TestException();
            }
        };
    }

    <T, R> Function1<T, R> funcThrow(T t, R r) {
        return new Function1<T, R>() {
            @Override
            public R invoke(T t) {
                throw new TestException();
            }
        };
    }

    @Test
    public void testFlatMapTransformsOnNextFuncThrows() {
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(funcThrow(1, onError), just(onError), just0(onComplete)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testFlatMapTransformsOnErrorFuncThrows() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.error(new TestException());

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), funcThrow((Throwable) null, onError), just0(onComplete)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testFlatMapTransformsOnCompletedFuncThrows() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.<Integer> asList());

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), just(onError), funcThrow0(onComplete)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testFlatMapTransformsMergeException() {
        Observable<Integer> onNext = Observable.error(new TestException());
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), just(onError), funcThrow0(onComplete)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    private static <T> Observable<T> composer(Observable<T> source, final AtomicInteger subscriptionCount, final int m) {
        return source.doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                    int n = subscriptionCount.getAndIncrement();
                    if (n >= m) {
                        Assert.fail("Too many subscriptions! " + (n + 1));
                    }
                return Unit.INSTANCE;
            }
        }).doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                    int n = subscriptionCount.decrementAndGet();
                    if (n < 0) {
                        Assert.fail("Too many unsubscriptions! " + (n - 1));
                    }
                return Unit.INSTANCE;
            }
        });
    }

    @Test
    public void testFlatMapMaxConcurrent() {
        final int m = 4;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> source = Observable.range(1, 10)
                .flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return composer(Observable.range(t1 * 10, 2), subscriptionCount, m)
                        .subscribeOn(Schedulers.computation());
            }
        }, m);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        source.subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        Set<Integer> expected = new HashSet<Integer>(Arrays.asList(
                10, 11, 20, 21, 30, 31, 40, 41, 50, 51, 60, 61, 70, 71, 80, 81, 90, 91, 100, 101
        ));
        Assert.assertEquals(expected.size(), ts.valueCount());
        Assert.assertTrue(expected.containsAll(ts.values()));
    }
    @Test
    public void testFlatMapSelectorMaxConcurrent() {
        final int m = 4;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> source = Observable.range(1, 10)
                .flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer t1) {
                return composer(Observable.range(t1 * 10, 2), subscriptionCount, m)
                        .subscribeOn(Schedulers.computation());
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 * 1000 + t2;
            }
        }, m);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        source.subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        Set<Integer> expected = new HashSet<Integer>(Arrays.asList(
                1010, 1011, 2020, 2021, 3030, 3031, 4040, 4041, 5050, 5051,
                6060, 6061, 7070, 7071, 8080, 8081, 9090, 9091, 10100, 10101
        ));
        Assert.assertEquals(expected.size(), ts.valueCount());
        System.out.println("--> testFlatMapSelectorMaxConcurrent: " + ts.values());
        Assert.assertTrue(expected.containsAll(ts.values()));
    }

    @Test
    public void testFlatMapTransformsMaxConcurrentNormalLoop() {
        for (int i = 0; i < 1000; i++) {
            if (i % 100 == 0) {
                System.out.println("testFlatMapTransformsMaxConcurrentNormalLoop => " + i);
            }
            testFlatMapTransformsMaxConcurrentNormal();
        }
    }

    @Test
    public void testFlatMapTransformsMaxConcurrentNormal() {
        final int m = 2;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> onNext =
                composer(
                        Observable.fromIterable(Arrays.asList(1, 2, 3))
                        .observeOn(Schedulers.computation())
                        ,
                subscriptionCount, m)
                .subscribeOn(Schedulers.computation())
                ;

        Observable<Integer> onComplete = composer(Observable.fromIterable(Arrays.asList(4)), subscriptionCount, m)
                .subscribeOn(Schedulers.computation());

        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Observer<Object> o = TestHelper.mockObserver();
        TestObserver<Object> ts = new TestObserver<Object>(o);

        Function1<Throwable, Observable<Integer>> just = just(onError);
        source.flatMap(just(onNext), just, just0(onComplete), m).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminated();

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onComplete();

        verify(o, never()).onNext(5);
        verify(o, never()).onError(any(Throwable.class));
    }

    @Ignore("Don't care for any reordering")
    @Test(timeout = 10000)
    public void flatMapRangeAsyncLoop() {
        for (int i = 0; i < 2000; i++) {
            if (i % 10 == 0) {
                System.out.println("flatMapRangeAsyncLoop > " + i);
            }
            TestObserver<Integer> ts = new TestObserver<Integer>();
            Observable.range(0, 1000)
                    .flatMap(new Function1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> invoke(Integer t) {
                    return Observable.just(t);
                }
            })
            .observeOn(Schedulers.computation())
            .subscribe(ts);

            ts.awaitTerminalEvent(2500, TimeUnit.MILLISECONDS);
            if (ts.completions() == 0) {
                System.out.println(ts.valueCount());
            }
            ts.assertTerminated();
            ts.assertNoErrors();
            List<Integer> list = ts.values();
            assertEquals(1000, list.size());
            boolean f = false;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j) != j) {
                    System.out.println(j + " " + list.get(j));
                    f = true;
                }
            }
            if (f) {
                Assert.fail("Results are out of order!");
            }
        }
    }
    @Test(timeout = 30000)
    public void flatMapRangeMixedAsyncLoop() {
        for (int i = 0; i < 2000; i++) {
            if (i % 10 == 0) {
                System.out.println("flatMapRangeAsyncLoop > " + i);
            }
            TestObserver<Integer> ts = new TestObserver<Integer>();
            Observable.range(0, 1000)
                    .flatMap(new Function1<Integer, Observable<Integer>>() {
                final Random rnd = new Random();
                @Override
                public Observable<Integer> invoke(Integer t) {
                    Observable<Integer> r = Observable.just(t);
                    if (rnd.nextBoolean()) {
                        r = r.hide();
                    }
                    return r;
                }
            })
            .observeOn(Schedulers.computation())
            .subscribe(ts);

            ts.awaitTerminalEvent(2500, TimeUnit.MILLISECONDS);
            if (ts.completions() == 0) {
                System.out.println(ts.valueCount());
            }
            ts.assertTerminated();
            ts.assertNoErrors();
            List<Integer> list = ts.values();
            if (list.size() < 1000) {
                Set<Integer> set = new HashSet<Integer>(list);
                for (int j = 0; j < 1000; j++) {
                    if (!set.contains(j)) {
                        System.out.println(j + " missing");
                    }
                }
            }
            assertEquals(1000, list.size());
        }
    }

    @Test
    public void flatMapIntPassthruAsync() {
        for (int i = 0;i < 1000; i++) {
            TestObserver<Integer> ts = new TestObserver<Integer>();

            Observable.range(1, 1000).flatMap(new Function1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> invoke(Integer t) {
                    return Observable.just(1).subscribeOn(Schedulers.computation());
                }
            }).subscribe(ts);

            ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertComplete();
            ts.assertValueCount(1000);
        }
    }
    @Test
    public void flatMapTwoNestedSync() {
        for (final int n : new int[] { 1, 1000, 1000000 }) {
            TestObserver<Integer> ts = new TestObserver<Integer>();

            Observable.just(1, 2).flatMap(new Function1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> invoke(Integer t) {
                    return Observable.range(1, n);
                }
            }).subscribe(ts);

            System.out.println("flatMapTwoNestedSync >> @ " + n);
            ts.assertNoErrors();
            ts.assertComplete();
            ts.assertValueCount(n * 2);
        }
    }

    @Test
    public void flatMapBiMapper() {
        Observable.just(1)
                .flatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.just(v * 10);
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true)
        .test()
        .assertResult(11);
    }

    @Test
    public void flatMapBiMapperWithError() {
        Observable.just(1)
                .flatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.just(v * 10).concatWith(Observable.<Integer>error(new TestException()));
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true)
        .test()
        .assertFailure(TestException.class, 11);
    }

    @Test
    public void flatMapBiMapperMaxConcurrency() {
        Observable.just(1, 2)
                .flatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.just(v * 10);
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true, 1)
        .test()
        .assertResult(11, 22);
    }

    @Test
    public void flatMapEmpty() {
        assertSame(Observable.empty(), Observable.empty().flatMap(new Function1<Object, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Object v) {
                return Observable.just(v);
            }
        }));
    }

    @Test
    public void mergeScalar() {
        Observable.merge(Observable.just(Observable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void mergeScalar2() {
        Observable.merge(Observable.just(Observable.just(1)).hide())
        .test()
        .assertResult(1);
    }

    @Test
    public void mergeScalarEmpty() {
        Observable.merge(Observable.just(Observable.empty()).hide())
        .test()
        .assertResult();
    }

    @Test
    public void mergeScalarError() {
        Observable.merge(Observable.just(Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        })).hide())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void scalarReentrant() {
        final PublishSubject<Observable<Integer>> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(Observable.just(2));
                }
            }
        };

        Observable.merge(ps)
        .subscribe(to);

        ps.onNext(Observable.just(1));
        ps.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void scalarReentrant2() {
        final PublishSubject<Observable<Integer>> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(Observable.just(2));
                }
            }
        };

        Observable.merge(ps, 2)
        .subscribe(to);

        ps.onNext(Observable.just(1));
        ps.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void innerCompleteCancelRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = Observable.merge(Observable.just(ps)).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void fusedInnerThrows() {
        Observable.just(1).hide()
                .flatMap(new Function1<Integer, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Integer v) {
                return Observable.range(1, 2).map(new Function1<Integer, Object>() {
                    @Override
                    public Object invoke(Integer w) {
                        throw new TestException();
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedInnerThrows2() {
        TestObserver<Integer> to = Observable.range(1, 2).hide()
                .flatMap(new Function1<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> invoke(Integer v) {
                return Observable.range(1, 2).map(new Function1<Integer, Integer>() {
                    @Override
                    public Integer invoke(Integer w) {
                        throw new TestException();
                    }
                });
            }
        }, true)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.errorList(to);

        TestCommonHelper.assertError(errors, 0, TestException.class);

        TestCommonHelper.assertError(errors, 1, TestException.class);
    }

    @Test
    public void noCrossBoundaryFusion() {
        for (int i = 0; i < 500; i++) {
            TestObserver<Object> ts = Observable.merge(
                    Observable.just(1).observeOn(Schedulers.single()).map(new Function1<Integer, Object>() {
                        @Override
                        public Object invoke(Integer v) {
                            return Thread.currentThread().getName().substring(0, 4);
                        }
                    }),
                    Observable.just(1).observeOn(Schedulers.computation()).map(new Function1<Integer, Object>() {
                        @Override
                        public Object invoke(Integer v) {
                            return Thread.currentThread().getName().substring(0, 4);
                        }
                    })
            )
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(2);

            List<Object> list = ts.values();

            assertTrue(list.toString(), list.contains("RxSi"));
            assertTrue(list.toString(), list.contains("RxCo"));
        }
    }

    @Test
    public void cancelScalarDrainRace() {
        for (int i = 0; i < 1000; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();
            try {

                final PublishSubject<Observable<Integer>> pp = PublishSubject.create();

                final TestObserver<Integer> ts = pp.flatMap(Functions.<Observable<Integer>>identity()).test();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ts.cancel();
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp.onComplete();
                    }
                };

                TestCommonHelper.race(r1, r2);

                assertTrue(errors.toString(), errors.isEmpty());
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @Test
    public void cancelDrainRace() {
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 50; j += 5) {
                List<Throwable> errors = TestCommonHelper.trackPluginErrors();
                try {

                    final PublishSubject<Observable<Integer>> pp = PublishSubject.create();

                    final TestObserver<Integer> ts = pp.flatMap(Functions.<Observable<Integer>>identity()).test();

                    final PublishSubject<Integer> just = PublishSubject.create();
                    final PublishSubject<Integer> just2 = PublishSubject.create();
                    pp.onNext(just);
                    pp.onNext(just2);

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            just2.onNext(1);
                            ts.cancel();
                        }
                    };
                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            just.onNext(1);
                        }
                    };

                    TestCommonHelper.race(r1, r2);

                    assertTrue(errors.toString(), errors.isEmpty());
                } finally {
                    RxJavaCommonPlugins.reset();
                }
            }
        }
    }
}
