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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.common.Disposable;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.extensions.QueueDisposable;
import io.reactivex.observable.observers.ObserverFusion;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.UnicastSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObservableDoFinallyTest implements Function0 {

    int calls;

    @Override
    public kotlin.Unit invoke() {
        calls++;
        return Unit.INSTANCE;
    }

    @Test
    public void normalJust() {
        Observable.just(1)
        .doFinally(this)
        .test()
        .assertResult(1);

        assertEquals(1, calls);
    }

    @Test
    public void normalEmpty() {
        Observable.empty()
        .doFinally(this)
        .test()
        .assertResult();

        assertEquals(1, calls);
    }

    @Test
    public void normalError() {
        Observable.error(new TestException())
        .doFinally(this)
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void normalTake() {
        Observable.range(1, 10)
        .doFinally(this)
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> f) throws Exception {
                return f.doFinally(ObservableDoFinallyTest.this);
            }
        });
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> f) throws Exception {
                return f.doFinally(ObservableDoFinallyTest.this).filter(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void syncFused() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5)
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedBoundary() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC | QueueDisposable.BOUNDARY);

        Observable.range(1, 5)
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFused() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ASYNC);

        UnicastSubject<Integer> up = UnicastSubject.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedBoundary() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ASYNC | QueueDisposable.BOUNDARY);

        UnicastSubject<Integer> up = UnicastSubject.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }


    @Test
    public void normalJustConditional() {
        Observable.just(1)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult(1);

        assertEquals(1, calls);
    }

    @Test
    public void normalEmptyConditional() {
        Observable.empty()
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult();

        assertEquals(1, calls);
    }

    @Test
    public void normalErrorConditional() {
        Observable.error(new TestException())
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void normalTakeConditional() {
        Observable.range(1, 10)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void nonFused() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5).hide()
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void nonFusedConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5).hide()
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedBoundaryConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC | QueueDisposable.BOUNDARY);

        Observable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ASYNC);

        UnicastSubject<Integer> up = UnicastSubject.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedBoundaryConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ASYNC | QueueDisposable.BOUNDARY);

        UnicastSubject<Integer> up = UnicastSubject.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test(expected = NullPointerException.class)
    public void nullAction() {
        Observable.just(1).doFinally(null);
    }

    @Test
    public void actionThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Observable.just(1)
                    .doFinally(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .test()
            .assertResult(1)
            .cancel();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void actionThrowsConditional() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Observable.just(1)
                    .doFinally(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertResult(1)
            .cancel();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void clearIsEmpty() {
        Observable.range(1, 5)
        .doFinally(this)
        .subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable s) {
                @SuppressWarnings("unchecked")
                QueueDisposable<Integer> qs = (QueueDisposable<Integer>)s;

                qs.requestFusion(QueueDisposable.ANY);

                assertFalse(qs.isEmpty());

                try {
                    assertEquals(1, qs.poll().intValue());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }

                assertFalse(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                qs.dispose();
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(1, calls);
    }

    @Test
    public void clearIsEmptyConditional() {
        Observable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable s) {
                @SuppressWarnings("unchecked")
                QueueDisposable<Integer> qs = (QueueDisposable<Integer>)s;

                qs.requestFusion(QueueDisposable.ANY);

                assertFalse(qs.isEmpty());

                assertFalse(qs.isDisposed());

                try {
                    assertEquals(1, qs.poll().intValue());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }

                assertFalse(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                qs.dispose();

                assertTrue(qs.isDisposed());
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(1, calls);
    }


    @Test
    public void eventOrdering() {
        final List<String> list = new ArrayList<String>();

        Observable.error(new TestException())
                .doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                list.add("dispose");
                return Unit.INSTANCE;
            }
        })
                .doFinally(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                list.add("finally");
                return Unit.INSTANCE;
            }
        })
        .subscribe(
                new Function1<Object, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Object v) {
                        list.add("onNext");
                        return Unit.INSTANCE;
                    }
                },
                new Function1<Throwable, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Throwable e) {
                        list.add("onError");
                        return Unit.INSTANCE;
                    }
                },
                new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        list.add("onComplete");
                        return Unit.INSTANCE;
                    }
                });

        assertEquals(Arrays.asList("onError", "finally"), list);
    }

    @Test
    public void eventOrdering2() {
        final List<String> list = new ArrayList<String>();

        Observable.just(1)
                .doOnDispose(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                list.add("dispose");
                return Unit.INSTANCE;
            }
        })
                .doFinally(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                list.add("finally");
                return Unit.INSTANCE;
            }
        })
        .subscribe(
                new Function1<Object, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Object v) {
                        list.add("onNext");
                        return Unit.INSTANCE;
                    }
                },
                new Function1<Throwable, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Throwable e) {
                        list.add("onError");
                        return Unit.INSTANCE;
                    }
                },
                new Function0() {
                    @Override
                    public kotlin.Unit invoke() {
                        list.add("onComplete");
                        return Unit.INSTANCE;
                    }
                });

        assertEquals(Arrays.asList("onNext", "onComplete", "finally"), list);
    }

}
