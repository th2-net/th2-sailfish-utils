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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.Value.Builder;

public class IMessageToProtoConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(IMessageToProtoConverter.class);
    private final Parameters parameters;
    private final IDictionaryStructure dictionaryStructure;

    public IMessageToProtoConverter() {
        this(null, Parameters.DEFAULT);
    }

    public IMessageToProtoConverter(Parameters parameters) {
        this(null, parameters);
    }

    public IMessageToProtoConverter(IDictionaryStructure dictionaryStructure, Parameters parameters) {
        this.dictionaryStructure = dictionaryStructure;
        this.parameters = requireNonNull(parameters, "'Parameters' parameter");
        if (parameters.isReplaceValuesWithEnumNames() && dictionaryStructure == null) {
            throw new IllegalArgumentException("The dictionary must be set to replace values with enums");
        }
    }

    public Message.Builder toProtoMessage(IMessage message) {
        Message.Builder builder = Message.newBuilder();
        builder.getMetadataBuilder().setMessageType(message.getName());
        IMessageStructure structure = getMessageByName(message.getName());
        for (String fieldName : message.getFieldNames()) {
            Object fieldValue = message.getField(fieldName);
            IFieldStructure filedStructure = getStructureByName(structure, fieldName);
            Value convertedValue = convertToValue(filedStructure, fieldValue);
            builder.putFields(fieldName, convertedValue);
        }
        return builder;
    }

    private Value convertToValue(IFieldStructure filedStructure, Object fieldValue) {
        Value.Builder valueBuilder = Value.newBuilder();
        if (fieldValue instanceof IMessage) {
            Message nestedMessage = convertComplex(filedStructure, (IMessage) fieldValue);
            valueBuilder.setMessageValue(nestedMessage);
        } else if (fieldValue instanceof List<?>) {
            ListValue listValue = convertToListValue(filedStructure, fieldValue);
            valueBuilder.setListValue(listValue);
        } else {
            addSimpleValue(filedStructure, fieldValue, valueBuilder);
        }
        return valueBuilder.build();
    }

    @NotNull
    private ListValue convertToListValue(IFieldStructure filedStructure, Object fieldValue) {
        ListValue.Builder listBuilder = ListValue.newBuilder();
        var fieldList = (List<?>)fieldValue;
        if (!fieldList.isEmpty() && fieldList.get(0) instanceof IMessage) {
            fieldList.forEach(message -> listBuilder.addValues(
                            Value.newBuilder()
                            .setMessageValue(convertComplex(filedStructure, (IMessage)message))
                            .build()
                    ));
        } else {
            fieldList.forEach(value -> listBuilder.addValues(
                            addSimpleValue(filedStructure, value, Value.newBuilder())
                    ));
        }
        return listBuilder.build();
    }

    private Builder addSimpleValue(IFieldStructure filedStructure, Object fieldValue, Builder valueBuilder) {
        String value;
        if (fieldValue instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal)fieldValue;
            value = (parameters.isStripTrailingZeros() ? bd.stripTrailingZeros() : bd).toPlainString();
        } else {
            value = fieldValue.toString();
        }
        if (parameters.isReplaceValuesWithEnumNames()) {
            if (filedStructure == null) {
                LOGGER.warn("cannot replace field value with enum name because structure is null");
            } else {
                String actualValue = value;
                value = filedStructure.getValues()
                        .values().stream()
                        .filter(attr -> actualValue.equals(attr.asString()))
                        .findFirst()
                        .map(IAttributeStructure::getName)
                        .orElse(actualValue);
            }
        }
        valueBuilder.setSimpleValue(value);
        return valueBuilder;
    }

    private Message convertComplex(IFieldStructure filedStructure, IMessage fieldValue) {
        IMessage nestedMessage = fieldValue;
        Message.Builder nestedMessageBuilder = Message.newBuilder();
        for (String fieldName : nestedMessage.getFieldNames()) {
            Value convertedValue = convertToValue(getStructureByName(filedStructure, fieldName), nestedMessage.getField(fieldName));
            nestedMessageBuilder.putFields(fieldName, convertedValue);
        }
        return nestedMessageBuilder.build();
    }

    @Nullable
    private IMessageStructure getMessageByName(String name) {
        if (parameters.isReplaceValuesWithEnumNames()) {
            return dictionaryStructure.getMessages().get(name);
        }
        return null;
    }

    @Nullable
    private IFieldStructure getStructureByName(@Nullable IFieldStructure struct, String name) {
        if (parameters.isReplaceValuesWithEnumNames()) {
            return struct == null ? null : struct.getFields().get(name);
        }
        return null;
    }

    public static Parameters.Builder parametersBuilder() {
        return new Parameters.Builder();
    }

    public static class Parameters {
        public static final Parameters DEFAULT = parametersBuilder().build();
        private final boolean stripTrailingZeros;
        private final boolean replaceValuesWithEnumNames;

        private Parameters(boolean stripTrailingZeros, boolean replaceValuesWithEnumNames) {
            this.stripTrailingZeros = stripTrailingZeros;
            this.replaceValuesWithEnumNames = replaceValuesWithEnumNames;
        }

        public boolean isStripTrailingZeros() {
            return stripTrailingZeros;
        }

        public boolean isReplaceValuesWithEnumNames() {
            return replaceValuesWithEnumNames;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean stripTrailingZeros;
            private boolean replaceValuesWithEnumNames;

            private Builder() {
            }

            public Builder setStripTrailingZeros(boolean stripTrailingZeros) {
                this.stripTrailingZeros = stripTrailingZeros;
                return this;
            }

            public Builder setReplaceValuesWithEnumNames(boolean replaceValuesWithEnumNames) {
                this.replaceValuesWithEnumNames = replaceValuesWithEnumNames;
                return this;
            }

            public Parameters build() {
                return new Parameters(stripTrailingZeros, replaceValuesWithEnumNames);
            }
        }
    }
}
