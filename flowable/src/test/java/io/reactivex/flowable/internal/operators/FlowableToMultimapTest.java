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
import org.reactivestreams.Subscriber;

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
import io.reactivex.flowable.TestHelper;
import kotlin.jvm.functions.Function1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableToMultimapTest {
    Subscriber<Object> objectObserver;

    @Before
    public void before() {
        objectObserver = TestHelper.mockSubscriber();
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
    public void testToMultimapFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMultimapWithValueSelectorFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicate);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMultimapWithMapFactoryFlowable() {
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

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(
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

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMultimapWithCollectionFactoryFlowable() {
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

        Flowable<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, identity, mapSupplier, collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, new HashSet<String>(Arrays.asList("eee")));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMultimapWithErrorFlowable() {
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

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFuncErr);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void testToMultimapWithErrorInValueSelectorFlowable() {
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

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicateErr);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void testToMultimapWithMapThrowingFactoryFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd", "eee", "fff");

        Callable<Map<Integer, Collection<String>>> mapFactory = new Callable<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> call() {
                throw new RuntimeException("Forced failure");
            }
        };

        Flowable<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, new Function1<String, String>() {
                    @Override
                    public String invoke(String v) {
                        return v;
                    }
                }, mapFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void testToMultimapWithThrowingCollectionFactoryFlowable() {
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

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc,
                identity, mapSupplier, collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Collections.singleton("eee"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }
}
