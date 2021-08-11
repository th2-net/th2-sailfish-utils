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

import java.util.Objects;

import org.apache.commons.io.FilenameUtils;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.th2.common.grpc.FilterOperation;

public class WildcardFilter implements IFilter {

    private final String value;
    private final FilterOperation operation;

    public WildcardFilter(FilterOperation operation, String value) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(operation);
        this.value = value;
        this.operation = operation;
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        Objects.requireNonNull(value);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getSimpleName());
        }
        boolean isMatched = FilenameUtils.wildcardMatch((String)value, this.value);
        if (operation == FilterOperation.WILDCARD) {
           return ExpressionResult.create(isMatched);
        }
        return ExpressionResult.create(!isMatched);

    }

    @Override
    public String getCondition() {
        return operation.name() + '_' + value;
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
