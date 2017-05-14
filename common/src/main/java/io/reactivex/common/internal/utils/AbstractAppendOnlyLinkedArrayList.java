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

package io.reactivex.common.internal.utils;

import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

/**
 * A linked-array-list implementation that only supports appending and consumption.
 *
 * @param <T> the value type
 */
public abstract class AbstractAppendOnlyLinkedArrayList<T> {
    protected final int capacity;
    protected final Object[] head;
    protected Object[] tail;
    protected int offset;

    /**
     * Constructs an empty list with a per-link capacity.
     * @param capacity the capacity of each link
     */
    public AbstractAppendOnlyLinkedArrayList(int capacity) {
        this.capacity = capacity;
        this.head = new Object[capacity + 1];
        this.tail = head;
    }

    /**
     * Append a non-null value to the list.
     * <p>Don't add null to the list!
     * @param value the value to append
     */
    public final void add(T value) {
        final int c = capacity;
        int o = offset;
        if (o == c) {
            Object[] next = new Object[c + 1];
            tail[c] = next;
            tail = next;
            o = 0;
        }
        tail[o] = value;
        offset = o + 1;
    }

    /**
     * Set a value as the first element of the list.
     * @param value the value to set
     */
    public final void setFirst(T value) {
        head[0] = value;
    }

    /**
     * Predicate interface suppressing the exception.
     *
     * @param <T> the value type
     */
    public interface NonThrowingPredicate<T> extends Function1<T, Boolean> {
        @Override
        Boolean invoke(T t);
    }

    /**
     * Loops over all elements of the array until a null element is encountered or
     * the given predicate returns true.
     * @param consumer the consumer of values that returns true if the forEach should terminate
     */
    @SuppressWarnings("unchecked")
    public final void forEachWhile(NonThrowingPredicate<? super T> consumer) {
        Object[] a = head;
        final int c = capacity;
        while (a != null) {
            for (int i = 0; i < c; i++) {
                Object o = a[i];
                if (o == null) {
                    break;
                }
                if (consumer.invoke((T) o)) {
                    break;
                }
            }
            a = (Object[])a[c];
        }
    }


    /**
     * Loops over all elements of the array until a null element is encountered or
     * the given predicate returns true.
     * @param <S> the extra state type
     * @param state the extra state passed into the consumer
     * @param consumer the consumer of values that returns true if the forEach should terminate
     * @throws Exception if the predicate throws
     */
    @SuppressWarnings("unchecked")
    public final <S> void forEachWhile(S state, Function2<? super S, ? super T, Boolean> consumer) throws Exception {
        Object[] a = head;
        final int c = capacity;
        for (;;) {
            for (int i = 0; i < c; i++) {
                Object o = a[i];
                if (o == null) {
                    return;
                }
                if (consumer.invoke(state, (T) o)) {
                    return;
                }
            }
            a = (Object[])a[c];
        }
    }

}
