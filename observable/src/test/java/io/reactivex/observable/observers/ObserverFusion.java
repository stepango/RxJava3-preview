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

package io.reactivex.observable.observers;

import io.reactivex.observable.Observable;
import io.reactivex.observable.extensions.QueueDisposable;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Utility methods that return functional interfaces to support assertions regarding fusion
 * in a TestObserver.
 * <p>Don't move this class as it needs package-private access to TestObserver's internals.
 */
public enum ObserverFusion {
    ;

    /**
     * Returns a function that takes a Flowable and returns a TestObserver that
     * is set up according to the parameters and is subscribed to the Flowable.
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueDisposable.ANY, false))
     * .assertResult(0);
     * </pre>
     * @param <T> the value type
     * @param mode the fusion mode to request, see {@link QueueDisposable} constants.
     * @param cancelled should the TestObserver cancelled before the subscription even happens?
     * @return the new Function instance
     */
    public static <T> Function1<Observable<T>, TestObserver<T>> test(
            final int mode, final boolean cancelled) {
        return new TestFunctionCallback<T>(mode, cancelled);
    }

    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
     * the upstream is Fuseable (sent a QueueDisposable subclass in onSubscribe).
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueDisposable.ANY, false))
     * .assertOf(ObserverFusion.assertFuseable());
     * </pre>
     * @param <T> the value type
     * @return the new Consumer instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Function1<TestObserver<T>, Unit> assertFuseable() {
        return (Function1) AssertFuseable.INSTANCE;
    }

    static final class AssertFusionConsumer<T> implements Function1<TestObserver<T>, Unit> {
        private final int mode;

        AssertFusionConsumer(int mode) {
            this.mode = mode;
        }

        @Override
        public Unit invoke(TestObserver<T> ts) {
            ts.assertFusionMode(mode);
            return Unit.INSTANCE;
        }
    }

    static final class TestFunctionCallback<T> implements Function1<Observable<T>, TestObserver<T>> {
        private final int mode;
        private final boolean cancelled;

        TestFunctionCallback(int mode, boolean cancelled) {
            this.mode = mode;
            this.cancelled = cancelled;
        }

        @Override
        public TestObserver<T> invoke(Observable<T> t) {
            TestObserver<T> ts = new TestObserver<T>();
            ts.setInitialFusionMode(mode);
            if (cancelled) {
                ts.cancel();
            }
            t.subscribe(ts);
            return ts;
        }
    }

    enum AssertFuseable implements Function1<TestObserver<Object>, Unit> {
        INSTANCE;
        @Override
        public Unit invoke(TestObserver<Object> ts) {
            ts.assertFuseable();
            return Unit.INSTANCE;
        }
    }

    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
     * the upstream is not Fuseable (didn't sent a QueueDisposable subclass in onSubscribe).
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueDisposable.ANY, false))
     * .assertOf(ObserverFusion.assertNotFuseable());
     * </pre>
     * @param <T> the value type
     * @return the new Consumer instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Function1<TestObserver<T>, Unit> assertNotFuseable() {
        return (Function1) AssertNotFuseable.INSTANCE;
    }

    enum AssertNotFuseable implements Function1<TestObserver<Object>, Unit> {
        INSTANCE;
        @Override
        public Unit invoke(TestObserver<Object> ts) {
            ts.assertNotFuseable();
            return Unit.INSTANCE;
        }
    }

    /**
     * Returns a Consumer that asserts on its TestObserver parameter that
     * the upstream is Fuseable (sent a QueueDisposable subclass in onSubscribe)
     * and the established the given fusion mode.
     * <p>
     * Use this as follows:
     * <pre>
     * source
     * .to(ObserverFusion.test(0, QueueDisposable.ANY, false))
     * .assertOf(ObserverFusion.assertFusionMode(QueueDisposable.SYNC));
     * </pre>
     * @param <T> the value type
     * @param mode the expected established fusion mode, see {@link QueueDisposable} constants.
     * @return the new Consumer instance
     */
    public static <T> Function1<TestObserver<T>, Unit> assertFusionMode(final int mode) {
        return new AssertFusionConsumer<T>(mode);
    }


    /**
     * Constructs a TestObserver with the given required fusion mode.
     * @param <T> the value type
     * @param mode the requested fusion mode, see {@link QueueDisposable} constants
     * @return the new TestObserver
     */
    public static <T> TestObserver<T> newTest(int mode) {
        TestObserver<T> ts = new TestObserver<T>();
        ts.setInitialFusionMode(mode);
        return ts;
    }

    /**
     * Assert that the TestObserver received a fuseabe QueueDisposable and
     * is in the given fusion mode.
     * @param <T> the value type
     * @param ts the TestObserver instance
     * @param mode the expected mode
     * @return the TestObserver
     */
    public static <T> TestObserver<T> assertFusion(TestObserver<T> ts, int mode) {
        return ts.assertOf(ObserverFusion.<T>assertFuseable())
                .assertOf(ObserverFusion.<T>assertFusionMode(mode));
    }
}
