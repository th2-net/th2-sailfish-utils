/*
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

import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.sailfish.utils.FilterSettings;
import com.exactpro.th2.sailfish.utils.filter.util.FilterUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.regex.Pattern;

public class TimeFilterWithPrecision extends AbstractFilterWithPrecision {
    
    private static final Pattern DEFAULT_TIME_PRECISION_REGEX = Pattern.compile("(\\d[HMS])(?!$)");

    public TimeFilterWithPrecision(@NotNull String simpleFilter, @NotNull FilterSettings filterSettings) {
        super(simpleFilter, filterSettings);
    }

    @Override
    public FilterOperation getOperation() {
        return FilterOperation.EQ_TIME_PRECISION;
    }


    @Override
    protected Comparable<?> convertValue(Object value) {
        try {
            return FilterUtils.convertDateValue(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Failed to parse value to Date. Value = " + value, ex);
        }
    }

    @Override
    protected boolean compareValues(Comparable<?> first, Comparable<?> second) {
        if (first.getClass() == second.getClass()) {
            if (first instanceof LocalDate || first instanceof LocalDateTime || first instanceof LocalTime) {
                return Duration.between((Temporal) first, (Temporal) second)
                        .abs()
                        .compareTo(filterSettings.getTimePrecision()) <= 0;
            }
        }

        throw new IllegalArgumentException(String.format("Failed to compare values {%s}, {%s} because it has an invalid types %s, %s", first, second, first.getClass(), second.getClass()));
    }

    @Override
    protected String getPrecision() {
        String precisionAsString = filterSettings.getTimePrecision().toString().substring(2);
        return DEFAULT_TIME_PRECISION_REGEX.matcher(precisionAsString)
                .replaceAll("$1 ")
                .toLowerCase();
    }
}
