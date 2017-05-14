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

package io.reactivex.interop.internal.operators;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.utils.CrashingMappedIterable;
import io.reactivex.flowable.subscribers.TestSubscriber;
import io.reactivex.interop.RxJava3Interop;
import io.reactivex.observable.Maybe;
import io.reactivex.observable.MaybeEmitter;
import io.reactivex.observable.MaybeOnSubscribe;
import io.reactivex.observable.MaybeSource;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;

public class MaybeConcatIterableTest {

    @SuppressWarnings("unchecked")
    @Test
    public void take() {
        RxJava3Interop.concatMaybeIterable(Arrays.asList(Maybe.just(1), Maybe.just(2), Maybe.just(3)))
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void iteratorThrows() {
        RxJava3Interop.concatMaybeIterable(new Iterable<MaybeSource<Object>>() {
            @Override
            public Iterator<MaybeSource<Object>> iterator() {
                throw new TestException("iterator()");
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void error() {
        RxJava3Interop.concatMaybeIterable(Arrays.asList(Maybe.just(1), Maybe.<Integer>error(new TestException()), Maybe.just(3)))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void successCancelRace() {
        for (int i = 0; i < 500; i++) {

            final PublishSubject<Integer> pp = PublishSubject.create();

            final TestSubscriber<Integer> to = RxJava3Interop.concatMaybeIterable(Arrays.asList(pp.singleElement()))
            .test();

            pp.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            TestCommonHelper.race(r1, r2);
        }
    }

    @Test
    public void hasNextThrows() {
        RxJava3Interop.concatMaybeIterable(new CrashingMappedIterable<Maybe<Integer>>(100, 1, 100, new Function1<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> invoke(Integer v) {
                return Maybe.just(1);
            }
        }))
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextThrows() {
        RxJava3Interop.concatMaybeIterable(new CrashingMappedIterable<Maybe<Integer>>(100, 100, 1, new Function1<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> invoke(Integer v) {
                return Maybe.just(1);
            }
        }))
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void nextReturnsNull() {
        RxJava3Interop.concatMaybeIterable(new CrashingMappedIterable<Maybe<Integer>>(100, 100, 100, new Function1<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> invoke(Integer v) {
                return null;
            }
        }))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void noSubsequentSubscription() {
        final int[] calls = { 0 };

        Maybe<Integer> source = Maybe.create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> s) throws Exception {
                calls[0]++;
                s.onSuccess(1);
            }
        });

        RxJava3Interop.concatMaybeIterable(Arrays.asList(source, source)).firstElement()
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void noSubsequentSubscriptionDelayError() {
        final int[] calls = { 0 };

        Maybe<Integer> source = Maybe.create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> s) throws Exception {
                calls[0]++;
                s.onSuccess(1);
            }
        });

        RxJava3Interop.concatMaybeIterableDelayError(Arrays.asList(source, source)).firstElement()
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }
}
