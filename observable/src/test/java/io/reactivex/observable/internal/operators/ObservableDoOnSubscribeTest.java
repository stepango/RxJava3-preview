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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObservableDoOnSubscribeTest {

    @Test
    public void testDoOnSubscribe() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> o = Observable.just(1).doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                    count.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(3, count.get());
    }

    @Test
    public void testDoOnSubscribe2() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> o = Observable.just(1).doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                    count.incrementAndGet();
                return Unit.INSTANCE;
            }
        }).take(1).doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                    count.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        o.subscribe();
        assertEquals(2, count.get());
    }

    @Test
    public void testDoOnUnSubscribeWorksWithRefCount() throws Exception {
        final AtomicInteger onSubscribed = new AtomicInteger();
        final AtomicInteger countBefore = new AtomicInteger();
        final AtomicInteger countAfter = new AtomicInteger();
        final AtomicReference<Observer<? super Integer>> sref = new AtomicReference<Observer<? super Integer>>();
        Observable<Integer> o = Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> s) {
                s.onSubscribe(Disposables.empty());
                onSubscribed.incrementAndGet();
                sref.set(s);
            }

        }).doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                    countBefore.incrementAndGet();
                return Unit.INSTANCE;
            }
        }).publish().refCount()
                .doOnSubscribe(new Function1<Disposable, kotlin.Unit>() {
            @Override
            public Unit invoke(Disposable s) {
                    countAfter.incrementAndGet();
                return Unit.INSTANCE;
            }
        });

        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(1, countBefore.get());
        assertEquals(1, onSubscribed.get());
        assertEquals(3, countAfter.get());
        sref.get().onComplete();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(2, countBefore.get());
        assertEquals(2, onSubscribed.get());
        assertEquals(6, countAfter.get());
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            final Disposable bs = Disposables.empty();

            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> s) {
                    s.onSubscribe(bs);
                    s.onError(new TestException("Second"));
                    s.onComplete();
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
