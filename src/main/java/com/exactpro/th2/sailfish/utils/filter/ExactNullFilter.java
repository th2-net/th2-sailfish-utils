/**
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
package com.exactpro.th2.sailfish.utils.filter;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.MvelException;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.sailfish.utils.filter.util.FilterUtils;

public class ExactNullFilter implements IOperationFilter {
    private final boolean equal;

    public ExactNullFilter(boolean equal) {
        this.equal = equal;
    }

    @Override
    public FilterOperation getOperation() {
        return equal ? FilterOperation.EQUAL : FilterOperation.NOT_EQUAL;
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        if (value == null) {
            throw new IllegalArgumentException("The value is missing");
        }
        boolean isExistingNullValue = value == FilterUtils.NULL_VALUE;
        return ExpressionResult.create(isExistingNullValue == equal);
    }

    @Override
    public String getCondition() {
        return equal ? "is null" : "is not null";
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
}
