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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import io.reactivex.observable.TestHelper;
import kotlin.jvm.functions.Function1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableToMapTest {
    Observer<Object> objectObserver;
    SingleObserver<Object> singleObserver;

    @Before
    public void before() {
        objectObserver = TestHelper.mockObserver();
        singleObserver = TestHelper.mockSingleObserver();
    }

    Function1<String, Integer> lengthFunc = new Function1<String, Integer>() {
        @Override
        public Integer invoke(String t1) {
            return t1.length();
        }
    };
    Function1<String, String> duplicate = new Function1<String, String>() {
        @Override
        public String invoke(String t1) {
            return t1 + t1;
        }
    };

    @Test
    public void testToMapObservable() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc).toObservable();

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMapWithValueSelectorObservable() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicate).toObservable();

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMapWithErrorObservable() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Function1<String, Integer> lengthFuncErr = new Function1<String, Integer>() {
            @Override
            public Integer invoke(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFuncErr).toObservable();

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
        verify(objectObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testToMapWithErrorInValueSelectorObservable() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Function1<String, String> duplicateErr = new Function1<String, String>() {
            @Override
            public String invoke(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicateErr).toObservable();

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
        verify(objectObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testToMapWithFactoryObservable() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Callable<Map<Integer, String>> mapFactory = new Callable<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> call() {
                return new LinkedHashMap<Integer, String>() {

                    private static final long serialVersionUID = -3296811238780863394L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                        return size() > 3;
                    }
                };
            }
        };

        Function1<String, Integer> lengthFunc = new Function1<String, Integer>() {
            @Override
            public Integer invoke(String t1) {
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, new Function1<String, String>() {
            @Override
            public String invoke(String v) {
                return v;
            }
        }, mapFactory).toObservable();

        Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMapWithErrorThrowingFactoryObservable() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Callable<Map<Integer, String>> mapFactory = new Callable<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> call() {
                throw new RuntimeException("Forced failure");
            }
        };

        Function1<String, Integer> lengthFunc = new Function1<String, Integer>() {
            @Override
            public Integer invoke(String t1) {
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, new Function1<String, String>() {
            @Override
            public String invoke(String v) {
                return v;
            }
        }, mapFactory).toObservable();

        Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
        verify(objectObserver, times(1)).onError(any(Throwable.class));
    }


    @Test
    public void testToMap() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void testToMapWithValueSelector() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicate);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void testToMapWithError() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Function1<String, Integer> lengthFuncErr = new Function1<String, Integer>() {
            @Override
            public Integer invoke(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };
        Single<Map<Integer, String>> mapped = source.toMap(lengthFuncErr);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(expected);
        verify(singleObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testToMapWithErrorInValueSelector() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Function1<String, String> duplicateErr = new Function1<String, String>() {
            @Override
            public String invoke(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicateErr);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(expected);
        verify(singleObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testToMapWithFactory() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Callable<Map<Integer, String>> mapFactory = new Callable<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> call() {
                return new LinkedHashMap<Integer, String>() {

                    private static final long serialVersionUID = -3296811238780863394L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                        return size() > 3;
                    }
                };
            }
        };

        Function1<String, Integer> lengthFunc = new Function1<String, Integer>() {
            @Override
            public Integer invoke(String t1) {
                return t1.length();
            }
        };
        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc, new Function1<String, String>() {
            @Override
            public String invoke(String v) {
                return v;
            }
        }, mapFactory);

        Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void testToMapWithErrorThrowingFactory() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Callable<Map<Integer, String>> mapFactory = new Callable<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> call() {
                throw new RuntimeException("Forced failure");
            }
        };

        Function1<String, Integer> lengthFunc = new Function1<String, Integer>() {
            @Override
            public Integer invoke(String t1) {
                return t1.length();
            }
        };
        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc, new Function1<String, String>() {
            @Override
            public String invoke(String v) {
                return v;
            }
        }, mapFactory);

        Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(expected);
        verify(singleObserver, times(1)).onError(any(Throwable.class));
    }

}
