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

import org.junit.Test;

import io.reactivex.common.TestCommonHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ObservableInternalHelperTest {

    @Test
    public void utilityClass() {
        TestCommonHelper.checkUtilityClass(ObservableInternalHelper.class);
    }

    @Test
    public void enums() {
        assertNotNull(ObservableInternalHelper.MapToInt.values()[0]);
        assertNotNull(ObservableInternalHelper.MapToInt.valueOf("INSTANCE"));

        assertNotNull(ObservableInternalHelper.ErrorMapper.values()[0]);
        assertNotNull(ObservableInternalHelper.ErrorMapper.valueOf("INSTANCE"));

        assertNotNull(ObservableInternalHelper.FilterMapper.values()[0]);
        assertNotNull(ObservableInternalHelper.FilterMapper.valueOf("INSTANCE"));
    }

    @Test
    public void mapToInt() throws Exception {
        assertEquals(0, ObservableInternalHelper.MapToInt.INSTANCE.invoke(null));
    }
}
