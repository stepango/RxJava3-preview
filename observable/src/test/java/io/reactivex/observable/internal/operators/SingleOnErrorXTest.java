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

import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.observers.TestObserver;
import kotlin.jvm.functions.Function1;

public class SingleOnErrorXTest {

    @Test
    public void returnSuccess() {
        Single.just(1)
        .onErrorReturnItem(2)
        .test()
        .assertResult(1);
    }

    @Test
    public void resumeThrows() {
        TestObserver<Integer> to = Single.<Integer>error(new TestException("Outer"))
                .onErrorReturn(new Function1<Throwable, Integer>() {
            @Override
            public Integer invoke(Throwable e) {
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
    public void resumeErrors() {
        Single.error(new TestException("Main"))
        .onErrorResumeNext(Single.error(new TestException("Resume")))
        .test()
        .assertFailureAndMessage(TestException.class, "Resume");
    }

    @Test
    public void resumeDispose() {
        TestHelper.checkDisposed(Single.error(new TestException("Main"))
        .onErrorResumeNext(Single.just(1)));
    }

    @Test
    public void resumeDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function1<Single<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> invoke(Single<Object> s) {
                return s.onErrorResumeNext(Single.just(1));
            }
        });
    }

    @Test
    public void resumeSuccess() {
        Single.just(1)
        .onErrorResumeNext(Single.just(2))
        .test()
        .assertResult(1);
    }
}
