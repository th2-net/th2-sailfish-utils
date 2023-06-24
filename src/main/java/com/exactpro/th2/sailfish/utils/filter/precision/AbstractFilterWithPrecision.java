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
package com.exactpro.th2.sailfish.utils.filter.precision;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.MvelException;
import com.exactpro.th2.sailfish.utils.FilterSettings;
import com.exactpro.th2.sailfish.utils.filter.IOperationFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class AbstractFilterWithPrecision implements IOperationFilter {

    protected final Comparable<?> value;
    protected final FilterSettings filterSettings;


    public AbstractFilterWithPrecision(@NotNull String simpleFilter, @NotNull FilterSettings filterSettings) {
        this.value = Objects.requireNonNull(convertValue(simpleFilter), "Value cannot be converted or null");
        this.filterSettings = Objects.requireNonNull(filterSettings, "Filter settings cannot be null");
    }


    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        Comparable<?> actualValue = Objects.requireNonNull(convertValue(value), "Actual value cannot be converted or null");
        return ExpressionResult.create(compareValues(actualValue, this.value));
    }

    @Override
    public String getCondition() {
        return getValue() + " ± " + getPrecision();
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

    protected abstract Comparable<?> convertValue(Object value);

    protected abstract boolean compareValues(Comparable<?> first, Comparable<?> second);

    protected abstract String getPrecision();
}
