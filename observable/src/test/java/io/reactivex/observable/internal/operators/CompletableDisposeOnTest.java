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
import java.util.concurrent.TimeUnit;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.TestScheduler;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Action;
import io.reactivex.observable.Completable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompletableDisposeOnTest {

    @Test
    public void cancelDelayed() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> ps = PublishSubject.create();

        ps.ignoreElements()
        .unsubscribeOn(scheduler)
        .test()
        .cancel();

        assertTrue(ps.hasObservers());

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().ignoreElements().unsubscribeOn(new TestScheduler()));
    }

    @Test
    public void completeAfterCancel() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Void> to = ps.ignoreElements()
        .unsubscribeOn(scheduler)
        .test();

        to.dispose();

        ps.onComplete();

        to.assertEmpty();
    }

    @Test
    public void errorAfterCancel() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            TestScheduler scheduler = new TestScheduler();

            PublishSubject<Integer> ps = PublishSubject.create();

            TestObserver<Void> to = ps.ignoreElements()
            .unsubscribeOn(scheduler)
            .test();

            to.dispose();

            ps.onError(new TestException());

            to.assertEmpty();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void normal() {
        TestScheduler scheduler = new TestScheduler();

        final int[] call = { 0 };

        Completable.complete()
        .doOnDispose(new Action() {
            @Override
            public void invoke() throws Exception {
                call[0]++;
            }
        })
        .unsubscribeOn(scheduler)
        .test()
        .assertResult();

        scheduler.triggerActions();

        assertEquals(0, call[0]);
    }

    @Test
    public void error() {
        TestScheduler scheduler = new TestScheduler();

        final int[] call = { 0 };

        Completable.error(new TestException())
        .doOnDispose(new Action() {
            @Override
            public void invoke() throws Exception {
                call[0]++;
            }
        })
        .unsubscribeOn(scheduler)
        .test()
        .assertFailure(TestException.class);

        scheduler.triggerActions();

        assertEquals(0, call[0]);
    }
}
