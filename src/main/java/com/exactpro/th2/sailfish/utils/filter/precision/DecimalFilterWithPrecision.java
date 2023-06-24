/*
 * Copyright 2020-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.th2.sailfish.utils.filter.precision;

import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.sailfish.utils.FilterSettings;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class DecimalFilterWithPrecision extends AbstractFilterWithPrecision {

    public DecimalFilterWithPrecision(@NotNull String simpleFilter, @NotNull FilterSettings filterSettings) {
        super(simpleFilter, filterSettings);
    }

    @Override
    public FilterOperation getOperation() {
        return FilterOperation.EQ_DECIMAL_PRECISION;
    }

    @Override
    protected Comparable<?> convertValue(Object value) {
        try {
            if (value instanceof String) {
                return new BigDecimal((String) value);
            } else if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Float) {
                return BigDecimal.valueOf((Float) value);
            } else if (value instanceof Double) {
                return BigDecimal.valueOf((Double) value);
            } else if (value instanceof Short) {
                return new BigDecimal((Short) value);
            } else if (value instanceof Integer) {
                return new BigDecimal((Integer) value);
            }
            throw new IllegalArgumentException("Value cannot be converted to decimal value. Value = " + value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse value to Number. Value = " + value, e);
        }
    }

    @Override
    protected boolean compareValues(Comparable<?> first, Comparable<?> second) {
        return ((BigDecimal) first).subtract((BigDecimal) second)
                .abs()
                .compareTo(BigDecimal.valueOf(filterSettings.getDecimalPrecision())) <= 0;
    }

    @Override
    protected String getPrecision() {
        return String.valueOf(filterSettings.getDecimalPrecision());
    }
}
