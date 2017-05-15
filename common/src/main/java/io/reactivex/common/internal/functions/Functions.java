/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.common.internal.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Notification;
import io.reactivex.common.RxJavaCommonPlugins;
import io.reactivex.common.Scheduler;
import io.reactivex.common.Timed;
import io.reactivex.common.exceptions.OnErrorNotImplementedException;
import kotlin.jvm.functions.Function2;
import io.reactivex.common.functions.Function3;
import io.reactivex.common.functions.Function4;
import io.reactivex.common.functions.Function5;
import kotlin.jvm.functions.Function6;
import kotlin.jvm.functions.Function7;
import kotlin.jvm.functions.Function8;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function9;

/**
 * Utility methods to convert the BiFunction, Function3..Function9 instances to Function of Object array.
 */
public final class Functions {

    /**
     * Utility class.
     */
    private Functions() {
        throw new IllegalStateException("No instances!");
    }

    public static <T1, T2, R> Function1<Object[], R> toFunction(final Function2<? super T1, ? super T2, ? extends R> f) {
        ObjectHelper.requireNonNull(f, "f is null");
        return new Array2Func<T1, T2, R>(f);
    }

    public static <T1, T2, T3, R> Function1<Object[], R> toFunction(final Function3<T1, T2, T3, R> f) {
        ObjectHelper.requireNonNull(f, "f is null");
        return new Array3Func<T1, T2, T3, R>(f);
    }

    public static <T1, T2, T3, T4, R> Function1<Object[], R> toFunction(final Function4<T1, T2, T3, T4, R> f) {
        ObjectHelper.requireNonNull(f, "f is null");
        return new Array4Func<T1, T2, T3, T4, R>(f);
    }

    public static <T1, T2, T3, T4, T5, R> Function1<Object[], R> toFunction(final Function5<T1, T2, T3, T4, T5, R> f) {
        ObjectHelper.requireNonNull(f, "f is null");
        return new Array5Func<T1, T2, T3, T4, T5, R>(f);
    }

