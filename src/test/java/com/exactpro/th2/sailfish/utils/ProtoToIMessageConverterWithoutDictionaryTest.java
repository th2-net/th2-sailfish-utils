/*
 *  Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Message.Builder;
import com.exactpro.th2.common.grpc.MetadataFilter;
import com.exactpro.th2.common.grpc.MetadataFilter.SimpleFilter;
import com.exactpro.th2.common.grpc.SimpleList;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy;
import com.exactpro.th2.sailfish.utils.filter.IOperationFilter;

class ProtoToIMessageConverterWithoutDictionaryTest extends AbstractProtoToIMessageConverterTest {
    private final DefaultMessageFactoryProxy messageFactory = new DefaultMessageFactoryProxy();
    private final SailfishURI dictionaryURI = SailfishURI.unsafeParse("test");
    private final ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
            messageFactory, null, dictionaryURI
    );

    @Test
    void convertsMessage() {
        Builder simpleMessage = createMessageBuilder("Simple")
                .putFields("Field", getSimpleValue("A"))
                .putFields("NullField", nullValue());
        Builder innerMessage = createMessageBuilder("InnerMessage")
                .putFields("Simple", getSimpleValue("hello"))
                .putFields("SimpleList", getListValue(getSimpleValue("1"), getSimpleValue("2")))
                .putFields("ComplexField", Value.newBuilder().setMessageValue(simpleMessage.build()).build())
                .putFields("ComplexList", getListValue(
                        Value.newBuilder().setMessageValue(simpleMessage.putFields("Index", getSimpleValue("0")).build()).build(),
                        Value.newBuilder().setMessageValue(simpleMessage.putFields("Index", getSimpleValue("1")).build()).build()
                ))
                .putFields("NullValue", nullValue());

        Message message = createMessageBuilder("SomeMessage")
                .putFields("Simple", getSimpleValue("hello"))
                .putFields("SimpleList", getListValue(getSimpleValue("1"), getSimpleValue("2")))
                .putFields("ComplexField", Value.newBuilder().setMessageValue(innerMessage.build()).build())
                .putFields("ComplexList", getListValue(
                        Value.newBuilder().setMessageValue(innerMessage.putFields("Index", getSimpleValue("0")).build()).build(),
                        Value.newBuilder().setMessageValue(innerMessage.putFields("Index", getSimpleValue("1")).build()).build()
                )).build();

        IMessage simple = messageFactory.createMessage(dictionaryURI, "Simple");
        simple.addField("Field", "A");
        simple.addField("NullField", null);
        IMessage simple0 = simple.cloneMessage();
        simple0.addField("Index", "0");
        IMessage simple1 = simple.cloneMessage();
        simple1.addField("Index", "1");

        IMessage actualInnerMessage = messageFactory.createMessage(dictionaryURI, "InnerMessage");
        actualInnerMessage.addField("Simple", "hello");
        actualInnerMessage.addField("SimpleList", List.of("1", "2"));
        actualInnerMessage.addField("ComplexField", simple);
        actualInnerMessage.addField("ComplexList", List.of(simple0, simple1));
        actualInnerMessage.addField("NullValue", null);

        IMessage actualInner0 = actualInnerMessage.cloneMessage();
        actualInner0.addField("Index", "0");
        IMessage actualInner1 = actualInnerMessage.cloneMessage();
        actualInner1.addField("Index", "1");

        IMessage expected = messageFactory.createMessage(dictionaryURI, "SomeMessage");
        expected.addField("Simple", "hello");
        expected.addField("SimpleList", List.of("1", "2"));
        expected.addField("ComplexField", actualInnerMessage);
        expected.addField("ComplexList", List.of(actualInner0, actualInner1));

        MessageWrapper result = converter.fromProtoMessage(message, false);

        assertPassed(expected, result);
    }

    @Test
    void conversionByDictionaryThrowException() {
        var illegalStateException = assertThrows(
                IllegalStateException.class,
                () -> converter.fromProtoMessage(createMessageBuilder("Test").build(), true)
        );

        assertEquals("Cannot convert using dictionary without dictionary set", illegalStateException.getMessage());
    }

    @Test
    void convertsMetadataFilter() {
        MetadataFilter metadataFilter = MetadataFilter.newBuilder()
                .putPropertyFilters("prop1", SimpleFilter.newBuilder()
                        .setOperation(FilterOperation.EQUAL)
                        .setValue("test")
                        .build())
                .putPropertyFilters("prop2", SimpleFilter.newBuilder()
                        .setOperation(FilterOperation.EQUAL)
                        .setValue("test")
                        .build())
                .build();
        IMessage message = converter.fromMetadataFilter(metadataFilter, "Metadata");

        assertEquals("Metadata", message.getName());
        assertEquals(2, message.getFieldCount());
        assertTrue(Set.of("prop1", "prop2").containsAll(message.getFieldNames()), () -> "Unknown fields: " + message);
        Object prop1 = message.getField("prop1");
        assertTrue(prop1 instanceof IFilter, () -> "Unexpected type: " + prop1.getClass());
        Object prop2 = message.getField("prop2");
        assertTrue(prop2 instanceof IFilter, () -> "Unexpected type: " + prop2.getClass());
    }

    private static List<Arguments> inOperationFilter() {
        return List.of(
                Arguments.of("A", StatusType.PASSED, FilterOperation.IN),
                Arguments.of("D", StatusType.FAILED, FilterOperation.IN),
                Arguments.of("D", StatusType.PASSED, FilterOperation.NOT_IN),
                Arguments.of("A", StatusType.FAILED, FilterOperation.NOT_IN)
        );
    }

    @ParameterizedTest
    @MethodSource("inOperationFilter")
    void testListContainsValueFilter(String value, StatusType status, FilterOperation operation) {
        MetadataFilter metadataFilter = MetadataFilter.newBuilder()
                .putPropertyFilters("prop1", SimpleFilter.newBuilder()
                        .setOperation(operation)
                        .setSimpleList(SimpleList.newBuilder()
                                .addAllSimpleValues(List.of("A", "B", "C")))
                        .build())
                .build();
        Message actual = createMessageBuilder("Metadata")
                .putFields("prop1", getSimpleValue(value))
                .build();
        MessageWrapper actualIMessage = converter.fromProtoMessage(actual, false);
        IMessage message = converter.fromMetadataFilter(metadataFilter, "Metadata");

        ComparisonResult result = MessageComparator.compare(actualIMessage, message, new ComparatorSettings());
        assertEquals(status, result.getResult("prop1").getStatus());
        assertEquals("Metadata", message.getName());
        assertEquals(1, message.getFieldCount());
        assertTrue(Set.of("prop1").containsAll(message.getFieldNames()), () -> "Unknown fields: " + message);
        Object prop1 = message.getField("prop1");
        assertTrue(prop1 instanceof IOperationFilter, () -> "Unexpected type: " + prop1.getClass());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "abc"})
    void testConvertsMetadataFilterWithSimpleValueException(String value) {
        MetadataFilter metadataFilter = MetadataFilter.newBuilder()
                .putPropertyFilters("prop1", SimpleFilter.newBuilder()
                        .setOperation(FilterOperation.IN)
                        .setValue(value)
                        .build())
                .build();

        assertThrows(IllegalArgumentException.class, () -> converter.fromMetadataFilter(metadataFilter, "Metadata"));
    }

    @Test
    void testConvertsMetadataFilterWithListException() {
        MetadataFilter metadataFilter = MetadataFilter.newBuilder()
                .putPropertyFilters("prop1", SimpleFilter.newBuilder()
                        .setOperation(FilterOperation.EQUAL)
                        .setSimpleList(SimpleList.newBuilder()
                                .addAllSimpleValues(List.of("A", "B"))
                                .build())
                        .build())
                .build();

        assertThrows(IllegalArgumentException.class, () -> converter.fromMetadataFilter(metadataFilter, "Metadata"));
    }

}