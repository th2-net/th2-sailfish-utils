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
package com.exactpro.th2.sailfish.utils;

import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.ListValueFilter;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.MessageMetadata;
import com.exactpro.th2.common.grpc.NullValue;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.ValueFilter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public class AbstractConverterTest {
    @NotNull
    protected static Message.Builder createMessageBuilder(String messageType) {
        return Message.newBuilder()
                .setMetadata(MessageMetadata.newBuilder()
                        .setMessageType(messageType)
                        .build());
    }

    protected static Value getComplex(String messageType, Map<String, String> values) {
        Message.Builder messageBuilder = createMessageBuilder(messageType);
        for (Entry<String, String> entry : values.entrySet()) {
            messageBuilder.putFields(entry.getKey(), getSimpleValue(entry.getValue()));
        }
        return Value.newBuilder().setMessageValue(messageBuilder).build();
    }

    protected static Value getListValue(Value... listValues) {
        if (listValues == null || listValues.length == 0) {
            return Value.newBuilder().setListValue(ListValue.newBuilder().build()).build();
        }
        return Value.newBuilder().setListValue(ListValue.newBuilder().addAllValues(List.of(listValues)).build()).build();
    }

    @NotNull
    protected static Value getSimpleValue(String value) {
        return Value.newBuilder().setSimpleValue(value).build();
    }

    protected static ValueFilter simpleValueFilter(@NotNull String value, @NotNull FilterOperation operation) {
        return ValueFilter.newBuilder().setSimpleFilter(value).setOperation(operation).build();
    }

    protected static ValueFilter listValueFilter(@NotNull FilterOperation operation, @NotNull String... values) {
        return listValueFilter(value -> simpleValueFilter(value, operation), values);
    }

    protected static ValueFilter listValueFilter(@NotNull MessageFilter... values) {
        return listValueFilter(AbstractConverterTest::messageValueFilter, values);
    }

    protected static MessageFilter messageFilter(Map<String, ValueFilter> fields) {
        return MessageFilter.newBuilder().putAllFields(fields).build();
    }

    protected static ValueFilter messageValueFilter(MessageFilter messageFilter) {
        return ValueFilter.newBuilder().setMessageFilter(messageFilter).build();
    }

    protected Value nullValue() {
        return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
    }

    @SafeVarargs
    private static <T> ValueFilter listValueFilter(Function<T, ValueFilter> valueConsumer, T... values) {
        var listValueFilter = ListValueFilter.newBuilder();
        for (T value : values) {
            listValueFilter.addValues(valueConsumer.apply(value));
        }
        return ValueFilter.newBuilder()
                .setListFilter(listValueFilter)
                .build();
    }
}
