/*
 * Copyright 2021 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.sailfish.utils.filter.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

class FilterUtilsTest {

    public static Stream<Arguments> comparableValues() {
        return Stream.of(
                Arguments.of(1L, "1"),
                Arguments.of("text", "text"),
                Arguments.of(LocalDateTime.parse("2007-12-03T10:15:30"), "2007-12-03T10:15:30"),
                Arguments.of(new BigDecimal(10), 10),
                Arguments.of(null, null)
                
        );
    }

    @ParameterizedTest
    @MethodSource("comparableValues")
    void convertToComparableValue(Object expected, Object actual) {
        Comparable<?> comparableActualValue = FilterUtils.convertToComparableValue(actual);
        Assertions.assertEquals(expected, comparableActualValue);
    }
}