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
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.observable.Completable;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertEquals;

public class CompletableDoFinallyTest implements Function0 {

    int calls;

    @Override
    public kotlin.Unit invoke() {
        calls++;
        return Unit.INSTANCE;
    }

    @Test
    public void normalEmpty() {
        Completable.complete()
        .doFinally(this)
        .test()
        .assertResult();

        assertEquals(1, calls);
    }

    @Test
    public void normalError() {
        Completable.error(new TestException())
        .doFinally(this)
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletable(new Function<Completable, Completable>() {
            @Override
            public Completable apply(Completable f) throws Exception {
                return f.doFinally(CompletableDoFinallyTest.this);
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void nullAction() {
        Completable.complete().doFinally(null);
    }

    @Test
    public void actionThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Completable.complete()
                    .doFinally(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .test()
            .assertResult()
            .cancel();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishSubject.create().ignoreElements().doFinally(this));
    }
}
