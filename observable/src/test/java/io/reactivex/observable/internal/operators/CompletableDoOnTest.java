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
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableObserver;
import io.reactivex.observable.observers.TestObserver;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompletableDoOnTest {

    @Test
    public void successAcceptThrows() {
        Completable.complete().doOnEvent(new Function1<Throwable, Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorAcceptThrows() {
        TestObserver<Void> to = Completable.error(new TestException("Outer")).doOnEvent(new Function1<Throwable, Unit>() {
            @Override
            public Unit invoke(Throwable e) {
                throw new TestException("Inner");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestCommonHelper.compositeList(to.errors().get(0));

        TestCommonHelper.assertError(errors, 0, TestException.class, "Outer");
        TestCommonHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void doOnDisposeCalled() {
        final AtomicBoolean atomicBoolean = new AtomicBoolean();

        assertFalse(atomicBoolean.get());

        Completable.complete()
                .doOnDispose(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    atomicBoolean.set(true);
                    return Unit.INSTANCE;
                }
            })
            .test()
            .assertResult()
            .dispose();

        assertTrue(atomicBoolean.get());
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            final Disposable bs = Disposables.empty();

            new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver s) {
                    s.onSubscribe(bs);
                    s.onError(new TestException("Second"));
                    s.onComplete();
                }
            }
                    .doOnSubscribe(new Function1<Disposable, Unit>() {
                @Override
                public Unit invoke(Disposable s) {
                    throw new TestException("First");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isDisposed());

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
