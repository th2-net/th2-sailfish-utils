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

import java.util.List;
import java.util.Objects;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.th2.common.grpc.FilterOperation;

public class ListContainFilter implements IOperationFilter {

    private final List<String> values;
    private final FilterOperation operation;

    public ListContainFilter(FilterOperation operation, List<String> value) {
        values = Objects.requireNonNull(value);
        this.operation = Objects.requireNonNull(operation);
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        Objects.requireNonNull(value);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getSimpleName());
        }
        boolean isContain = values.contains(value);
        if (operation == FilterOperation.IN) {
           return ExpressionResult.create(isContain);
        }
        return ExpressionResult.create(!isContain);

    }

    @Override
    public String getCondition() {
        return operation.name() + ' ' + values;
    }

    @Override
    public String getCondition(Object value) {
        return value + " " + getCondition();
    }

    @Override
    public Object getValue() {
        return values;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public FilterOperation getOperation() {
        return operation;
    }
}
