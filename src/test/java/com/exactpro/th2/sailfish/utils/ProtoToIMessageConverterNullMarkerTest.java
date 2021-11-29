/*
 *  Copyright 2021 Exactpro (Exactpro Systems Limited)
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

import static com.exactpro.th2.common.value.ValueUtils.toValue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy;
import com.exactpro.th2.sailfish.utils.filter.util.FilterUtils;

class ProtoToIMessageConverterNullMarkerTest extends AbstractProtoToIMessageConverterTest {
    private final DefaultMessageFactoryProxy messageFactory = new DefaultMessageFactoryProxy();
    private final SailfishURI dictionaryURI = SailfishURI.unsafeParse("test");
    private static IDictionaryStructure dictionaryStructure;

    @BeforeAll
    static void beforeAll() throws IOException {
        try (var input = ProtoToIMessageConverterNullMarkerTest.class.getClassLoader().getResourceAsStream("dictionary.xml")) {
            dictionaryStructure = new XmlDictionaryStructureLoader().load(input);
        }
    }

    @ParameterizedTest(name = "useDictionary = {0}")
    @ValueSource(booleans = {true, false})
    void convertsNullToMarker(boolean useDictionary) {
        ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
                messageFactory, dictionaryStructure, dictionaryURI, ProtoToIMessageConverter.createParameters().setUseMarkerForNullsInMessage(true)
        );

        MessageWrapper converted = converter.fromProtoMessage(createMessageBuilder("RootWithNestedComplex")
                        .putFields("nullField", nullValue())
                        .putFields("complex", toValue(createMessageBuilder("SubMessage").putFields("field1", nullValue()).build()))
                        .putFields("simpleCollection", toValue(List.of(nullValue())))
                        .putFields("msgCollection", toValue(List.of(createMessageBuilder("SubMessage").putFields("field1", nullValue()).build())))
                        .build(), useDictionary);

        IMessage expected = messageFactory.createMessage(dictionaryURI, "RootWithNestedComplex");
        expected.addField("nullField", FilterUtils.NULL_VALUE);

        IMessage inner = messageFactory.createMessage(dictionaryURI, "SubMessage");
        inner.addField("field1", FilterUtils.NULL_VALUE);
        expected.addField("complex", inner);
        expected.addField("simpleCollection", List.of(FilterUtils.NULL_VALUE));
        expected.addField("msgCollection", List.of(inner));
        assertPassed(expected, converted);
    }
}