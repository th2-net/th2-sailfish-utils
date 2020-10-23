/*
 *  Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.th2.sailfish.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.NullValue;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.ValueFilter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static org.junit.jupiter.api.Assertions.*;

class ProtoToIMessageConverterWithDictionaryTest extends AbstractProtoToIMessageConverterTest {
    private static SailfishURI dictionaryURI;
    private static ProtoToIMessageConverter converter;

    @BeforeAll
    static void beforeAll() {
        try {
            IDictionaryStructure dictionary = new XmlDictionaryStructureLoader().load(
                    Files.newInputStream(Path.of("src", "test", "resources", "dictionary.xml")));
            dictionaryURI = SailfishURI.unsafeParse(dictionary.getNamespace());
            converter = new ProtoToIMessageConverter(
                    new DefaultMessageFactoryProxy(), dictionary, dictionaryURI);
        } catch (IOException e) {
            throw new RuntimeException("could not create converter", e);
        }
    }

    @Test
    void convertByDictionaryPositive() {
        Message protoMessage = createMessage().build();
        MessageWrapper actualIMessage = converter.fromProtoMessage(protoMessage, true);
        MessageWrapper expectedIMessage = createExpectedIMessage();
        assertPassed(expectedIMessage, actualIMessage);
    }

    @Test
    void UnknownEnumExceptionTest() {
        Message protoMessage = createMessage()
                .putFields("enumInt", getSimpleValue("UNKNOWN_ALIAS")).build();
        var missingEnumValueException = assertThrows(
                UnknownEnumException.class,
                () -> converter.fromProtoMessage(protoMessage, true),
                "Conversion for message with missing enum value should fails");
        assertEquals("Unknown 'enumInt' enum value/alias for 'UNKNOWN_ALIAS' field in the 'dictionary' dictionary",
                missingEnumValueException.getMessage());
    }

    @Test
    void convertUnknownMessageThrowException() {
        var nullPointerException = assertThrows(
                NullPointerException.class,
                () -> converter.fromProtoMessage(createMessageBuilder("SomeUnknownMessage").build(), true),
                "Conversion for unknown message should fails");
        assertEquals("Unknown message: SomeUnknownMessage", nullPointerException.getMessage());
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

    private MessageWrapper createExpectedIMessage() {
        IMessage message = new DefaultMessageFactoryProxy().createMessage(dictionaryURI, "RootWithNestedComplex");
        message.addField("string", "StringValue");
        message.addField("byte", (byte)0);
        message.addField("short", (short)1);
        message.addField("int", 2);
        message.addField("long", (long)3);
        message.addField("float", 1.1f);
        message.addField("double", 2.2);
        message.addField("decimal", new BigDecimal("3.3"));
        message.addField("char", 'A');
        message.addField("bool", true);
        message.addField("boolY", true);
        message.addField("boolN", false);
        message.addField("enumInt", -1);
        IMessage nestedComplex = new DefaultMessageFactoryProxy().createMessage(dictionaryURI, "SubMessage");
        nestedComplex.addField("field1", "field1");
        nestedComplex.addField("field2", "field2");
        IMessage nestedComplexSecond = new DefaultMessageFactoryProxy().createMessage(dictionaryURI, "SubMessage");
        nestedComplexSecond.addField("field1", "field3");
        nestedComplexSecond.addField("field2", "field4");
        message.addField("complex", nestedComplex);
        IMessage nestedComplexList = new DefaultMessageFactoryProxy().createMessage(dictionaryURI, "SubComplexList");
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
                .putFields("complex", getComplex("SubMessage", ImmutableMap.of(
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
                .addValues(0, getComplex("SubMessage", ImmutableMap.of(
                        "field1", "field1",
                        "field2", "field2"
                )))
                .addValues(1, getComplex("SubMessage", ImmutableMap.of(
                        "field1", "field3",
                        "field2", "field4"
                )))
                .build()
        ).build();
    }

}