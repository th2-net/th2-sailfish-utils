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

package com.exactpro.th2.sailfish.utils.filter.util;

import com.exactpro.sf.common.messages.IMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.stream.Collectors;

public class FilterUtils {
    public static final String DEFAULT_DECIMAL_SEPARATOR = String.valueOf(DecimalFormatSymbols.getInstance().getDecimalSeparator());


    public static Comparable<?> convertNumberValue(String value) {
        if (value.contains(DEFAULT_DECIMAL_SEPARATOR)) {
            return new BigDecimal(value);
        }
        return Long.parseLong(value);
    }

    @Nullable
    public static Comparable<?> convertNumberValue(Object value) {
        if (value instanceof String) {
            return convertNumberValue((String) value);
        } else if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Float) {
            return BigDecimal.valueOf((Float) value);
        } else if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        } else if (value instanceof Short) {
            return new BigDecimal((Short) value);
        } else if (value instanceof Integer) {
            return new BigDecimal((Integer) value);
        }

        return null;
    }

    @Nullable
    public static Comparable<?> convertToComparableValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            try {
                Comparable<?> result = convertNumberValue(value);
                return result == null ? convertDateValue(value) : result;
            } catch (NumberFormatException e) {
                return convertDateValue(value);
            }
        } catch (DateTimeParseException e) {
            if (value instanceof String) {
                return (String) value;
            }
            throw new IllegalArgumentException("Incorrect value type " + value.getClass().getCanonicalName());
        }
    }

    public static Comparable<?> convertDateValue(String value) {
        if (value.contains(":")) {
            if (value.contains("-")) {
                return LocalDateTime.parse(value);
            }
            return LocalTime.parse(value);
        }
        return LocalDate.parse(value);
    }

    @Nullable
    public static Comparable<?> convertDateValue(Object value) {
        if (value instanceof String) {
            return convertDateValue((String) value);
        } else if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof LocalTime) {
            return (LocalTime) value;
        } else if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return null;
    }

    @NotNull
    public static String getObjectType(Object value) {
        return getObjectType(value, true);
    }

    @NotNull
    public static String getObjectType(Object value, boolean addCollectionContent) {
        if (value == null) {
            return "null";
        }
        if (value instanceof IMessage) {
            return "Message";
        }

        if (value instanceof Collection<?>) {
            if (!addCollectionContent) {
                return "Collection";
            }
            Collection<?> list = (Collection<?>)value;
            return list.isEmpty()
                    ? "Empty collection"
                    : "Collection of " + getObjectType(list.iterator().next(), false) + "s";
        }

        return value.getClass().getSimpleName();
    }

    private static String collectValueTypes(Collection<?> collection) {
        return collection.stream()
                .map(Object::toString)
                .collect(Collectors.joining( ", "));
    }
}
