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

package com.exactpro.th2;

import static com.exactpro.sf.comparison.ComparisonUtil.getResultCount;
import static com.exactpro.sf.comparison.MessageComparator.compare;
import static com.exactpro.sf.scriptrunner.StatusType.CONDITIONALLY_FAILED;
import static com.exactpro.sf.scriptrunner.StatusType.CONDITIONALLY_PASSED;
import static com.exactpro.sf.scriptrunner.StatusType.FAILED;
import static com.exactpro.sf.scriptrunner.StatusType.PASSED;
import static com.exactpro.th2.Messages.getSimpleFieldCountRecursive;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.th2.infra.grpc.FilterOperation;
import com.exactpro.th2.infra.grpc.ListValue;
import com.exactpro.th2.infra.grpc.Message;
import com.exactpro.th2.infra.grpc.MessageFilter;
import com.exactpro.th2.infra.grpc.MessageMetadata;
import com.exactpro.th2.infra.grpc.Value;
import com.exactpro.th2.infra.grpc.ValueFilter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class ProtoToIMessageConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtoToIMessageConverterTest.class);
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
        Message protoMessage = createMessage();
        MessageWrapper actualIMessage = converter.fromProtoMessage(protoMessage, true);
        MessageWrapper expectedIMessage = createExpectedIMessage();
        assertPassed(expectedIMessage, actualIMessage);
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

    private void assertPassed(IMessage expected, IMessage actual) {
        ComparisonResult comparisonResult = compare(actual, expected, new ComparatorSettings());
        LOGGER.debug("Message comparison result: {}", comparisonResult);
        Assertions.assertEquals(getSimpleFieldCountRecursive(expected),
                getResultCount(comparisonResult, PASSED));
        Assertions.assertEquals(0, getResultCount(comparisonResult, FAILED));
        Assertions.assertEquals(0, getResultCount(comparisonResult, CONDITIONALLY_FAILED));
        Assertions.assertEquals(0, getResultCount(comparisonResult, CONDITIONALLY_PASSED));
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

    private Message createMessage() {
        return Message.newBuilder()
                .setMetadata(MessageMetadata.newBuilder()
                        .setMessageType("RootWithNestedComplex")
                        .build())
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
                .putFields("complexList", Value.newBuilder().setMessageValue(
                        Message.newBuilder().putFields("list", getComplexList())
                    ).build())
                .build();
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

    private Value getComplex(String messageType, Map<String, String> values) {
        Message.Builder messageBuilder = Message.newBuilder();
        messageBuilder.setMetadata(MessageMetadata.newBuilder().setMessageType(messageType).build());
        for (Entry<String, String> entry : values.entrySet()) {
            messageBuilder.putFields(entry.getKey(), getSimpleValue(entry.getValue()));
        }
        return Value.newBuilder().setMessageValue(messageBuilder).build();
    }

    @NotNull
    private Value getSimpleValue(String value) {
        return Value.newBuilder().setSimpleValue(value).build();
    }
}