    public static <T1, T2, T3, T4, T5, T6, R> Function1<Object[], R> toFunction(
            final Function6<T1, T2, T3, T4, T5, T6, R> f) {
        ObjectHelper.requireNonNull(f, "f is null");
        return new Array6Func<T1, T2, T3, T4, T5, T6, R>(f);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, R> Function1<Object[], R> toFunction(
            final Function7<T1, T2, T3, T4, T5, T6, T7, R> f) {
        ObjectHelper.requireNonNull(f, "f is null");
        return new Array7Func<T1, T2, T3, T4, T5, T6, T7, R>(f);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function1<Object[], R> toFunction(
            final Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) {
        ObjectHelper.requireNonNull(f, "f is null");
        return new Array8Func<T1, T2, T3, T4, T5, T6, T7, T8, R>(f);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Function1<Object[], R> toFunction(
            final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> f) {
        ObjectHelper.requireNonNull(f, "f is null");
        return new Array9Func<T1, T2, T3, T4, T5, T6, T7, T8, T9, R>(f);
    }

    /**
     * A singleton identity function.
     */
    static final Function1<Object, Object> IDENTITY = new Identity();

    /**
     * Returns an identity function that simply returns its argument.
     *
     * @param <T> the input and output value type
     * @return the identity function
     */
    @SuppressWarnings("unchecked")
    public static <T> Function1<T, T> identity() {
        return (Function1<T, T>) IDENTITY;
    }

    public static final Runnable EMPTY_RUNNABLE = new EmptyRunnable();

    public static final Function0 EMPTY_ACTION = new EmptyAction();

    static final Function1<Object, Unit> EMPTY_CONSUMER = new EmptyConsumer();

    /**
     * Returns an empty consumer that does nothing.
     *
     * @param <T> the consumed value type, the value is ignored
     * @return an empty consumer that does nothing.
     */
    @SuppressWarnings("unchecked")
    public static <T> Function1<T, Unit> emptyConsumer() {
        return (Function1<T, Unit>) EMPTY_CONSUMER;
    }

    public static final Function1<Throwable, Unit> ERROR_CONSUMER = new ErrorConsumer();

    /**
     * Wraps the consumed Throwable into an OnErrorNotImplementedException and
     * signals it to the plugin error handler.
     */
    public static final Function1<Throwable, Unit> ON_ERROR_MISSING = new OnErrorMissingConsumer();

    public static final Function1<Long, Unit> EMPTY_LONG_CONSUMER = new EmptyLongConsumer();

    static final Function1<Object, Boolean> ALWAYS_TRUE = new TruePredicate();

    static final Function1<Object, Boolean> ALWAYS_FALSE = new FalsePredicate();

    static final Callable<Object> NULL_SUPPLIER = new NullCallable();

    static final Comparator<Object> NATURAL_COMPARATOR = new NaturalObjectComparator();

    @SuppressWarnings("unchecked")
    public static <T> Function1<T, Boolean> alwaysTrue() {
        return (Function1<T, Boolean>) ALWAYS_TRUE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Function1<T, Boolean> alwaysFalse() {
        return (Function1<T, Boolean>) ALWAYS_FALSE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Callable<T> nullSupplier() {
        return (Callable<T>) NULL_SUPPLIER;
    }

    /**
     * Returns a natural order comparator which casts the parameters to Comparable.
     *
     * @param <T> the value type
     * @return a natural order comparator which casts the parameters to Comparable
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalOrder() {
        return (Comparator<T>) NATURAL_COMPARATOR;
    }

    static final class FutureAction implements Function0 {
        final Future<?> future;

        FutureAction(Future<?> future) {
            this.future = future;
        }

        @Override
        public Unit invoke() {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return Unit.INSTANCE;
        }
    }

    /**
     * Wraps the blocking get call of the Future into an Action.
     *
     * @param future the future to call get() on, not null
     * @return the new Action instance
     */
    public static Function0 futureAction(Future<?> future) {
        return new FutureAction(future);
    }

    static final class JustValue<T, U> implements Callable<U>, Function1<T, U> {
        final U value;

        JustValue(U value) {
            this.value = value;
        }

        @Override
        public U call() throws Exception {
            return value;
        }

        @Override
        public U invoke(T t) {
            return value;
        }
    }

    /**
     * Returns a Callable that returns the given value.
     *
     * @param <T>   the value type
     * @param value the value to return
     * @return the new Callable instance
     */
    public static <T> Callable<T> justCallable(T value) {
        return new JustValue<Object, T>(value);
    }

    /**
     * Returns a Function that ignores its parameter and returns the given value.
     *
     * @param <T>   the function's input type
     * @param <U>   the value and return type of the function
     * @param value the value to return
     * @return the new Function instance
     */
    public static <T, U> Function1<T, U> justFunction(U value) {
        return new JustValue<T, U>(value);
    }

    static final class CastToClass<T, U> implements Function1<T, U> {
        final Class<U> clazz;

        CastToClass(Class<U> clazz) {
            this.clazz = clazz;
        }

        @Override
        public U invoke(T t) {
            return clazz.cast(t);
        }
    }

    /**
     * Returns a function that cast the incoming values via a Class object.
     *
     * @param <T>    the input value type
     * @param <U>    the output and target type
     * @param target the target class
     * @return the new Function instance
     */
    public static <T, U> Function1<T, U> castFunction(Class<U> target) {
        return new CastToClass<T, U>(target);
    }

    static final class ArrayListCapacityCallable<T> implements Callable<List<T>> {
        final int capacity;

        ArrayListCapacityCallable(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public List<T> call() throws Exception {
            return new ArrayList<T>(capacity);
        }
    }

    public static <T> Callable<List<T>> createArrayList(int capacity) {
        return new ArrayListCapacityCallable<T>(capacity);
    }

    static final class EqualsPredicate<T> implements Function1<T, Boolean> {
        final T value;

        EqualsPredicate(T value) {
            this.value = value;
        }

        @Override
        public Boolean invoke(T t) {
            return ObjectHelper.equals(t, value);
        }
    }

    public static <T> Function1<T, Boolean> equalsWith(T value) {
        return new EqualsPredicate<T>(value);
    }

    enum HashSetCallable implements Callable<Set<Object>> {
        INSTANCE;

        @Override
        public Set<Object> call() throws Exception {
            return new HashSet<Object>();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> Callable<Set<T>> createHashSet() {
        return (Callable) HashSetCallable.INSTANCE;
    }

    static final class NotificationOnNext<T> implements Function1<T, Unit> {
        final Function1<? super Notification<T>, Unit> onNotification;

        NotificationOnNext(Function1<? super Notification<T>, Unit> onNotification) {
            this.onNotification = onNotification;
        }

        @Override
        public Unit invoke(T v) {
            onNotification.invoke(Notification.createOnNext(v));
            return Unit.INSTANCE;
        }
    }

    static final class NotificationOnError<T> implements Function1<Throwable, Unit> {
        final Function1<? super Notification<T>, Unit> onNotification;

        NotificationOnError(Function1<? super Notification<T>, Unit> onNotification) {
            this.onNotification = onNotification;
        }

        @Override
        public Unit invoke(Throwable v) {
            onNotification.invoke(Notification.<T>createOnError(v));
            return Unit.INSTANCE;
        }
    }

    static final class NotificationOnComplete<T> implements Function0 {
        final Function1<? super Notification<T>, Unit> onNotification;

        NotificationOnComplete(Function1<? super Notification<T>, Unit> onNotification) {
            this.onNotification = onNotification;
        }

        @Override
        public kotlin.Unit invoke() {
            try {
                onNotification.invoke(Notification.<T>createOnComplete());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Unit.INSTANCE;
        }
    }

    public static <T> Function1<T, Unit> notificationOnNext(Function1<? super Notification<T>, Unit> onNotification) {
        return new NotificationOnNext<T>(onNotification);
    }

    public static <T> Function1<Throwable, Unit> notificationOnError(Function1<? super Notification<T>, Unit> onNotification) {
        return new NotificationOnError<T>(onNotification);
    }

    public static <T> Function0 notificationOnComplete(Function1<? super Notification<T>, Unit> onNotification) {
        return new NotificationOnComplete<T>(onNotification);
    }

    static final class ActionConsumer<T> implements Function1<T, Unit> {
        final Function0 action;

        ActionConsumer(Function0 action) {
            this.action = action;
        }

        @Override
        public Unit invoke(T t) {
            action.invoke();
            return Unit.INSTANCE;
        }
    }

    public static <T> Function1<T, Unit> actionConsumer(Function0 action) {
        return new ActionConsumer<T>(action);
    }

    static final class ClassFilter<T, U> implements Function1<T, Boolean> {
        final Class<U> clazz;

        ClassFilter(Class<U> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Boolean invoke(T t) {
            return clazz.isInstance(t);
        }
    }

    public static <T, U> Function1<T, Boolean> isInstanceOf(Class<U> clazz) {
        return new ClassFilter<T, U>(clazz);
    }

    static final class BooleanSupplierPredicateReverse<T> implements Function1<T, Boolean> {
        final Function0<Boolean> supplier;

        BooleanSupplierPredicateReverse(Function0<Boolean> supplier) {
            this.supplier = supplier;
        }

        @Override
        public Boolean invoke(T t) {
            return !supplier.invoke();
        }
    }

    public static <T> Function1<T, Boolean> predicateReverseFor(Function0<Boolean> supplier) {
        return new BooleanSupplierPredicateReverse<T>(supplier);
    }

    static final class TimestampFunction<T> implements Function1<T, Timed<T>> {
        final TimeUnit unit;

        final Scheduler scheduler;

        TimestampFunction(TimeUnit unit, Scheduler scheduler) {
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Timed<T> invoke(T t) {
            return new Timed<T>(t, scheduler.now(unit), unit);
        }
    }

    public static <T> Function1<T, Timed<T>> timestampWith(TimeUnit unit, Scheduler scheduler) {
        return new TimestampFunction<T>(unit, scheduler);
    }

    static final class ToMapKeySelector<K, T> implements Function2<Map<K, T>, T, kotlin.Unit> {
        private final Function1<? super T, ? extends K> keySelector;

        ToMapKeySelector(Function1<? super T, ? extends K> keySelector) {
            this.keySelector = keySelector;
        }

        @Override
        public Unit invoke(Map<K, T> m, T t) {
            K key = null;
            try {
                key = keySelector.invoke(t);
            } catch (Exception e) {
                //TODO checked exception
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
            m.put(key, t);
            return Unit.INSTANCE;
        }
    }

    public static <T, K> Function2<Map<K, T>, T, kotlin.Unit> toMapKeySelector(final Function1<? super T, ? extends K> keySelector) {
        return new ToMapKeySelector<K, T>(keySelector);
    }

    static final class ToMapKeyValueSelector<K, V, T> implements Function2<Map<K, V>, T, kotlin.Unit> {
        private final Function1<? super T, ? extends V> valueSelector;
        private final Function1<? super T, ? extends K> keySelector;

        ToMapKeyValueSelector(Function1<? super T, ? extends V> valueSelector,
                              Function1<? super T, ? extends K> keySelector) {
            this.valueSelector = valueSelector;
            this.keySelector = keySelector;
        }

        @Override
        public Unit invoke(Map<K, V> m, T t) {
            K key = null;
            V value = null;
            try {
                key = keySelector.invoke(t);
                value = valueSelector.invoke(t);
            } catch (Exception e) {
                //TODO checked exception
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
            m.put(key, value);
            return Unit.INSTANCE;
        }
    }

    public static <T, K, V> Function2<Map<K, V>, T, kotlin.Unit> toMapKeyValueSelector(final Function1<? super T, ? extends K> keySelector, final Function1<? super T, ? extends V> valueSelector) {
        return new ToMapKeyValueSelector<K, V, T>(valueSelector, keySelector);
    }

    static final class ToMultimapKeyValueSelector<K, V, T> implements Function2<Map<K, Collection<V>>, T, kotlin.Unit> {
        private final Function1<? super K, ? extends Collection<? super V>> collectionFactory;
        private final Function1<? super T, ? extends V> valueSelector;
        private final Function1<? super T, ? extends K> keySelector;

        ToMultimapKeyValueSelector(Function1<? super K, ? extends Collection<? super V>> collectionFactory,
                                   Function1<? super T, ? extends V> valueSelector, Function1<? super T, ? extends K> keySelector) {
            this.collectionFactory = collectionFactory;
            this.valueSelector = valueSelector;
            this.keySelector = keySelector;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Unit invoke(Map<K, Collection<V>> m, T t) {
            try {
                K key = keySelector.invoke(t);
                Collection<V> coll = m.get(key);
                if (coll == null) {
                    coll = (Collection<V>) collectionFactory.invoke(key);
                    m.put(key, coll);
                }

                V value = valueSelector.invoke(t);

                coll.add(value);
            } catch (Exception e) {
                //TODO checked exception
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
            return Unit.INSTANCE;
        }
    }

    public static <T, K, V> Function2<Map<K, Collection<V>>, T, kotlin.Unit> toMultimapKeyValueSelector(
            final Function1<? super T, ? extends K> keySelector, final Function1<? super T, ? extends V> valueSelector,
            final Function1<? super K, ? extends Collection<? super V>> collectionFactory) {
        return new ToMultimapKeyValueSelector<K, V, T>(collectionFactory, valueSelector, keySelector);
    }

    enum NaturalComparator implements Comparator<Object> {
        INSTANCE;

        @SuppressWarnings("unchecked")
        @Override
        public int compare(Object o1, Object o2) {
            return ((Comparable<Object>) o1).compareTo(o2);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalComparator() {
        return (Comparator<T>) NaturalComparator.INSTANCE;
    }

    static final class ListSorter<T> implements Function1<List<T>, List<T>> {
        final Comparator<? super T> comparator;

        ListSorter(Comparator<? super T> comparator) {
            this.comparator = comparator;
        }

        @Override
        public List<T> invoke(List<T> v) {
            Collections.sort(v, comparator);
            return v;
        }
    }

    public static <T> Function1<List<T>, List<T>> listSorter(final Comparator<? super T> comparator) {
        return new ListSorter<T>(comparator);
    }

    static final class Array2Func<T1, T2, R> implements Function1<Object[], R> {
        final Function2<? super T1, ? super T2, ? extends R> f;

        Array2Func(Function2<? super T1, ? super T2, ? extends R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R invoke(Object[] a) {
            if (a.length != 2) {
                throw new IllegalArgumentException("Array of size 2 expected but got " + a.length);
            }
            try {
                return f.invoke((T1) a[0], (T2) a[1]);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class Array3Func<T1, T2, T3, R> implements Function1<Object[], R> {
        final Function3<T1, T2, T3, R> f;

        Array3Func(Function3<T1, T2, T3, R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R invoke(Object[] a) {
            if (a.length != 3) {
                throw new IllegalArgumentException("Array of size 3 expected but got " + a.length);
            }
            try {
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2]);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class Array4Func<T1, T2, T3, T4, R> implements Function1<Object[], R> {
        final Function4<T1, T2, T3, T4, R> f;

        Array4Func(Function4<T1, T2, T3, T4, R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R invoke(Object[] a) {
            if (a.length != 4) {
                throw new IllegalArgumentException("Array of size 4 expected but got " + a.length);
            }
            try {
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3]);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class Array5Func<T1, T2, T3, T4, T5, R> implements Function1<Object[], R> {
        private final Function5<T1, T2, T3, T4, T5, R> f;

        Array5Func(Function5<T1, T2, T3, T4, T5, R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R invoke(Object[] a) {
            if (a.length != 5) {
                throw new IllegalArgumentException("Array of size 5 expected but got " + a.length);
            }
            try {
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4]);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class Array6Func<T1, T2, T3, T4, T5, T6, R> implements Function1<Object[], R> {
        final Function6<T1, T2, T3, T4, T5, T6, R> f;

        Array6Func(Function6<T1, T2, T3, T4, T5, T6, R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R invoke(Object[] a) {
            if (a.length != 6) {
                throw new IllegalArgumentException("Array of size 6 expected but got " + a.length);
            }
            try {
                return f.invoke((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4], (T6) a[5]);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class Array7Func<T1, T2, T3, T4, T5, T6, T7, R> implements Function1<Object[], R> {
        final Function7<T1, T2, T3, T4, T5, T6, T7, R> f;

        Array7Func(Function7<T1, T2, T3, T4, T5, T6, T7, R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R invoke(Object[] a) {
            if (a.length != 7) {
                throw new IllegalArgumentException("Array of size 7 expected but got " + a.length);
            }
            try {
                return f.invoke((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4], (T6) a[5], (T7) a[6]);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class Array8Func<T1, T2, T3, T4, T5, T6, T7, T8, R> implements Function1<Object[], R> {
        final Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f;

        Array8Func(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R invoke(Object[] a) {
            if (a.length != 8) {
                throw new IllegalArgumentException("Array of size 8 expected but got " + a.length);
            }
            try {
                return f.invoke((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4], (T6) a[5], (T7) a[6], (T8) a[7]);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class Array9Func<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> implements Function1<Object[], R> {
        final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> f;

        Array9Func(Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R invoke(Object[] a) {
            if (a.length != 9) {
                throw new IllegalArgumentException("Array of size 9 expected but got " + a.length);
            }
            try {
                return f.invoke((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4], (T6) a[5], (T7) a[6], (T8) a[7], (T9) a[8]);
            } catch (Exception e) {
                //TODO checked exceptions
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                else throw new RuntimeException(e);
            }
        }
    }

    static final class Identity implements Function1<Object, Object> {
        @Override
        public Object invoke(Object v) {
            return v;
        }

        @Override
        public String toString() {
            return "IdentityFunction";
        }
    }

    static final class EmptyRunnable implements Runnable {
        @Override
        public void run() {
        }

        @Override
        public String toString() {
            return "EmptyRunnable";
        }
    }

    static final class EmptyAction implements Function0 {
        @Override
        public kotlin.Unit invoke() {
            return Unit.INSTANCE;
        }

        @Override
        public String toString() {
            return "EmptyAction";
        }
    }

    static final class EmptyConsumer implements Function1<Object, Unit> {
        @Override
        public Unit invoke(Object v) {
            return Unit.INSTANCE;
        }

        @Override
        public String toString() {
            return "EmptyConsumer";
        }
    }

    static final class ErrorConsumer implements Function1<Throwable, Unit> {
        @Override
        public Unit invoke(Throwable error) {
            RxJavaCommonPlugins.onError(error);
            return Unit.INSTANCE;
        }
    }

    static final class OnErrorMissingConsumer implements Function1<Throwable, Unit> {
        @Override
        public Unit invoke(Throwable error) {
            RxJavaCommonPlugins.onError(new OnErrorNotImplementedException(error));
            return Unit.INSTANCE;
        }
    }

    static final class EmptyLongConsumer implements Function1<Long, Unit> {
        @Override
        public Unit invoke(Long v) {
            return Unit.INSTANCE;
        }
    }

    static final class TruePredicate implements Function1<Object, Boolean> {
        @Override
        public Boolean invoke(Object o) {
            return true;
        }
    }

    static final class FalsePredicate implements Function1<Object, Boolean> {
        @Override
        public Boolean invoke(Object o) {
            return false;
        }
    }

    static final class NullCallable implements Callable<Object> {
        @Override
        public Object call() {
            return null;
        }
    }

    static final class NaturalObjectComparator implements Comparator<Object> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public int compare(Object a, Object b) {
            return ((Comparable) a).compareTo(b);
        }
    }

}
