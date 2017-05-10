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
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertEquals;

public class SingleDoAfterTerminateTest {

    private final int[] call = { 0 };

    private final Function0 afterTerminate = new Function0() {
        @Override
        public kotlin.Unit invoke() {
            call[0]++;
            return Unit.INSTANCE;
        }
    };

    private final TestObserver<Integer> ts = new TestObserver<Integer>();

    @Test
    public void just() {
        Single.just(1)
        .doAfterTerminate(afterTerminate)
        .subscribeWith(ts)
        .assertResult(1);

        assertAfterTerminateCalledOnce();
    }

    @Test
    public void error() {
        Single.<Integer>error(new TestException())
        .doAfterTerminate(afterTerminate)
        .subscribeWith(ts)
        .assertFailure(TestException.class);

        assertAfterTerminateCalledOnce();
    }

    @Test(expected = NullPointerException.class)
    public void afterTerminateActionNull() {
        Single.just(1).doAfterTerminate(null);
    }

    @Test
    public void justConditional() {
        Single.just(1)
        .doAfterTerminate(afterTerminate)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertResult(1);

        assertAfterTerminateCalledOnce();
    }

    @Test
    public void errorConditional() {
        Single.<Integer>error(new TestException())
        .doAfterTerminate(afterTerminate)
        .filter(Functions.alwaysTrue())
        .subscribeWith(ts)
        .assertFailure(TestException.class);

        assertAfterTerminateCalledOnce();
    }

    @Test
    public void actionThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            Single.just(1)
                    .doAfterTerminate(new Function0() {
                @Override
                public kotlin.Unit invoke() {
                    throw new TestException();
                }
            })
            .test()
            .assertResult(1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.<Integer>create().singleOrError().doAfterTerminate(afterTerminate));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Integer>, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Single<Integer> m) throws Exception {
                return m.doAfterTerminate(afterTerminate);
            }
        });
    }

    private void assertAfterTerminateCalledOnce() {
        assertEquals(1, call[0]);
    }
}
