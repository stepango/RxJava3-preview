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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import java.util.NoSuchElementException;

import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.TestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableFirstTest {

    Subscriber<String> w;

    private static final kotlin.jvm.functions.Function1<String, Boolean> IS_D = new kotlin.jvm.functions.Function1<String, Boolean>() {
        @Override
        public Boolean invoke(String value) {
            return "d".equals(value);
        }
    };

    @Before
    public void before() {
        w = TestHelper.mockSubscriber();
    }

    @Test
    public void testFirstOrElseOfNoneFlowable() {
        Flowable<String> src = Flowable.empty();
        src.first("default").subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("default");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testFirstOrElseOfSomeFlowable() {
        Flowable<String> src = Flowable.just("a", "b", "c");
        src.first("default").subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("a");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testFirstOrElseWithPredicateOfNoneMatchingThePredicateFlowable() {
        Flowable<String> src = Flowable.just("a", "b", "c");
        src.filter(IS_D).first("default").subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("default");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testFirstOrElseWithPredicateOfSomeFlowable() {
        Flowable<String> src = Flowable.just("a", "b", "c", "d", "e", "f");
        src.filter(IS_D).first("default").subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("d");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testFirstFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3).firstElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithOneElementFlowable() {
        Flowable<Integer> observable = Flowable.just(1).firstElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithEmptyFlowable() {
        Flowable<Integer> observable = Flowable.<Integer> empty().firstElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithPredicateFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3, 4, 5, 6)
                .filter(new kotlin.jvm.functions.Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .firstElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithPredicateAndOneElementFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2)
                .filter(new kotlin.jvm.functions.Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .firstElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithPredicateAndEmptyFlowable() {
        Flowable<Integer> observable = Flowable.just(1)
                .filter(new kotlin.jvm.functions.Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .firstElement();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3)
                .first(4);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithOneElementFlowable() {
        Flowable<Integer> observable = Flowable.just(1).first(2);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithEmptyFlowable() {
        Flowable<Integer> observable = Flowable.<Integer> empty()
                .first(1);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithPredicateFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3, 4, 5, 6)
                .filter(new kotlin.jvm.functions.Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .first(8);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithPredicateAndOneElementFlowable() {
        Flowable<Integer> observable = Flowable.just(1, 2)
                .filter(new kotlin.jvm.functions.Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .first(4);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithPredicateAndEmptyFlowable() {
        Flowable<Integer> observable = Flowable.just(1)
                .filter(new kotlin.jvm.functions.Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .first(2);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void firstOrErrorNoElement() {
        Flowable.empty()
            .firstOrError()
            .test()
            .assertNoValues()
            .assertError(NoSuchElementException.class);
    }

    @Test
    public void firstOrErrorOneElement() {
        Flowable.just(1)
            .firstOrError()
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void firstOrErrorMultipleElements() {
        Flowable.just(1, 2, 3)
            .firstOrError()
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void firstOrErrorError() {
        Flowable.error(new RuntimeException("error"))
            .firstOrError()
            .test()
            .assertNoValues()
            .assertErrorMessage("error")
            .assertError(RuntimeException.class);
    }

    @Test
    public void firstOrErrorNoElementFlowable() {
        Flowable.empty()
            .firstOrError()

            .test()
            .assertNoValues()
            .assertError(NoSuchElementException.class);
    }

    @Test
    public void firstOrErrorOneElementFlowable() {
        Flowable.just(1)
            .firstOrError()

            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void firstOrErrorMultipleElementsFlowable() {
        Flowable.just(1, 2, 3)
            .firstOrError()

            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void firstOrErrorErrorFlowable() {
        Flowable.error(new RuntimeException("error"))
            .firstOrError()

            .test()
            .assertNoValues()
            .assertErrorMessage("error")
            .assertError(RuntimeException.class);
    }
}
