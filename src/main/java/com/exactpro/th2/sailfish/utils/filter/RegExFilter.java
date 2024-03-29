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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.th2.common.grpc.FilterOperation;

public class RegExFilter extends AbstractNotNullFilter {

    private final Pattern pattern;
    private final FilterOperation operation;

    public RegExFilter(FilterOperation operation, String value) {
        pattern = Pattern.compile(value);
        switch (operation) {
        case LIKE:
        case NOT_LIKE:
            this.operation = Objects.requireNonNull(operation);
            break;
        default:
            throw new IllegalArgumentException("Incorrect operation for {LIKE/NOT_LIKE} filter " + operation);
        }
    }

    @Override
    protected @NotNull ExpressionResult validateInternal(@NotNull Object value) {
        Objects.requireNonNull(value);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getSimpleName());
        }
        Matcher matcher = pattern.matcher((CharSequence)value);
        boolean isMatched = matcher.matches();
        if (operation == FilterOperation.LIKE) {
            return ExpressionResult.create(isMatched);
        }
        return ExpressionResult.create(!isMatched);

    }

    @Override
    public String getCondition() {
        return operation.name() + ' ' + pattern.pattern();
    }

    @Override
    public String getCondition(Object value) {
        return getCondition();
    }

    @Override
    public Object getValue() {
        return pattern;
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
