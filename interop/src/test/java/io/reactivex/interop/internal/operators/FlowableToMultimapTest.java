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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.flowable.Flowable;
import io.reactivex.interop.TestHelper;
import io.reactivex.observable.Single;
import io.reactivex.observable.SingleObserver;
import kotlin.jvm.functions.Function1;

import static io.reactivex.interop.RxJava3Interop.toMultimap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableToMultimapTest {
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
    public void testToMultimap() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Single<Map<Integer, Collection<String>>> mapped = toMultimap(source, lengthFunc);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void testToMultimapWithValueSelector() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Single<Map<Integer, Collection<String>>> mapped = toMultimap(source, lengthFunc, duplicate);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void testToMultimapWithMapFactory() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd", "eee", "fff");

        Callable<Map<Integer, Collection<String>>> mapFactory = new Callable<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> call() {
                return new LinkedHashMap<Integer, Collection<String>>() {

                    private static final long serialVersionUID = -2084477070717362859L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, Collection<String>> eldest) {
                        return size() > 2;
                    }
                };
            }
        };

        Function1<String, String> identity = new Function1<String, String>() {
            @Override
            public String invoke(String v) {
                return v;
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = toMultimap(source,
                lengthFunc, identity,
                mapFactory, new Function1<Integer, Collection<String>>() {
                    @Override
                    public Collection<String> invoke(Integer e) {
                        return new ArrayList<String>();
                    }
                });

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void testToMultimapWithCollectionFactory() {
        Flowable<String> source = Flowable.just("cc", "dd", "eee", "eee");

        Function1<Integer, Collection<String>> collectionFactory = new Function1<Integer, Collection<String>>() {
            @Override
            public Collection<String> invoke(Integer t1) {
                if (t1 == 2) {
                    return new ArrayList<String>();
                } else {
                    return new HashSet<String>();
                }
            }
        };

        Function1<String, String> identity = new Function1<String, String>() {
            @Override
            public String invoke(String v) {
                return v;
            }
        };
        Callable<Map<Integer, Collection<String>>> mapSupplier = new Callable<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> call() {
                return new HashMap<Integer, Collection<String>>();
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = toMultimap(source
                , lengthFunc, identity, mapSupplier, collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, new HashSet<String>(Arrays.asList("eee")));

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void testToMultimapWithError() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Function1<String, Integer> lengthFuncErr = new Function1<String, Integer>() {
            @Override
            public Integer invoke(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = toMultimap(source, lengthFuncErr);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, times(1)).onError(any(Throwable.class));
        verify(singleObserver, never()).onSuccess(expected);
    }

    @Test
    public void testToMultimapWithErrorInValueSelector() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Function1<String, String> duplicateErr = new Function1<String, String>() {
            @Override
            public String invoke(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = toMultimap(source, lengthFunc, duplicateErr);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, times(1)).onError(any(Throwable.class));
        verify(singleObserver, never()).onSuccess(expected);
    }

    @Test
    public void testToMultimapWithMapThrowingFactory() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd", "eee", "fff");

        Callable<Map<Integer, Collection<String>>> mapFactory = new Callable<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> call() {
                throw new RuntimeException("Forced failure");
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = toMultimap(source
                , lengthFunc, new Function1<String, String>() {
                    @Override
                    public String invoke(String v) {
                        return v;
                    }
                }, mapFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, times(1)).onError(any(Throwable.class));
        verify(singleObserver, never()).onSuccess(expected);
    }

    @Test
    public void testToMultimapWithThrowingCollectionFactory() {
        Flowable<String> source = Flowable.just("cc", "cc", "eee", "eee");

        Function1<Integer, Collection<String>> collectionFactory = new Function1<Integer, Collection<String>>() {
            @Override
            public Collection<String> invoke(Integer t1) {
                if (t1 == 2) {
                    throw new RuntimeException("Forced failure");
                } else {
                    return new HashSet<String>();
                }
            }
        };

        Function1<String, String> identity = new Function1<String, String>() {
            @Override
            public String invoke(String v) {
                return v;
            }
        };
        Callable<Map<Integer, Collection<String>>> mapSupplier = new Callable<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> call() {
                return new HashMap<Integer, Collection<String>>();
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = toMultimap(source, lengthFunc,
                identity, mapSupplier, collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Collections.singleton("eee"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, times(1)).onError(any(Throwable.class));
        verify(singleObserver, never()).onSuccess(expected);
    }

}
