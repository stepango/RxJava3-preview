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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Disposable;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Action;
import io.reactivex.common.functions.Function;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class MaybeUnsubscribeOnTest {

    @Test
    public void normal() throws Exception {
        PublishSubject<Integer> pp = PublishSubject.create();

        final String[] name = { null };

        final CountDownLatch cdl = new CountDownLatch(1);

        pp.doOnDispose(new Action() {
            @Override
            public kotlin.Unit invoke() {
                name[0] = Thread.currentThread().getName();
                cdl.countDown();
                return Unit.INSTANCE;
            }
        })
        .singleElement()
        .unsubscribeOn(Schedulers.single())
        .test(true)
        ;

        assertTrue(cdl.await(5, TimeUnit.SECONDS));

        int times = 10;

        while (times-- > 0 && pp.hasObservers()) {
            Thread.sleep(100);
        }

        assertFalse(pp.hasObservers());

        assertNotEquals(Thread.currentThread().getName(), name[0]);
    }

    @Test
    public void just() {
        Maybe.just(1)
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertResult(1);
    }

    @Test
    public void error() {
        Maybe.<Integer>error(new TestException())
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void empty() {
        Maybe.empty()
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertResult();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Maybe.just(1)
        .unsubscribeOn(Schedulers.single()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Maybe<Object> v) throws Exception {
                return v.unsubscribeOn(Schedulers.single());
            }
        });
    }

    @Test
    public void disposeRace() {
        for (int i = 0; i < 500; i++) {
            PublishSubject<Integer> pp = PublishSubject.create();

            final Disposable[] ds = { null };
            pp.singleElement().unsubscribeOn(Schedulers.computation())
            .subscribe(new MaybeObserver<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {
                    ds[0] = d;
                }

                @Override
                public void onSuccess(Integer value) {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onComplete() {

                }
            });

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ds[0].dispose();
                }
            };

            TestCommonHelper.race(r, r, Schedulers.single());
        }
    }
}
