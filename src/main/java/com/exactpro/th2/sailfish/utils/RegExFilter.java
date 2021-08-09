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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;

public class RegExFilter implements IFilter {

    private final Pattern pattern;

    public RegExFilter(String value) {
        pattern = Pattern.compile(value);
    }

    @Override
    public ExpressionResult validate(Object value) throws RuntimeException {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getSimpleName());
        }
        Matcher matcher = pattern.matcher((String)value);
        StringBuilder foundValue = new StringBuilder();
        while (matcher.find()) {
            foundValue.append((String)value, matcher.start(), matcher.end());
        }
        boolean result = false;
        if (foundValue.length() != 0) {
            result = true;
        }
        return new ExpressionResult(result, foundValue.toString());

    }

    @Override
    public String getCondition() {
        return "LIKE_" + pattern.pattern();
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
}
