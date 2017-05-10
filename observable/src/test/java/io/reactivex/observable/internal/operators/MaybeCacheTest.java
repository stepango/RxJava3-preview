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

import io.reactivex.common.Disposable;
import io.reactivex.common.Schedulers;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Consumer;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeObserver;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MaybeCacheTest {

    @Test
    public void offlineSuccess() {
        Maybe<Integer> source = Maybe.just(1).cache();
        assertEquals(1, source.blockingGet().intValue());

        source.test()
        .assertResult(1);
    }

    @Test
    public void offlineError() {
        Maybe<Integer> source = Maybe.<Integer>error(new TestException()).cache();

        try {
            source.blockingGet();
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }

        source.test()
        .assertFailure(TestException.class);
    }


    @Test
    public void offlineComplete() {
        Maybe<Integer> source = Maybe.<Integer>empty().cache();

        assertNull(source.blockingGet());

        source.test()
        .assertResult();
    }

    @Test
    public void onlineSuccess() {
        PublishSubject<Integer> pp = PublishSubject.create();

        Maybe<Integer> source = pp.singleElement().cache();

        assertFalse(pp.hasObservers());

        assertNotNull(((MaybeCache<Integer>)source).source.get());

        TestObserver<Integer> ts = source.test();

        assertNull(((MaybeCache<Integer>)source).source.get());

        assertTrue(pp.hasObservers());

        source.test(true).assertEmpty();

        ts.assertEmpty();

        pp.onNext(1);
        pp.onComplete();

        ts.assertResult(1);

        source.test().assertResult(1);

        source.test(true).assertEmpty();
    }

    @Test
    public void onlineError() {
        PublishSubject<Integer> pp = PublishSubject.create();

        Maybe<Integer> source = pp.singleElement().cache();

        assertFalse(pp.hasObservers());

        assertNotNull(((MaybeCache<Integer>)source).source.get());

        TestObserver<Integer> ts = source.test();

        assertNull(((MaybeCache<Integer>)source).source.get());

        assertTrue(pp.hasObservers());

        source.test(true).assertEmpty();

        ts.assertEmpty();

        pp.onError(new TestException());

        ts.assertFailure(TestException.class);

        source.test().assertFailure(TestException.class);

        source.test(true).assertEmpty();
    }

    @Test
    public void onlineComplete() {
        PublishSubject<Integer> pp = PublishSubject.create();

        Maybe<Integer> source = pp.singleElement().cache();

        assertFalse(pp.hasObservers());

        assertNotNull(((MaybeCache<Integer>)source).source.get());

        TestObserver<Integer> ts = source.test();

        assertNull(((MaybeCache<Integer>)source).source.get());

        assertTrue(pp.hasObservers());

        source.test(true).assertEmpty();

        ts.assertEmpty();

        pp.onComplete();

        ts.assertResult();

        source.test().assertResult();

        source.test(true).assertEmpty();
    }

    @Test
    public void crossCancelOnSuccess() {

        final TestObserver<Integer> ts = new TestObserver<Integer>();

        PublishSubject<Integer> pp = PublishSubject.create();

        Maybe<Integer> source = pp.singleElement().cache();

        source.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                ts.cancel();
            }
        });

        source.subscribe(ts);

        pp.onNext(1);
        pp.onComplete();

        ts.assertEmpty();
    }

    @Test
    public void crossCancelOnError() {

        final TestObserver<Integer> ts = new TestObserver<Integer>();

        PublishSubject<Integer> pp = PublishSubject.create();

        Maybe<Integer> source = pp.singleElement().cache();

        source.subscribe(Functions.emptyConsumer(), new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                ts.cancel();
            }
        });

        source.subscribe(ts);

        pp.onError(new TestException());

        ts.assertEmpty();
    }

    @Test
    public void crossCancelOnComplete() {

        final TestObserver<Integer> ts = new TestObserver<Integer>();

        PublishSubject<Integer> pp = PublishSubject.create();

        Maybe<Integer> source = pp.singleElement().cache();

        source.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), new Function0() {
            @Override
            public kotlin.Unit invoke() {
                ts.cancel();
                return Unit.INSTANCE;
            }
        });

        source.subscribe(ts);

        pp.onComplete();

        ts.assertEmpty();
    }

    @Test
    public void addAddRace() {
        for (int i = 0; i < 500; i++) {
            PublishSubject<Integer> pp = PublishSubject.create();

            final Maybe<Integer> source = pp.singleElement().cache();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    source.test();
                }
            };

            TestCommonHelper.race(r, r, Schedulers.single());
        }
    }

    @Test
    public void removeRemoveRace() {
        for (int i = 0; i < 500; i++) {
            PublishSubject<Integer> pp = PublishSubject.create();

            final Maybe<Integer> source = pp.singleElement().cache();

            final TestObserver<Integer> ts1 = source.test();
            final TestObserver<Integer> ts2 = source.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts2.cancel();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void doubleDispose() {
        PublishSubject<Integer> pp = PublishSubject.create();

        final Maybe<Integer> source = pp.singleElement().cache();

        final Disposable[] dout = { null };

        source.subscribe(new MaybeObserver<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                dout[0] = d;
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

        dout[0].dispose();
        dout[0].dispose();
    }
}
