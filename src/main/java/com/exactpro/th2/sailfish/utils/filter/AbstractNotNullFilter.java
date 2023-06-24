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
import com.exactpro.th2.sailfish.utils.filter.util.FilterUtils;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNotNullFilter implements IOperationFilter {
    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        if (FilterUtils.isNull(value)) {
            return ExpressionResult.create(false);
        }
        return validateInternal(value);
    }

    @NotNull
    protected abstract ExpressionResult validateInternal(@NotNull Object value);
}
