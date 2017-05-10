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
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Consumer;
import io.reactivex.common.internal.functions.Functions;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaybeCallbackObserverTest {

    @Test
    public void dispose() {
        MaybeCallbackObserver<Object> mo = new MaybeCallbackObserver<Object>(Functions.emptyConsumer(), Functions.emptyConsumer(), Functions.EMPTY_ACTION);

        Disposable d = Disposables.empty();

        mo.onSubscribe(d);

        assertFalse(mo.isDisposed());

        mo.dispose();

        assertTrue(mo.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onSuccessCrashes() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            MaybeCallbackObserver<Object> mo = new MaybeCallbackObserver<Object>(
                    new Consumer<Object>() {
                        @Override
                        public void accept(Object v) throws Exception {
                            throw new TestException();
                        }
                    },
                    Functions.emptyConsumer(),
                    Functions.EMPTY_ACTION);

            mo.onSubscribe(Disposables.empty());

            mo.onSuccess(1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onErrorCrashes() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            MaybeCallbackObserver<Object> mo = new MaybeCallbackObserver<Object>(
                    Functions.emptyConsumer(),
                    new Consumer<Object>() {
                        @Override
                        public void accept(Object v) throws Exception {
                            throw new TestException("Inner");
                        }
                    },
                    Functions.EMPTY_ACTION);

            mo.onSubscribe(Disposables.empty());

            mo.onError(new TestException("Outer"));

            TestCommonHelper.assertError(errors, 0, CompositeException.class);

            List<Throwable> ce = TestCommonHelper.compositeList(errors.get(0));

            TestCommonHelper.assertError(ce, 0, TestException.class, "Outer");
            TestCommonHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrashes() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            MaybeCallbackObserver<Object> mo = new MaybeCallbackObserver<Object>(
                    Functions.emptyConsumer(),
                    Functions.emptyConsumer(),
                    new Function0() {
                        @Override
                        public kotlin.Unit invoke() {
                            throw new TestException();
                        }
                    });

            mo.onSubscribe(Disposables.empty());

            mo.onComplete();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
