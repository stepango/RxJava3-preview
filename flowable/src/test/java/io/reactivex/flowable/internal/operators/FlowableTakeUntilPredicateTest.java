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

package io.reactivex.flowable.internal.operators;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import java.util.List;

import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.flowable.internal.subscriptions.BooleanSubscription;
import io.reactivex.flowable.processors.PublishProcessor;
import io.reactivex.flowable.subscribers.TestSubscriber;
import kotlin.jvm.functions.Function1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

;

public class FlowableTakeUntilPredicateTest {
    @Test
    public void takeEmpty() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.empty().takeUntil(new Function1<Object, Boolean>() {
            @Override
            public Boolean invoke(Object v) {
                return true;
            }
        }).subscribe(o);

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeAll() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.just(1, 2).takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return false;
            }
        }).subscribe(o);

        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeFirst() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.just(1, 2).takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return true;
            }
        }).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeSome() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.just(1, 2, 3).takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                return t1 == 2;
            }
        })
        .subscribe(o);

        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onNext(3);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void functionThrows() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Function1<Integer, Boolean> predicate = new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer t1) {
                    throw new TestException("Forced failure");
            }
        };
        Flowable.just(1, 2, 3).takeUntil(predicate).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }
    @Test
    public void sourceThrows() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.just(1)
        .concatWith(Flowable.<Integer>error(new TestException()))
        .concatWith(Flowable.just(2))
                .takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return false;
            }
        }).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }
    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(5L);

        Flowable.range(1, 1000).takeUntil(new Function1<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer v) {
                return false;
            }
        }).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNotComplete();
    }

    @Test
    public void testErrorIncludesLastValueAsCause() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        final TestException e = new TestException("Forced failure");
        Function1<String, Boolean> predicate = new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String t) {
                    throw e;
            }
        };
        Flowable.just("abc").takeUntil(predicate).subscribe(ts);

        ts.assertTerminated();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
        // FIXME last cause value is not saved
//        assertTrue(ts.errors().get(0).getCause().getMessage().contains("abc"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().takeUntil(Functions.alwaysFalse()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> o) throws Exception {
                return o.takeUntil(Functions.alwaysFalse());
            }
        });
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onComplete();
                    observer.onNext(1);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .takeUntil(Functions.alwaysFalse())
            .test()
            .assertResult();

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
