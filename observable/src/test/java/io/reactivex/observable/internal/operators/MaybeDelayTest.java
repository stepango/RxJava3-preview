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

import java.util.concurrent.TimeUnit;

import io.reactivex.common.TestScheduler;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaybeDelayTest {

    @Test
    public void success() {
        Maybe.just(1).delay(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void error() {
        Maybe.error(new TestException()).delay(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void complete() {
        Maybe.empty().delay(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test(expected = NullPointerException.class)
    public void nullUnit() {
        Maybe.just(1).delay(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void nullScheduler() {
        Maybe.just(1).delay(1, TimeUnit.MILLISECONDS, null);
    }

    @Test
    public void disposeDuringDelay() {
        TestScheduler scheduler = new TestScheduler();

        TestObserver<Integer> ts = Maybe.just(1).delay(100, TimeUnit.MILLISECONDS, scheduler)
        .test();

        ts.cancel();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertEmpty();
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Integer> ts = pp.singleElement().delay(100, TimeUnit.MILLISECONDS).test();

        assertTrue(pp.hasObservers());

        ts.cancel();

        assertFalse(pp.hasObservers());
    }

    @Test
    public void isDisposed() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestHelper.checkDisposed(pp.singleElement().delay(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function1<Maybe<Object>, Maybe<Object>>() {
            @Override
            public Maybe<Object> invoke(Maybe<Object> f) {
                return f.delay(100, TimeUnit.MILLISECONDS);
            }
        });
    }
}
