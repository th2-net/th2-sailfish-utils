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
package com.exactpro.th2.sailfish.utils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.Value.KindCase;

class TestIMessageToProtoConverter extends AbstractConverterTest {

    @Test
    void convertsEmptyCollectionInField() {
        IMessage message = createMessage("Test");
        message.addField("emptyCol", Collections.emptyList());
        IMessageToProtoConverter converter = new IMessageToProtoConverter();
        Message convertedMessage = converter.toProtoMessage(message).build();
        assertEquals("Test", convertedMessage.getMetadata().getMessageType(), () -> "Converted message: " + convertedMessage);
        Map<String, Value> fieldsMap = convertedMessage.getFieldsMap();
        assertEquals(1, fieldsMap.size(), "Unexpected fields count: " + fieldsMap);

        Value emptyColValue = fieldsMap.get("emptyCol");
        assertNotNull(emptyColValue, () -> "Field doesn't have 'emptyCol' field: " + fieldsMap);
        assertEquals(KindCase.LIST_VALUE, emptyColValue.getKindCase(), () -> "Unexpected kind case: " + emptyColValue);

        ListValue listValue = emptyColValue.getListValue();
        assertNotNull(listValue, () -> "Null list value: " + emptyColValue);
        assertTrue(listValue.getValuesList().isEmpty(), () -> "List is not empty: " + listValue);
    }

    @Test
    void convertsNullFields() {
        List<Integer> collectionWithNull = new ArrayList<>();
        collectionWithNull.add(1);
        collectionWithNull.add(null);
        collectionWithNull.add(2);
        IMessage message = createMessage("Test");
        message.addField("emptyField", null);
        message.addField("collectionWithNull", collectionWithNull);
        IMessageToProtoConverter converter = new IMessageToProtoConverter();
        Message convertedMessage = converter.toProtoMessage(message).build();
        assertEquals("Test", convertedMessage.getMetadata().getMessageType(), () -> "Converted message: " + convertedMessage);
        Map<String, Value> fieldsMap = convertedMessage.getFieldsMap();
        assertEquals(2, fieldsMap.size(), "Unexpected fields count: " + fieldsMap);

        Value emptyColValue = fieldsMap.get("emptyField");
        assertNotNull(emptyColValue, () -> "Message doesn't have 'emptyField' field: " + fieldsMap);
        assertEquals(emptyColValue.getKindCase(), KindCase.NULL_VALUE);

        Value collectionWithNullValue = fieldsMap.get("collectionWithNull");
        assertNotNull(collectionWithNullValue, () -> "Message doesn't have 'collectionWithNull' field: " + fieldsMap);
        assertTrue(collectionWithNullValue.hasListValue());

        List<Value> values = collectionWithNullValue.getListValue().getValuesList();
        assertEquals(values.size(), 3, () -> "Unexpected list size in 'collectionWithNull' field");
        assertEquals("1",values.get(0).getSimpleValue());
        assertEquals(values.get(1).getKindCase(), KindCase.NULL_VALUE);
        assertEquals("2", values.get(2).getSimpleValue());
    }

    @Test
    void stripsTrailingZeroes() {
        IMessage message = createMessage("test");
        message.addField("bd", new BigDecimal("0.0000000"));
        message.addField("bdCollection", List.of(new BigDecimal("0.00000000")));
        Message protoMessage = new IMessageToProtoConverter(IMessageToProtoConverter.parametersBuilder().setStripTrailingZeros(true).build())
                .toProtoMessage(message).build();
        assertAll(
                () -> {
                    Value bd = protoMessage.getFieldsMap().get("bd");
                    assertNotNull(bd, () -> "Missing field in " + protoMessage);
                    assertEquals(getSimpleValue("0"), bd, () -> "Unexpected value: " + bd);
                },
                () -> {
                    Value bd = protoMessage.getFieldsMap().get("bdCollection");
                    assertNotNull(bd, () -> "Missing field in " + protoMessage);
                    assertEquals(getListValue(getSimpleValue("0")), bd, () -> "Unexpected value: " + bd);
                }
        );
    }

    @Test
    void convertsBigDecimalInPlainFormat() {
        IMessage message = createMessage("test");
        message.addField("bd", new BigDecimal("0.0000000"));
        message.addField("bdCollection", List.of(new BigDecimal("0.00000000")));
        Message protoMessage = new IMessageToProtoConverter()
                .toProtoMessage(message).build();

        assertAll(
                () -> {
                    Value bd = protoMessage.getFieldsMap().get("bd");
                    assertNotNull(bd, () -> "Missing field in " + protoMessage);
                    assertEquals("0.0000000", bd.getSimpleValue(), () -> "Unexpected value: " + bd);
                },
                () -> {
                    Value bd = protoMessage.getFieldsMap().get("bdCollection");
                    assertNotNull(bd, () -> "Missing field in " + protoMessage);
                    assertEquals(getListValue(getSimpleValue("0.00000000")), bd, () -> "Unexpected value: " + bd);
                }
        );
    }

    @ParameterizedTest
    @MethodSource("times")
    void keepsMillisecondsForTimeAndDateTime(LocalTime timePart, String expectedResult) {
        IMessage message = createMessage("test");
        message.addField("time", timePart);
        message.addField("dateTime", LocalDateTime.of(LocalDate.EPOCH, timePart));
        Message protoMessage = new IMessageToProtoConverter()
                .toProtoMessage(message).build();

        assertAll(
                () -> {
                    Value time = protoMessage.getFieldsMap().get("time");
                    assertNotNull(time, () -> "Missing field in " + protoMessage);
                    assertEquals(expectedResult, time.getSimpleValue(), () -> "Unexpected value: " + time);
                },
                () -> {
                    Value dateTime = protoMessage.getFieldsMap().get("dateTime");
                    assertNotNull(dateTime, () -> "Missing field in " + protoMessage);
                    assertEquals("1970-01-01T" + expectedResult, dateTime.getSimpleValue(), () -> "Unexpected value: " + dateTime);
                }
        );
    }

    static List<Arguments> times() {
        return List.of(
                arguments(LocalTime.of(0, 0), "00:00:00.000"),
                arguments(LocalTime.of(12, 0), "12:00:00.000"),
                arguments(LocalTime.of(12, 42), "12:42:00.000"),
                arguments(LocalTime.of(12, 42, 1), "12:42:01.000"),
                arguments(LocalTime.of(12, 42, 1, 1_000_000), "12:42:01.001"),
                arguments(LocalTime.of(12, 42, 1, 1_000), "12:42:01.000001"),
                arguments(LocalTime.of(12, 42, 1, 1), "12:42:01.000000001")
        );
    }

    private static IMessage createMessage(String name) {
        return DefaultMessageFactory.getFactory().createMessage(name, "test");
    }
}