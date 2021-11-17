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
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.th2.common.grpc.FilterOperation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

import static com.exactpro.th2.sailfish.utils.filter.util.FilterUtils.getObjectType;


public class EqualityFilter implements IOperationFilter {

    private final Object value;
    private final boolean shouldBeEqual;


    public EqualityFilter(@NotNull String simpleFilter, boolean shouldBeEqual) {
        this.value = Objects.requireNonNull(simpleFilter, "Value cannot be converted or null");
        this.shouldBeEqual = shouldBeEqual;
    }


    @Override
    public FilterOperation getOperation() {
        return shouldBeEqual ? FilterOperation.EQUAL : FilterOperation.NOT_EQUAL;
    }

    @Override
    public ExpressionResult validate(Object actualValue) throws RuntimeException {
        validateActualValue(actualValue);
        return ExpressionResult.create(value.equals(actualValue) && shouldBeEqual);
    }

    @Override
    public String getCondition() {
        return String.valueOf(value);
    }

    @Override
    public String getCondition(Object value) {
        return getCondition();
    }

    @Override
    public Object getValue() throws MvelException {
        return value;
    }

    @Override
    public boolean hasValue() {
        return true;
    }
    
    private void validateActualValue(Object actualValue) {
        if (actualValue == null || actualValue instanceof Collection<?> || actualValue instanceof IMessage) {
            throw new IllegalArgumentException(String.format(
                    "Value type mismatch - actual: %s, expected: %s",
                    getObjectType(actualValue),
                    getObjectType(getValue())
            ));
        }
    }
}
