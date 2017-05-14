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

import java.util.List;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.Observable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaybeTakeUntilTest {

    @Test
    public void normalPublisher() {
        Maybe.just(1).takeUntil(Observable.never())
        .test()
        .assertResult(1);
    }

    @Test
    public void normalMaybe() {
        Maybe.just(1).takeUntil(Maybe.never())
        .test()
        .assertResult(1);
    }

    @Test
    public void untilFirstPublisher() {
        Maybe.just(1).takeUntil(Observable.just("one"))
        .test()
        .assertResult();
    }

    @Test
    public void untilFirstMaybe() {
        Maybe.just(1).takeUntil(Maybe.just("one"))
        .test()
        .assertResult();
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishSubject.create().singleElement().takeUntil(Maybe.never()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function1<Maybe<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> invoke(Maybe<Object> m) {
                return m.takeUntil(Maybe.never());
            }
        });
    }

    @Test
    public void mainErrors() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2.singleElement()).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp1.onError(new TestException());

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void otherErrors() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2.singleElement()).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp2.onError(new TestException());

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainCompletes() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2.singleElement()).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp1.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertResult();
    }

    @Test
    public void otherCompletes() {
        PublishSubject<Integer> pp1 = PublishSubject.create();
        PublishSubject<Integer> pp2 = PublishSubject.create();

        TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2.singleElement()).test();

        assertTrue(pp1.hasObservers());
        assertTrue(pp2.hasObservers());

        pp2.onComplete();

        assertFalse(pp1.hasObservers());
        assertFalse(pp2.hasObservers());

        to.assertResult();
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> pp1 = PublishSubject.create();
            final PublishSubject<Integer> pp2 = PublishSubject.create();

            TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2.singleElement()).test();

            final TestException ex1 = new TestException();
            final TestException ex2 = new TestException();

            List<Throwable> errors = TestCommonHelper.trackPluginErrors();
            try {

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp2.onError(ex2);
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
    public void onCompleteRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> pp1 = PublishSubject.create();
            final PublishSubject<Integer> pp2 = PublishSubject.create();

            TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2.singleElement()).test();

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

            to.assertResult();
        }
    }
}
