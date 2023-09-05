/*
 * Copyright 2021-2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.th2.common.grpc.FilterOperation;

import java.util.Objects;

import static com.exactpro.th2.sailfish.utils.filter.util.FilterUtils.NULL_VALUE;

public class NullFilter implements IOperationFilter {
    private static final String NOT_NULL = "*";
    private static final String NULL = "#";
    private final boolean acceptNull;
    private final boolean checkNullValueAsEmpty;

    private NullFilter(boolean acceptNull, boolean checkNullValueAsEmpty) {
        this.acceptNull = acceptNull;
        this.checkNullValueAsEmpty = checkNullValueAsEmpty;
    }

    public static IOperationFilter nullValue(boolean checkNullValueAsEmpty) {
        return new NullFilter(true, checkNullValueAsEmpty);
    }

    public static IOperationFilter notNullValue(boolean checkNullValueAsEmpty) {
        return new NullFilter(false, checkNullValueAsEmpty);
    }

    @Override
    public FilterOperation getOperation() {
        return acceptNull ? FilterOperation.EMPTY : FilterOperation.NOT_EMPTY;
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        if (value == NULL_VALUE) {
            return ExpressionResult.create(checkNullValueAsEmpty == acceptNull);
        }
        return ExpressionResult.create(Objects.isNull(value) == acceptNull);
    }

    @Override
    public String getCondition() {
        return acceptNull ? NULL : NOT_NULL;
    }

    @Override
    public String getCondition(Object value) {
        return getCondition();
    }

    @Override
    public Object getValue() {
        throw new UnsupportedOperationException("getValue method is not implemented");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public String toString() {
        return getCondition();
    }
}
