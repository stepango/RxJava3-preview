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

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.flowable.Flowable;
import io.reactivex.interop.TestHelper;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import kotlin.jvm.functions.Function1;

import static io.reactivex.interop.RxJava3Interop.toMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableToMapTest {
    SingleObserver<Object> singleObserver;

    @Before
    public void before() {
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
    public void testToMap() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Single<Map<Integer, String>> mapped = toMap(source, lengthFunc);

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
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Single<Map<Integer, String>> mapped = toMap(source, lengthFunc, duplicate);

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
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Function1<String, Integer> lengthFuncErr = new Function1<String, Integer>() {
            @Override
            public Integer invoke(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };
        Single<Map<Integer, String>> mapped = toMap(source, lengthFuncErr);

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
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Function1<String, String> duplicateErr = new Function1<String, String>() {
            @Override
            public String invoke(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Single<Map<Integer, String>> mapped = toMap(source, lengthFunc, duplicateErr);

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
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

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
        Single<Map<Integer, String>> mapped = toMap(source, lengthFunc, new Function1<String, String>() {
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
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

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
        Single<Map<Integer, String>> mapped = toMap(source, lengthFunc, new Function1<String, String>() {
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
