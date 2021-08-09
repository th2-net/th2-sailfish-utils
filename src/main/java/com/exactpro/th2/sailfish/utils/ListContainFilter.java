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

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.google.protobuf.ProtocolStringList;

public class ListContainFilter implements IFilter {

    private final ProtocolStringList values;

    public ListContainFilter(ProtocolStringList value) {
        values = value;
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getSimpleName());
        }
        if (values.contains(value)) {
            return ExpressionResult.create(true);
        }
        return ExpressionResult.create(false);
    }

    @Override
    public String getCondition() {
        return "IN";
    }

    @Override
    public String getCondition(Object value) {
        return getCondition();
    }

    @Override
    public Object getValue() {
        return values;
    }

    @Override
    public boolean hasValue() {
        return true;
    }
}
