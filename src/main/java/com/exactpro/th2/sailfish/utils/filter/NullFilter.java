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

package com.exactpro.th2.sailfish.utils.filter;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.MvelException;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.sailfish.utils.filter.util.FilterUtils;

public class NullFilter implements IOperationFilter {
    private static final String NOT_NULL = "*";
    private static final String NULL = "#";
    private final boolean acceptNull;

    private NullFilter(boolean acceptNull) {
        this.acceptNull = acceptNull;
    }

    public static IOperationFilter nullValue() {
        return new NullFilter(true);
    }

    public static IOperationFilter notNullValue() {
        return new NullFilter(false);
    }

    @Override
    public FilterOperation getOperation() {
        return acceptNull ? FilterOperation.EMPTY : FilterOperation.NOT_EMPTY;
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        return ExpressionResult.create(FilterUtils.isNull(value) == acceptNull);
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
    public Object getValue() throws MvelException {
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
