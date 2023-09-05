/*
 * Copyright 2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MetadataExtensions;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Message.Builder;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.NullValue;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.ValueFilter;
import com.exactpro.th2.sailfish.utils.proto.AbstractProtoToIMessageConverterTest;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static com.exactpro.th2.sailfish.utils.ProtoToIMessageConverter.DEFAULT_MESSAGE_FACTORY;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtoToIMessageConverterWithDictionaryTest extends AbstractProtoToIMessageConverterTest {
    private static ProtoToIMessageConverter converter;

    @BeforeAll
    static void beforeAll() {
        try {
            IDictionaryStructure dictionary = new XmlDictionaryStructureLoader().load(
                    Files.newInputStream(Path.of("src", "test", "resources", "dictionary.xml")));
            converter = new ProtoToIMessageConverter(
                    DEFAULT_MESSAGE_FACTORY, dictionary);
        } catch (IOException e) {
            throw new RuntimeException("could not create converter", e);
        }
    }

    @Test
    void convertByDictionaryPositive() {
        Builder builder = createMessage();
        Map<String, String> properties = Map.of("key", "value");
        builder.getMetadataBuilder().putAllProperties(properties);
        Message protoMessage = builder
                .build();
        MessageWrapper actualIMessage = converter.fromProtoMessage(protoMessage, true);
        MessageWrapper expectedIMessage = createExpectedIMessage();
        assertAll(
                () -> assertPassed(expectedIMessage, actualIMessage),
                () -> assertComplexFieldsHasCorrectNames(actualIMessage, Map.ofEntries(
                        entry("complex", "SubMessage"),
                        entry("list", "SubMessage"),
                        entry("complexList", "SubComplexList")
                )),
                () -> assertEquals(properties, MetadataExtensions.getMessageProperties(actualIMessage.getMetaData()))
        );
    }

    @Test
    void UnknownEnumExceptionTest() {
        Message protoMessage = createMessage()
                .putFields("enumInt", getSimpleValue("UNKNOWN_ALIAS")).build();
        var exception = assertThrows(
                MessageConvertException.class,
                () -> converter.fromProtoMessage(protoMessage, true),
                "Conversion for message with missing enum value should fails");
        assertEquals("Message path: RootWithNestedComplex.enumInt, cause: Unknown 'enumInt' enum value/alias for 'UNKNOWN_ALIAS' field in the 'dictionary' dictionary", exception.getMessage());
    }

    @Test
    void convertUnknownMessageThrowException() {
        var exception = assertThrows(
                IllegalStateException.class,
                () -> converter.fromProtoMessage(createMessageBuilder("SomeUnknownMessage").build(), true),
                "Conversion for unknown message should fails");
        assertEquals("Message 'SomeUnknownMessage' hasn't been found in dictionary", exception.getMessage());
    }

    @Test
    void convertMessageWithoutMessageTypeThrowException() {
        var argumentException = assertThrows(
                IllegalArgumentException.class,
                () -> converter.fromProtoMessage(Message.newBuilder().build(), true),
                "Conversion for message without message type should fails");
        assertEquals("Cannot convert message with blank message type", argumentException.getMessage());
    }

    @Test
    void createSimpleFilterFromStringWithQuotes() {
        assertDoesNotThrow(() -> {
            MessageFilter filter = MessageFilter.newBuilder()
                    .putFields("field", ValueFilter.newBuilder()
                            .setOperation(FilterOperation.EQUAL)
                            .setSimpleFilter("\"badly quoted string`'")
                            .build())
                    .build();

            return converter.fromProtoFilter(filter, "message");
        });
    }

    @Test
    void unknownFieldInRoot() {
        var exception = assertThrows(
                MessageConvertException.class,
                () -> {
                    Message message = createMessageBuilder("RootWithNestedComplex")
                            .putFields("Fake", getSimpleValue("fake"))
                            .build();
                    converter.fromProtoMessage(message, true);
                },
                "Conversion for message with unknown field in root should fails");
        assertEquals("Message path: RootWithNestedComplex, cause: Field 'Fake' hasn't been found in message structure: RootWithNestedComplex", exception.getMessage());
    }

    @Test
    void unknownFieldInSubMessage() {
        var exception = assertThrows(
                MessageConvertException.class,
                () -> {
                    Message message = createMessageBuilder("RootWithNestedComplex")
                            .putFields("complex", getComplex("SubMessage", Map.of(
                                    "Fake", "fake"
                            )))
                            .build();
                    converter.fromProtoMessage(message, true);
                },
                "Conversion for message with unknown field in sub-message should fails");
        assertEquals("Message path: RootWithNestedComplex.complex, cause: Field 'Fake' hasn't been found in message structure: complex", exception.getMessage());
    }

    @Test
    void unknownFieldInMessageCollection() {
        var exception = assertThrows(
                MessageConvertException.class,
                () -> {
                    Value listValue = Value.newBuilder().setListValue(ListValue.newBuilder()
                                    .addValues(getComplex("SubMessage", Map.of("field1", "field1")))
                                    .addValues(getComplex("SubMessage", Map.of("Fake", "fake"))))
                            .build();
                    Message message = createMessageBuilder("RootWithNestedComplex")
                            .putFields("msgCollection", listValue)
                            .build();
                    converter.fromProtoMessage(message, true);
                },
                "Conversion for message with unknown field in message collection should fails");
        assertEquals("Message path: RootWithNestedComplex.msgCollection.[1], cause: Field 'Fake' hasn't been found in message structure: msgCollection", exception.getMessage());
    }

    @Test
    void unknownFieldInSimpleCollection() {
        var exception = assertThrows(
                MessageConvertException.class,
                () -> {
                    Value listValue = Value.newBuilder().setListValue(ListValue.newBuilder()
                                    .addValues(getSimpleValue("1"))
                                    .addValues(getSimpleValue("abc")))
                            .build();
                    Message message = createMessageBuilder("RootWithNestedComplex")
                            .putFields("simpleCollection", listValue)
                            .build();
                    converter.fromProtoMessage(message, true);
                },
                "Conversion for message with unknown field in simple collection should fails");
        assertEquals("Message path: RootWithNestedComplex.simpleCollection.[1], cause: Cannot convert from String to Integer - value: abc, reason: For input string: \"abc\"", exception.getMessage());
    }

    @Test
    void incorrectSimpleType() {
        var exception = assertThrows(
                MessageConvertException.class,
                () -> {
                    Message message = createMessageBuilder("RootWithNestedComplex")
                            .putFields("string", Value.newBuilder().setMessageValue(Message.newBuilder().build()).build())
                            .build();
                    converter.fromProtoMessage(message, true);
                },
                "Conversion for message with incorrect type of field with simple value should fails");
        assertEquals("Message path: RootWithNestedComplex.string, cause: Expected 'SIMPLE_VALUE' value but got 'MESSAGE_VALUE' for field 'string'", exception.getMessage());
    }

    @Test
    void incorrectComplexType() {
        var exception = assertThrows(
                MessageConvertException.class,
                () -> {
                    Message message = createMessageBuilder("RootWithNestedComplex")
                            .putFields("complex", getSimpleValue("fake"))
                            .build();
                    converter.fromProtoMessage(message, true);
                },
                "Conversion for message with incorrect type of field with complex value should fails");
        assertEquals("Message path: RootWithNestedComplex.complex, cause: Expected 'MESSAGE_VALUE' value but got 'SIMPLE_VALUE' for field 'complex'", exception.getMessage());
    }

    @Test
    void incorrectListOfComplexType() {
        var exception = assertThrows(
                MessageConvertException.class,
                () -> {
                    Message message = createMessageBuilder("RootWithNestedComplex")
                            .putFields("msgCollection", getSimpleValue("fake"))
                            .build();
                    converter.fromProtoMessage(message, true);
                },
                "Conversion for message with incorrect type of field with list complex value should fails");
        assertEquals("Message path: RootWithNestedComplex.msgCollection, cause: Expected 'LIST_VALUE' value but got 'SIMPLE_VALUE' for field 'msgCollection'", exception.getMessage());
    }

    @Test
    void incorrectTypeOfList() {
        var exception = assertThrows(
                MessageConvertException.class,
                () -> {
                    Message message = createMessageBuilder("RootWithNestedComplex")
                            .putFields("msgCollection", Value.newBuilder()
                                    .setListValue(ListValue.newBuilder()
                                            .addValues(getSimpleValue("fake"))
                                    )
                                    .build())
                            .build();
                    converter.fromProtoMessage(message, true);
                },
                "Conversion for message with incorrect type of field with list complex value should fails");
        assertEquals("Message path: RootWithNestedComplex.msgCollection.[0], cause: Expected 'MESSAGE_VALUE' value but got 'SIMPLE_VALUE' for field 'msgCollection'", exception.getMessage());
    }

    private void assertMessageName(Map<String, String> fieldNameToMessageName, String fieldName, IMessage msg) {
        String expectedMessageName = requireNonNull(fieldNameToMessageName.get(fieldName),
                () -> "Field " + fieldName + " is complex but is not specified to check");
        assertEquals(expectedMessageName, msg.getName());
    }

    private void assertComplexFieldsHasCorrectNames(IMessage message, Map<String, String> fieldNameToMessageName) {
        for (String fieldName : message.getFieldNames()) {
            Object field = message.getField(fieldName);
            if (field instanceof IMessage) {
                IMessage msg = (IMessage) field;
                assertMessageName(fieldNameToMessageName, fieldName, msg);
                assertComplexFieldsHasCorrectNames(msg, fieldNameToMessageName);
                continue;
            }
            if (field instanceof Collection<?>) {
                var collection = (Collection<?>) field;
                if (collection.isEmpty()) {
                    continue;
                }
                Object firstEl = collection.iterator().next();
                if (!(firstEl instanceof IMessage)) {
                    continue;
                }
                for (Object obj : collection) {
                    IMessage msg = (IMessage) obj;
                    assertMessageName(fieldNameToMessageName, fieldName, msg);
                    assertComplexFieldsHasCorrectNames(msg, fieldNameToMessageName);
                }
            }
        }
    }

    private MessageWrapper createExpectedIMessage() {
        IMessage message = DEFAULT_MESSAGE_FACTORY.createMessage("RootWithNestedComplex", converter.getNamespace());
        message.addField("string", "StringValue");
        message.addField("byte", (byte) 0);
        message.addField("short", (short) 1);
        message.addField("int", 2);
        message.addField("long", (long) 3);
        message.addField("float", 1.1f);
        message.addField("double", 2.2);
        message.addField("decimal", new BigDecimal("3.3"));
        message.addField("char", 'A');
        message.addField("bool", true);
        message.addField("boolY", true);
        message.addField("boolN", false);
        message.addField("enumInt", -1);
        IMessage nestedComplex = DEFAULT_MESSAGE_FACTORY.createMessage("SubMessage", converter.getNamespace());
        nestedComplex.addField("field1", "field1");
        nestedComplex.addField("field2", "field2");
        IMessage nestedComplexSecond = DEFAULT_MESSAGE_FACTORY.createMessage("SubMessage", converter.getNamespace());
        nestedComplexSecond.addField("field1", "field3");
        nestedComplexSecond.addField("field2", "field4");
        message.addField("complex", nestedComplex);
        IMessage nestedComplexList = DEFAULT_MESSAGE_FACTORY.createMessage("SubComplexList", converter.getNamespace());
        nestedComplexList.addField("list", ImmutableList.of(nestedComplex, nestedComplexSecond));
        message.addField("complexList", nestedComplexList);
        return new MessageWrapper(message);
    }

    private Message.Builder createMessage() {
        return createMessageBuilder("RootWithNestedComplex")
                .putFields("string", getSimpleValue("StringValue"))
                .putFields("byte", getSimpleValue("0"))
                .putFields("short", getSimpleValue("1"))
                .putFields("int", getSimpleValue("2"))
                .putFields("long", getSimpleValue("3"))
                .putFields("float", getSimpleValue("1.1"))
                .putFields("double", getSimpleValue("2.2"))
                .putFields("decimal", getSimpleValue("3.3"))
                .putFields("char", getSimpleValue("A"))
                .putFields("bool", getSimpleValue("true"))
                .putFields("boolY", getSimpleValue("Y"))
                .putFields("boolN", getSimpleValue("n"))
                .putFields("enumInt", getSimpleValue("MINUS_ONE"))
                .putFields("complex", getComplex("SubMessage", Map.of(
                        "field1", "field1",
                        "field2", "field2"
                )))
                .putFields("nullField", Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
                .putFields("complexList", Value.newBuilder().setMessageValue(
                        Message.newBuilder().putFields("list", getComplexList())
                ).build());
    }

    private Value getComplexList() {
        return Value.newBuilder().setListValue(ListValue.newBuilder()
                .addValues(0, getComplex("SubMessage", Map.of(
                        "field1", "field1",
                        "field2", "field2"
                )))
                .addValues(1, getComplex("SubMessage", Map.of(
                        "field1", "field3",
                        "field2", "field4"
                )))
                .build()
        ).build();
    }

}