/*
 * Copyright 2021-2023 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.sailfish.utils.proto;

import static com.exactpro.th2.common.value.ValueFilterUtilsKt.toValueFilter;
import static com.exactpro.th2.common.value.ValueUtils.toValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import com.exactpro.th2.sailfish.utils.MessageWrapper;
import com.exactpro.th2.sailfish.utils.ProtoToIMessageConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.ValueSource;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.ListValueFilter;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.NullValue;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.ValueFilter;
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


    @ParameterizedTest
    @EnumSource(value = FilterOperation.class, names = {"EQUAL", "NOT_EQUAL"}, mode = Mode.INCLUDE)
    void testExpectedNullValue(FilterOperation op) {
        ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
                messageFactory, dictionaryStructure, dictionaryURI, ProtoToIMessageConverter.createParameters().setUseMarkerForNullsInMessage(true)
        );
        Supplier<Value> value = () -> op == FilterOperation.EQUAL ? nullValue() : getSimpleValue("test");
        Message message = createMessageBuilder("Test")
                .putFields("A", value.get())
                .putFields("B", getListValue(value.get()))
                .putFields("C", toValue(createMessageBuilder("inner").putFields("A", value.get()).build()))
                .build();
        ValueFilter filter = ValueFilter.newBuilder().setNullValue(NullValue.NULL_VALUE).setOperation(op).build();
        MessageFilter messageFilter = MessageFilter.newBuilder()
                .putFields("A", filter)
                .putFields("B", ValueFilter.newBuilder().setListFilter(ListValueFilter.newBuilder().addValues(filter)).build())
                .putFields("C", toValueFilter(MessageFilter.newBuilder().putFields("A", filter)))
                .build();

        IMessage expected = converter.fromProtoFilter(messageFilter, "Test");
        IMessage actual = converter.fromProtoMessage(message, false);

        ComparisonResult result = MessageComparator.compare(actual, expected, new ComparatorSettings());
        assertEquals(StatusType.PASSED, ComparisonUtil.getStatusType(result), () -> "Unexpected result: " + result);
    }

    @ParameterizedTest
    @EnumSource(value = FilterOperation.class, names = {"EQUAL", "NOT_EQUAL"}, mode = Mode.INCLUDE)
    void testNullValueFilterFailsIfFieldIsMissing(FilterOperation op) {
        ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
                messageFactory, dictionaryStructure, dictionaryURI, ProtoToIMessageConverter.createParameters().setUseMarkerForNullsInMessage(true)
        );
        Message message = createMessageBuilder("Test")
                .build();
        ValueFilter filter = ValueFilter.newBuilder().setNullValue(NullValue.NULL_VALUE).setOperation(op).build();
        MessageFilter messageFilter = MessageFilter.newBuilder()
                .putFields("A", filter)
                .build();

        IMessage expected = converter.fromProtoFilter(messageFilter, "Test");
        IMessage actual = converter.fromProtoMessage(message, false);

        ComparisonResult result = MessageComparator.compare(actual, expected, new ComparatorSettings());
        assertEquals(StatusType.FAILED, ComparisonUtil.getStatusType(result), () -> "Unexpected result: " + result);
    }
}