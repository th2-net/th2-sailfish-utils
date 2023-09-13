/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.th2.sailfish.utils.filter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.sailfish.utils.filter.util.FilterUtils;

public class CompareFilter extends AbstractNotNullFilter {

    private final Comparable<?> value;
    private final String stringFormatOperation;
    private final FilterOperation operation;
    private final boolean isNumber;

    public CompareFilter(FilterOperation operation, String value) {
        Objects.requireNonNull(value);
        Exception potentialException = null;
        Comparable<?> tmpValue = null;
        try {
            tmpValue = FilterUtils.convertNumberValue(value);
        } catch (NumberFormatException e) {
            potentialException = new IllegalArgumentException("Failed to parse value to Number. Value = " + value, e);
        }
        isNumber = tmpValue != null;
        if (!isNumber) {
            try {
                tmpValue = FilterUtils.convertDateValue(value);
            } catch (DateTimeParseException ex) {
                ex.addSuppressed(potentialException);
                throw new IllegalArgumentException("Failed to parse value to Date. Value = " + value, ex);
            }
        }
        this.value = tmpValue;
        switch (operation) {
        case MORE:
            this.stringFormatOperation = ">";
            break;
        case LESS:
            this.stringFormatOperation = "<";
            break;
        case NOT_MORE:
            this.stringFormatOperation = "<=";
            break;
        case NOT_LESS:
            this.stringFormatOperation = ">=";
            break;
        default:
            throw new IllegalArgumentException("Incorrect compare operation " + operation);
        }
        this.operation = Objects.requireNonNull(operation);
    }

    @Override
    protected @NotNull ExpressionResult validateInternal(@NotNull Object value) {
        Objects.requireNonNull(value);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getSimpleName());
        }
        Comparable<?> tmpValue;
        if (isNumber) {
            try {
                tmpValue = Objects.requireNonNull(FilterUtils.convertNumberValue((String) value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Failed to parse value to Number. Value = " + value, e);
            }
        } else {
            try {
                tmpValue = FilterUtils.convertDateValue((String) value);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Failed to parse value to Date. Value = " + value, ex);
            }
        }

        return ExpressionResult.create(compareValues(tmpValue, this.value));

    }

    @Override
    public String getCondition() {
        return stringFormatOperation + getValue();
    }

    @Override
    public String getCondition(Object value) {
        return getCondition();
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    private boolean compareValues(Comparable<?> first, Comparable<?> second) {
        int result = compareValueWithoutOperation(first, second);
        switch (operation) {
        case MORE:
            return result > 0;
        case LESS:
            return result < 0;
        case NOT_MORE:
            return result <= 0;
        case NOT_LESS:
            return result >= 0;
        default:
            throw new IllegalArgumentException("Incorrect math operation " + operation);
        }
    }

    private int compareValueWithoutOperation(Comparable<?> first, Comparable<?> second) {
        if (first.getClass() == second.getClass()) {
            if (first instanceof LocalDate) {
                return  ((LocalDate)first).compareTo((LocalDate)second);
            }
            if (first instanceof LocalDateTime) {
                return ((LocalDateTime)first).compareTo((LocalDateTime)second);
            }
            if (first instanceof LocalTime) {
                return ((LocalTime)first).compareTo((LocalTime)second);
            }
            if (first instanceof BigDecimal) {
                return ((BigDecimal)first).compareTo((BigDecimal)second);
            }
            if (first instanceof BigInteger) {
                return ((BigInteger) first).compareTo((BigInteger) second);
            }
            return ((Long)first).compareTo((Long)second);
        }
        if (first instanceof LocalDate || first instanceof LocalDateTime || first instanceof LocalTime) {
            throw new IllegalArgumentException(String.format("Failed to compare Temporal values {%s}, {%s}", first, second));
        }
        if (second instanceof LocalDate || second instanceof LocalDateTime || second instanceof LocalTime) {
            throw new IllegalArgumentException(String.format("Failed to compare Temporal values {%s}, {%s}", first, second));
        }
        var result = compareWithTransformation(BigDecimal.class, first, second, it -> new BigDecimal(it.toString()));
        if (result.matchType) {
            return result.comparisonResult;
        }
        result = compareWithTransformation(BigInteger.class, first, second, it -> new BigInteger(it.toString()));
        if (result.matchType) {
            return result.comparisonResult;
        }
        return Long.valueOf(first.toString()).compareTo(Long.valueOf(second.toString()));
    }

    @Override
    public FilterOperation getOperation() {
        return operation;
    }

    private static class CompareResult {
        private final boolean matchType;
        private final int comparisonResult;

        private CompareResult(boolean matchType, int result) {
            this.matchType = matchType;
            this.comparisonResult = result;
        }
    }

    private static CompareResult match(int result) {
        return new CompareResult(true, result);
    }

    private static CompareResult mismatch() {
        return new CompareResult(false, 0);
    }

    private static <T extends Comparable<T>> CompareResult compareWithTransformation(
            Class<T> type,
            Object first,
            Object second,
            Function<Object, T> transform
    ) {
        if (type.isInstance(first)) {
            return match(type.cast(first).compareTo(transform.apply(second)));
        }
        if (type.isInstance(second)) {
            return match(transform.apply(first).compareTo(type.cast(second)));
        }
        return mismatch();
    }
}
