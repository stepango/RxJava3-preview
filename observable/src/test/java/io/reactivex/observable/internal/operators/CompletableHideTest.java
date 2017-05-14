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

import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Completable;
import io.reactivex.observable.CompletableSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.CompletableSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertFalse;

public class CompletableHideTest {

    @Test
    public void never() {
        Completable.never()
        .hide()
        .test()
        .assertNotComplete()
        .assertNoErrors();
    }

    @Test
    public void complete() {
        Completable.complete()
        .hide()
        .test()
        .assertResult();
    }

    @Test
    public void error() {
        Completable.error(new TestException())
        .hide()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void hidden() {
        assertFalse(CompletableSubject.create().hide() instanceof CompletableSubject);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposedCompletable(new Function1<Completable, CompletableSource>() {
            @Override
            public CompletableSource invoke(Completable m) {
                return m.hide();
            }
        });
    }

    @Test
    public void isDisposed() {
        CompletableSubject pp = CompletableSubject.create();

        TestHelper.checkDisposed(pp.hide());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletable(new Function1<Completable, Completable>() {
            @Override
            public Completable invoke(Completable f) {
                return f.hide();
            }
        });
    }
}
