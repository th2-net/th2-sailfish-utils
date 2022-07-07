/******************************************************************************
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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
 ******************************************************************************/
package com.exactpro.th2.sailfish.utils;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Category;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.Value.Builder;

public class IMessageToProtoConverter {
    private final Parameters parameters;

    public IMessageToProtoConverter() {
        this(Parameters.DEFAULT);
    }

    public IMessageToProtoConverter(Parameters parameters) {
        this.parameters = requireNonNull(parameters, "'Parameters' parameter");
    }

    public Message.Builder toProtoMessage(IMessage message) {
        Message.Builder builder = Message.newBuilder();
        builder.getMetadataBuilder().setMessageType(message.getName());
        for (String fieldName : message.getFieldNames()) {
            Object fieldValue = message.getField(fieldName);
            Value convertedValue = convertToValue(fieldValue);
            builder.putFields(fieldName, convertedValue);
        }
        return builder;
    }

    private Value convertToValue(Object fieldValue) {
        Value.Builder valueBuilder = Value.newBuilder();
        if (fieldValue instanceof IMessage) {
            Message nestedMessage = convertComplex((IMessage) fieldValue);
            valueBuilder.setMessageValue(nestedMessage);
        } else if (fieldValue instanceof List<?>) {
            ListValue listValue = convertToListValue(fieldValue);
            valueBuilder.setListValue(listValue);
        } else {
            addSimpleValue(fieldValue, valueBuilder);
        }
        return valueBuilder.build();
    }

    @NotNull
    private ListValue convertToListValue(Object fieldValue) {
        ListValue.Builder listBuilder = ListValue.newBuilder();
        var fieldList = (List<?>)fieldValue;
        if (!fieldList.isEmpty() && fieldList.get(0) instanceof IMessage) {
            fieldList.forEach(message -> listBuilder.addValues(
                            Value.newBuilder()
                            .setMessageValue(convertComplex((IMessage)message))
                            .build()
                    ));
        } else {
            fieldList.forEach(value -> listBuilder.addValues(
                            addSimpleValue(value, Value.newBuilder())
                    ));
        }
        return listBuilder.build();
    }

    private Builder addSimpleValue(Object fieldValue, Builder valueBuilder) {
        String result = toSimpleValueString(fieldValue);
        return valueBuilder.setSimpleValue(result);
    }

    private String toSimpleValueString(Object fieldValue) {
        if (fieldValue instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal)fieldValue;
            return (parameters.isStripTrailingZeros() ? bd.stripTrailingZeros() : bd).toPlainString();
        }
        if (fieldValue instanceof LocalDateTime) {
            return ((LocalDateTime)fieldValue).format(parameters.getDateTimeFormatter());
        }
        if (fieldValue instanceof LocalTime) {
            return ((LocalTime)fieldValue).format(parameters.getTimeFormatter());
        }
        return fieldValue.toString();
    }

    private Message convertComplex(IMessage fieldValue) {
        IMessage nestedMessage = fieldValue;
        Message.Builder nestedMessageBuilder = Message.newBuilder();
        for (String fieldName : nestedMessage.getFieldNames()) {
            Value convertedValue = convertToValue(nestedMessage.getField(fieldName));
            nestedMessageBuilder.putFields(fieldName, convertedValue);
        }
        return nestedMessageBuilder.build();
    }

    public static Parameters.Builder parametersBuilder() {
        return new Parameters.Builder();
    }

    public static class Parameters {
        public static final Parameters DEFAULT = parametersBuilder().build();

        private final boolean stripTrailingZeros;

        private final DateTimeFormatter dateTimeFormatter;

        private final DateTimeFormatter timeFormatter;

        private Parameters(boolean stripTrailingZeros, DateTimeFormatter dateTimeFormatter, DateTimeFormatter timeFormatter) {
            this.stripTrailingZeros = stripTrailingZeros;
            this.dateTimeFormatter = requireNonNull(dateTimeFormatter, "'Date time formatter' parameter");
            this.timeFormatter = requireNonNull(timeFormatter, "'Time formatter' parameter");
        }

        public boolean isStripTrailingZeros() {
            return stripTrailingZeros;
        }

        public DateTimeFormatter getDateTimeFormatter() {
            return dateTimeFormatter;
        }

        public DateTimeFormatter getTimeFormatter() {
            return timeFormatter;
        }

        public static class Builder {
            private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(ISO_LOCAL_DATE)
                    .appendLiteral('T')
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .appendFraction(NANO_OF_SECOND, 3, 9, true)
                    .toFormatter(Locale.getDefault(Category.FORMAT));

            private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .appendFraction(NANO_OF_SECOND, 3, 9, true)
                    .toFormatter(Locale.getDefault(Category.FORMAT));
            private boolean stripTrailingZeros;

            private DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER;

            private DateTimeFormatter timeFormatter = TIME_FORMATTER;

            private Builder() {
            }

            public Builder setStripTrailingZeros(boolean stripTrailingZeros) {
                this.stripTrailingZeros = stripTrailingZeros;
                return this;
            }

            public Builder setDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
                this.dateTimeFormatter = dateTimeFormatter;
                return this;
            }

            public Builder setTimeFormatter(DateTimeFormatter timeFormatter) {
                this.timeFormatter = timeFormatter;
                return this;
            }

            public Parameters build() {
                return new Parameters(stripTrailingZeros, dateTimeFormatter, timeFormatter);
            }
        }
    }
}
