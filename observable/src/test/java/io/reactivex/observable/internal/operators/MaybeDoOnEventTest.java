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

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static org.junit.Assert.assertTrue;

public class MaybeDoOnEventTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.<Integer>create().singleElement().doOnEvent(new Function2<Integer, Throwable, kotlin.Unit>() {
            @Override
            public Unit invoke(Integer v, Throwable e) {
                // irrelevant
                return Unit.INSTANCE;
            }
        }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Integer>, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Maybe<Integer> m) throws Exception {
                return m.doOnEvent(new Function2<Integer, Throwable, kotlin.Unit>() {
                    @Override
                    public Unit invoke(Integer v, Throwable e) {
                        // irrelevant
                        return Unit.INSTANCE;
                    }
                });
            }
        });
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            final Disposable bs = Disposables.empty();

            new Maybe<Integer>() {
                @Override
                protected void subscribeActual(MaybeObserver<? super Integer> s) {
                    s.onSubscribe(bs);
                    s.onError(new TestException("Second"));
                    s.onComplete();
                    s.onSuccess(1);
                }
            }
                    .doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
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
