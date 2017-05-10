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

package io.reactivex.observable.internal.observers;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.common.Disposable;
import io.reactivex.common.Disposables;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.TestCommonHelper;
import io.reactivex.common.exceptions.CompositeException;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Action;
import io.reactivex.common.functions.Consumer;
import io.reactivex.observable.Observable;
import io.reactivex.observable.Observer;
import io.reactivex.observable.subjects.PublishSubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LambdaObserverTest {

    @Test
    public void onSubscribeThrows() {
        final List<Object> received = new ArrayList<Object>();

        LambdaObserver<Object> o = new LambdaObserver<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public void invoke() throws Exception {
                received.add(100);
            }
        }, new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
                throw new TestException();
            }
        });

        assertFalse(o.isDisposed());

        Observable.just(1).subscribe(o);

        assertTrue(received.toString(), received.get(0) instanceof TestException);
        assertEquals(received.toString(), 1, received.size());

        assertTrue(o.isDisposed());
    }

    @Test
    public void onNextThrows() {
        final List<Object> received = new ArrayList<Object>();

        LambdaObserver<Object> o = new LambdaObserver<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                throw new TestException();
            }
        },
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public void invoke() throws Exception {
                received.add(100);
            }
        }, new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
            }
        });

        assertFalse(o.isDisposed());

        Observable.just(1).subscribe(o);

        assertTrue(received.toString(), received.get(0) instanceof TestException);
        assertEquals(received.toString(), 1, received.size());

        assertTrue(o.isDisposed());
    }

    @Test
    public void onErrorThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<Object>();

            LambdaObserver<Object> o = new LambdaObserver<Object>(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    received.add(v);
                }
            },
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    throw new TestException("Inner");
                }
            }, new Action() {
                @Override
                public void invoke() throws Exception {
                    received.add(100);
                }
            }, new Consumer<Disposable>() {
                @Override
                public void accept(Disposable s) throws Exception {
                }
            });

            assertFalse(o.isDisposed());

            Observable.<Integer>error(new TestException("Outer")).subscribe(o);

            assertTrue(received.toString(), received.isEmpty());

            assertTrue(o.isDisposed());

            TestCommonHelper.assertError(errors, 0, CompositeException.class);
            List<Throwable> ce = TestCommonHelper.compositeList(errors.get(0));
            TestCommonHelper.assertError(ce, 0, TestException.class, "Outer");
            TestCommonHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onCompleteThrows() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            final List<Object> received = new ArrayList<Object>();

            LambdaObserver<Object> o = new LambdaObserver<Object>(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    received.add(v);
                }
            },
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    received.add(e);
                }
            }, new Action() {
                @Override
                public void invoke() throws Exception {
                    throw new TestException();
                }
            }, new Consumer<Disposable>() {
                @Override
                public void accept(Disposable s) throws Exception {
                }
            });

            assertFalse(o.isDisposed());

            Observable.<Integer>empty().subscribe(o);

            assertTrue(received.toString(), received.isEmpty());

            assertTrue(o.isDisposed());

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void badSourceOnSubscribe() {
        Observable<Integer> source = new Observable<Integer>() {
            @Override
            public void subscribeActual(Observer<? super Integer> s) {
                Disposable s1 = Disposables.empty();
                s.onSubscribe(s1);
                Disposable s2 = Disposables.empty();
                s.onSubscribe(s2);

                assertFalse(s1.isDisposed());
                assertTrue(s2.isDisposed());

                s.onNext(1);
                s.onComplete();
            }
        };

        final List<Object> received = new ArrayList<Object>();

        LambdaObserver<Object> o = new LambdaObserver<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public void invoke() throws Exception {
                received.add(100);
            }
        }, new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
            }
        });

        source.subscribe(o);

        assertEquals(Arrays.asList(1, 100), received);
    }
    @Test
    public void badSourceEmitAfterDone() {
        Observable<Integer> source = new Observable<Integer>() {
            @Override
            public void subscribeActual(Observer<? super Integer> s) {
                s.onSubscribe(Disposables.empty());

                s.onNext(1);
                s.onComplete();
                s.onNext(2);
                s.onError(new TestException());
                s.onComplete();
            }
        };

        final List<Object> received = new ArrayList<Object>();

        LambdaObserver<Object> o = new LambdaObserver<Object>(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                received.add(v);
            }
        },
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                received.add(e);
            }
        }, new Action() {
            @Override
            public void invoke() throws Exception {
                received.add(100);
            }
        }, new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
            }
        });

        source.subscribe(o);

        assertEquals(Arrays.asList(1, 100), received);
    }

    @Test
    public void onNextThrowsCancelsUpstream() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final List<Throwable> errors = new ArrayList<Throwable>();

        ps.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                errors.add(e);
            }
        });

        assertTrue("No observers?!", ps.hasObservers());
        assertTrue("Has errors already?!", errors.isEmpty());

        ps.onNext(1);

        assertFalse("Has observers?!", ps.hasObservers());
        assertFalse("No errors?!", errors.isEmpty());

        assertTrue(errors.toString(), errors.get(0) instanceof TestException);
    }

    @Test
    public void onSubscribeThrowsCancelsUpstream() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final List<Throwable> errors = new ArrayList<Throwable>();

        ps.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                errors.add(e);
            }
        }, new Action() {
            @Override
            public void invoke() throws Exception {
            }
        }, new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
                throw new TestException();
            }
        });

        assertFalse("Has observers?!", ps.hasObservers());
        assertFalse("No errors?!", errors.isEmpty());

        assertTrue(errors.toString(), errors.get(0) instanceof TestException);
    }
}
