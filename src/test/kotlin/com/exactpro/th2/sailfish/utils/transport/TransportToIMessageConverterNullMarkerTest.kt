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
package com.exactpro.th2.sailfish.utils.transport

import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader
import com.exactpro.sf.comparison.ComparatorSettings
import com.exactpro.sf.comparison.ComparisonUtil
import com.exactpro.sf.comparison.MessageComparator
import com.exactpro.sf.configuration.suri.SailfishURI
import com.exactpro.sf.scriptrunner.StatusType
import com.exactpro.th2.common.grpc.FilterOperation
import com.exactpro.th2.common.grpc.ListValueFilter
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageFilter
import com.exactpro.th2.common.grpc.NullValue
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.grpc.ValueFilter
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.common.value.toValue
import com.exactpro.th2.common.value.toValueFilter
import com.exactpro.th2.sailfish.utils.MessageWrapper
import com.exactpro.th2.sailfish.utils.ProtoToIMessageConverter
import com.exactpro.th2.sailfish.utils.ToSailfishParameters
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy
import com.exactpro.th2.sailfish.utils.filter.util.FilterUtils
import com.exactpro.th2.sailfish.utils.proto.AbstractProtoToIMessageConverterTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.function.Supplier

internal class TransportToIMessageConverterNullMarkerTest : AbstractProtoToIMessageConverterTest() {
    private var dictionaryStructure: IDictionaryStructure =
        TransportToIMessageConverterNullMarkerTest::class.java.classLoader.getResourceAsStream("dictionary.xml")
            .use { input -> XmlDictionaryStructureLoader().load(input) }
    private val messageFactory = DefaultMessageFactoryProxy()
    private val dictionaryURI = SailfishURI.unsafeParse("test")

    @ParameterizedTest(name = "useDictionary = {0}")
    @ValueSource(booleans = [true, false])
    fun convertsNullToMarker(useDictionary: Boolean) {
        val converter = TransportToIMessageConverter(
            messageFactory,
            dictionaryStructure,
            dictionaryURI,
            ToSailfishParameters(useMarkerForNullsInMessage = true)
        )
        val converted: MessageWrapper = converter.fromTransport(
            "test-book",
            "test-session-group",
            ParsedMessage.builder().apply {
                setType("RootWithNestedComplex")
                bodyBuilder().apply {
                    put("nullField", null)
                    put("complex", hashMapOf("field1" to null))
                    put("simpleCollection", listOf(null))
                    put("msgCollection", listOf(hashMapOf("field1" to null)))
                }
            }.build(),
            useDictionary
        )
        val expected = messageFactory.createMessage(dictionaryURI, "RootWithNestedComplex")
        expected.addField("nullField", FilterUtils.NULL_VALUE)
        val inner = messageFactory.createMessage(dictionaryURI, "SubMessage")
        inner.addField("field1", FilterUtils.NULL_VALUE)
        expected.addField("complex", inner)
        expected.addField("simpleCollection", listOf(FilterUtils.NULL_VALUE))
        expected.addField("msgCollection", listOf(inner))
        assertPassed(expected, converted)
    }

    @ParameterizedTest
    @EnumSource(value = FilterOperation::class, names = ["EQUAL", "NOT_EQUAL"], mode = EnumSource.Mode.INCLUDE)
    fun testExpectedNullValue(op: FilterOperation) {
        val converter = ProtoToIMessageConverter(
            messageFactory,
            dictionaryStructure,
            dictionaryURI,
            ProtoToIMessageConverter.createParameters().setUseMarkerForNullsInMessage(true)
        )
        val value: Supplier<Value> =
            Supplier<Value> { if (op == FilterOperation.EQUAL) nullValue() else getSimpleValue("test") }
        val message: Message = createMessageBuilder("Test")
            .putFields("A", value.get())
            .putFields("B", getListValue(value.get()))
            .putFields("C", createMessageBuilder("inner").putFields("A", value.get()).build().toValue())
            .build()
        val filter: ValueFilter = ValueFilter.newBuilder().setNullValue(NullValue.NULL_VALUE).setOperation(op).build()
        val messageFilter: MessageFilter = MessageFilter.newBuilder()
            .putFields("A", filter)
            .putFields(
                "B",
                ValueFilter.newBuilder().setListFilter(ListValueFilter.newBuilder().addValues(filter)).build()
            )
            .putFields("C", MessageFilter.newBuilder().putFields("A", filter).toValueFilter())
            .build()
        val expected = converter.fromProtoFilter(messageFilter, "Test")
        val actual: IMessage = converter.fromProtoMessage(message, false)
        val result = MessageComparator.compare(actual, expected, ComparatorSettings())
        Assertions.assertEquals(
            StatusType.PASSED,
            ComparisonUtil.getStatusType(result)
        ) { "Unexpected result: $result" }
    }
}