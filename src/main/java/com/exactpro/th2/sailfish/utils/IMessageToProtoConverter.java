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

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

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
        if (fieldValue instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal)fieldValue;
            valueBuilder.setSimpleValue((parameters.isStripTrailingZeros() ? bd.stripTrailingZeros() : bd).toPlainString());
        } else {
            valueBuilder.setSimpleValue(fieldValue.toString());
        }
        return valueBuilder;
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

    public static class Parameters {
        public static final Parameters DEFAULT = builder().build();
        private final boolean stripTrailingZeros;

        private Parameters(boolean stripTrailingZeros) {
            this.stripTrailingZeros = stripTrailingZeros;
        }

        public boolean isStripTrailingZeros() {
            return stripTrailingZeros;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean stripTrailingZeros;

            private Builder() {
            }

            public Builder setStripTrailingZeros(boolean stripTrailingZeros) {
                this.stripTrailingZeros = stripTrailingZeros;
                return this;
            }

            public Parameters build() {
                return new Parameters(stripTrailingZeros);
            }
        }
    }
}
