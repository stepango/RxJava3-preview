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

import org.junit.Before;
import org.junit.Test;

import io.reactivex.observable.Observable;
import io.reactivex.observable.ObservableSource;
import io.reactivex.observable.Observer;
import io.reactivex.observable.TestHelper;
import io.reactivex.observable.subjects.PublishSubject;
import kotlin.jvm.functions.Function1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableSkipUntilTest {
    Observer<Object> observer;

    @Before
    public void before() {
        observer = TestHelper.mockObserver();
    }

    @Test
    public void normal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(other);
        m.subscribe(observer);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(4);
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void otherNeverFires() {
        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(Observable.never());

        m.subscribe(observer);

        source.onNext(0);
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void otherEmpty() {
        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(Observable.empty());

        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
    }

    @Test
    public void otherFiresAndCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(other);
        m.subscribe(observer);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);
        other.onComplete();

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(4);
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void sourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(other);
        m.subscribe(observer);

        source.onNext(0);
        source.onNext(1);

        other.onNext(100);
        other.onComplete();

        source.onNext(2);
        source.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void otherThrowsImmediately() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> m = source.skipUntil(other);
        m.subscribe(observer);

        source.onNext(0);
        source.onNext(1);

        other.onError(new RuntimeException("Forced failure"));

        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().skipUntil(PublishSubject.create()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Object> o) {
                return o.skipUntil(Observable.never());
            }
        });

        TestHelper.checkDoubleOnSubscribeObservable(new Function1<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> invoke(Observable<Object> o) {
                return Observable.never().skipUntil(o);
            }
        });
    }
}
