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

import java.util.Arrays;
import java.util.List;

import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.Observable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.CompletableSubject;
import io.reactivex.observable.subjects.PublishSubject;
import io.reactivex.observable.subjects.UnicastSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CompletableConcatTest {

    @Test
    public void invalidPrefetch() {
        try {
            Completable.concat(Observable.just(Completable.complete()), -99);
            fail("Should have thrown IllegalArgumentExceptio");
        } catch (IllegalArgumentException ex) {
            assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Completable.concat(Observable.just(Completable.complete())));
    }

    @Test
    public void errorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestCommonHelper.trackPluginErrors();

            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final CompletableSubject ps2 = CompletableSubject.create();

                TestObserver<Void> to = Completable.concat(ps1.map(new Function1<Integer, Completable>() {
                    @Override
                    public Completable invoke(Integer v) {
                        return ps2;
                    }
                })).test();

                ps1.onNext(1);

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @Test
    public void synchronousFusedCrash() {
        Completable.concat(Observable.range(1, 2).map(new Function1<Integer, Completable>() {
            @Override
            public Completable invoke(Integer v) {
                throw new TestException();
            }
        }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void unboundedIn() {
        Completable.concat(Observable.just(Completable.complete()).hide(), Integer.MAX_VALUE)
        .test()
        .assertResult();
    }

    @Test
    public void syncFusedUnboundedIn() {
        Completable.concat(Observable.just(Completable.complete()), Integer.MAX_VALUE)
        .test()
        .assertResult();
    }

    @Test
    public void asyncFusedUnboundedIn() {
        UnicastSubject<Completable> up = UnicastSubject.create();
        up.onNext(Completable.complete());
        up.onComplete();

        Completable.concat(up, Integer.MAX_VALUE)
        .test()
        .assertResult();
    }

    @Test
    public void arrayCancelled() {
        Completable.concatArray(Completable.complete(), Completable.complete())
        .test(true)
        .assertEmpty();
    }

    @Test
    public void arrayFirstCancels() {
        final TestObserver<Void> to = new TestObserver<Void>();

        Completable.concatArray(new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver s) {
                s.onSubscribe(Disposables.empty());
                to.cancel();
                s.onComplete();
            }
        }, Completable.complete())
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void iterableCancelled() {
        Completable.concat(Arrays.asList(Completable.complete(), Completable.complete()))
        .test(true)
        .assertEmpty();
    }

    @Test
    public void iterableFirstCancels() {
        final TestObserver<Void> to = new TestObserver<Void>();

        Completable.concat(Arrays.asList(new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver s) {
                s.onSubscribe(Disposables.empty());
                to.cancel();
                s.onComplete();
            }
        }, Completable.complete()))
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void arrayCancelRace() {
        Completable[] a = new Completable[1024];
        Arrays.fill(a, Completable.complete());

        for (int i = 0; i < 500; i++) {

            final Completable c = Completable.concatArray(a);

            final TestObserver<Void> to = new TestObserver<Void>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    c.subscribe(to);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void iterableCancelRace() {
        Completable[] a = new Completable[1024];
        Arrays.fill(a, Completable.complete());

        for (int i = 0; i < 500; i++) {

            final Completable c = Completable.concat(Arrays.asList(a));

            final TestObserver<Void> to = new TestObserver<Void>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    c.subscribe(to);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }
}
