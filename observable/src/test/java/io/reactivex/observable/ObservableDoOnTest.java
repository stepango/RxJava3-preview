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

package io.reactivex.observable;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.common.exceptions.TestException;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObservableDoOnTest {

    @Test
    public void testDoOnEach() {
        final AtomicReference<String> r = new AtomicReference<String>();
        String output = Observable.just("one").doOnNext(new Function1<String, kotlin.Unit>() {
            @Override
            public Unit invoke(String v) {
                r.set(v);
                return Unit.INSTANCE;
            }
        }).blockingSingle();

        assertEquals("one", output);
        assertEquals("one", r.get());
    }

    @Test
    public void testDoOnError() {
        final AtomicReference<Throwable> r = new AtomicReference<Throwable>();
        Throwable t = null;
        try {
            Observable.<String> error(new RuntimeException("an error"))
                    .doOnError(new Function1<Throwable, kotlin.Unit>() {
                @Override
                public Unit invoke(Throwable v) {
                    r.set(v);
                    return Unit.INSTANCE;
                }
            }).blockingSingle();
            fail("expected exception, not a return value");
        } catch (Throwable e) {
            t = e;
        }

        assertNotNull(t);
        assertEquals(t, r.get());
    }

    @Test
    public void testDoOnCompleted() {
        final AtomicBoolean r = new AtomicBoolean();
        String output = Observable.just("one").doOnComplete(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                r.set(true);
                return Unit.INSTANCE;
            }
        }).blockingSingle();

        assertEquals("one", output);
        assertTrue(r.get());
    }

    @Test
    public void doOnTerminateComplete() {
        final AtomicBoolean r = new AtomicBoolean();
        String output = Observable.just("one").doOnTerminate(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                r.set(true);
                return Unit.INSTANCE;
            }
        }).blockingSingle();

        assertEquals("one", output);
        assertTrue(r.get());

    }

    @Test
    public void doOnTerminateError() {
        final AtomicBoolean r = new AtomicBoolean();
        Observable.<String>error(new TestException()).doOnTerminate(new Function0() {
            @Override
            public kotlin.Unit invoke() {
                r.set(true);
                return Unit.INSTANCE;
            }
        })
        .test()
        .assertFailure(TestException.class);
        assertTrue(r.get());
    }
}
