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
import java.text.NumberFormat;
import java.text.ParseException;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.th2.common.grpc.FilterOperation;

public class MathFilter implements IFilter {

    private final Number value;
    private final String stringFormatOperation;
    private final FilterOperation operation;

    public MathFilter(FilterOperation operation, String value) throws ParseException {
        this.value = NumberFormat.getInstance().parse(value);
        switch (operation) {
        case MORE:
            this.stringFormatOperation = ">";
            break;
        case LESS:
            this.stringFormatOperation = "<";
            break;
        default:
            throw new IllegalArgumentException("Incorrect math operation " + operation);
        }
        this.operation = operation;
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getSimpleName());
        }
        try {
            value = NumberFormat.getInstance().parse((String)value);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse value to Number. " + value.getClass().getSimpleName(), e);
        }
        boolean result = false;
        switch (operation) {
            case MORE:
                result = new BigDecimal(value.toString()).compareTo(new BigDecimal(this.value.toString())) > 0;
                break;
            case LESS:
                result = new BigDecimal(value.toString()).compareTo(new BigDecimal(this.value.toString())) < 0;
                break;
        }
        return ExpressionResult.create(result);
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
}
