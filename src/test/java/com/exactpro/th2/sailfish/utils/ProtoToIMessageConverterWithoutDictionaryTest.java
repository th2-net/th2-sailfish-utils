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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.th2.infra.grpc.Message;
import com.exactpro.th2.infra.grpc.Message.Builder;
import com.exactpro.th2.infra.grpc.Value;

class ProtoToIMessageConverterWithoutDictionaryTest extends AbstractProtoToIMessageConverterTest {
    private final DefaultMessageFactoryProxy messageFactory = new DefaultMessageFactoryProxy();
    private final SailfishURI dictionaryURI = SailfishURI.unsafeParse("test");
    private final ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
            messageFactory, null, dictionaryURI
    );

    @Test
    void convertsMessage() {
        Builder simpleMessage = createMessageBuilder("Simple")
                .putFields("Field", getSimpleValue("A"));
        Builder innerMessage = createMessageBuilder("InnerMessage")
                .putFields("Simple", getSimpleValue("hello"))
                .putFields("SimpleList", getListValue(getSimpleValue("1"), getSimpleValue("2")))
                .putFields("ComplexField", Value.newBuilder().setMessageValue(simpleMessage.build()).build())
                .putFields("ComplexList", getListValue(
                        Value.newBuilder().setMessageValue(simpleMessage.putFields("Index", getSimpleValue("0")).build()).build(),
                        Value.newBuilder().setMessageValue(simpleMessage.putFields("Index", getSimpleValue("1")).build()).build()
                ));

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
        IMessage simple0 = simple.cloneMessage();
        simple0.addField("Index", "0");
        IMessage simple1 = simple.cloneMessage();
        simple1.addField("Index", "1");

        IMessage actualInnerMessage = messageFactory.createMessage(dictionaryURI, "InnerMessage");
        actualInnerMessage.addField("Simple", "hello");
        actualInnerMessage.addField("SimpleList", List.of("1", "2"));
        actualInnerMessage.addField("ComplexField", simple);
        actualInnerMessage.addField("ComplexList", List.of(simple0, simple1));

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
}