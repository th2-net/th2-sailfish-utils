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
package com.exactpro.th2.sailfish.utils;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Objects;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.th2.common.grpc.FilterOperation;

public class MathFilter implements IFilter {

    private final Object value;
    private final String stringFormatOperation;
    private final FilterOperation operation;
    private boolean isNumber;

    public MathFilter(FilterOperation operation, String value) {
        Objects.requireNonNull(value);
        Exception potentialException = null;
        Object tmpValue = null;
        try {
            tmpValue = convertValue(value);
            isNumber = true;
        } catch (NumberFormatException e) {
            potentialException = new IllegalArgumentException("Failed to parse value to Number. Value = " + value, e);
        }
        if (!isNumber) {
            try {
                tmpValue = convertDateValue(value);
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
            throw new IllegalArgumentException("Incorrect math operation " + operation);
        }
        Objects.requireNonNull(operation);
        this.operation = operation;
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        Objects.requireNonNull(value);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getSimpleName());
        }
        Comparable tmpValue;
        if (!isNumber) {
            try {
                tmpValue = (Comparable)convertDateValue((String)value);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Failed to parse value to Date. Value = " + value, ex);
            }
        } else {
            try {
                tmpValue = convertValue((String)value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Failed to parse value to Number. Value = " + value, e);
            }
        }

        return ExpressionResult.create(compareValues(tmpValue, (Comparable)this.value));

    }

    @Override
    public String getCondition() {
        return stringFormatOperation;
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

    private static Comparable convertValue(String value) {
        if (value.contains(String.valueOf(DecimalFormatSymbols.getInstance().getDecimalSeparator()))) {
            return Double.parseDouble(value);
        }
        return Long.parseLong(value);
    }

    private static Temporal convertDateValue(String value) {
        if (value.contains(":")) {
            if (value.contains("-")) {
                return LocalDateTime.parse(value);
            } else {
                return LocalTime.parse(value);
            }
        } else {
            return LocalDate.parse(value);
        }
    }

    private boolean compareValues(Comparable first, Comparable second) {
        int result;
        if (first instanceof Double || first instanceof Float || second instanceof Double || second instanceof Float) {
            result = new BigDecimal(first.toString()).compareTo(new BigDecimal(second.toString()));
        } else {
            result = first.compareTo(second);
        }
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
}
