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

import java.util.concurrent.TimeoutException;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.Observable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaybeTimeoutPublisherTest {

    @Test
    public void mainError() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp1.onError(new TestException());

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp2.onError(new TestException());

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void fallbackError() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2, Maybe.<Integer>error(new TestException())).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp2.onNext(1);
        pp2.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void fallbackComplete() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2, Maybe.<Integer>empty()).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp2.onNext(1);
        pp2.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertResult();
    }

    @Test
    public void mainComplete() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp1.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertResult();
    }

    @Test
    public void otherComplete() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp2.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertFailure(TimeoutException.class);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestHelper.checkDisposed(pp1.singleElement().timeout(pp2));
    }

    @Test
    public void dispose2() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestHelper.checkDisposed(pp1.singleElement().timeout(pp2, Maybe.just(1)));
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            TestCommonHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> pp1 = PublishSubject.create();
                final PublishSubject<Integer> pp2 = PublishSubject.create();

                TestObserver<Integer> to = pp1.singleElement().timeout(pp2).test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp2.onError(ex);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);
            } finally {
                RxJavaCommonPlugins.reset();
            }
        }
    }

    @Test
    public void onCompleteRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> pp1 = PublishSubject.create();
            final PublishSubject<Integer> pp2 = PublishSubject.create();

            TestObserver<Integer> to = pp1.singleElement().timeout(pp2).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onComplete();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());

            to.assertSubscribed().assertNoValues();

            if (to.errorCount() != 0) {
                to.assertError(TimeoutException.class).assertNotComplete();
            } else {
                to.assertNoErrors().assertComplete();
            }
        }
    }

    @Test
    public void badSourceOther() {
        TestHelper.checkBadSourceObservable(new Function1<Observable<Integer>, Object>() {
            @Override
            public Object invoke(Observable<Integer> f) {
                return Maybe.never().timeout(f, Maybe.just(1));
            }
        }, false, null, 1, 1);
    }
}
