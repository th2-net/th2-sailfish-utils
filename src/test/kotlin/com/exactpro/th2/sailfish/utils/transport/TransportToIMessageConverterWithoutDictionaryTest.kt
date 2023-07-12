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
import com.exactpro.sf.comparison.ComparatorSettings
import com.exactpro.sf.comparison.IComparisonFilter
import com.exactpro.sf.comparison.MessageComparator
import com.exactpro.sf.scriptrunner.StatusType
import com.exactpro.th2.common.grpc.FilterOperation
import com.exactpro.th2.common.grpc.MetadataFilter
import com.exactpro.th2.common.grpc.SimpleList
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.sailfish.utils.MessageWrapper
import com.exactpro.th2.sailfish.utils.ProtoToIMessageConverter
import com.exactpro.th2.sailfish.utils.filter.IOperationFilter
import com.exactpro.th2.sailfish.utils.transport.TransportToIMessageConverter.Companion.DEFAULT_MESSAGE_FACTORY
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TransportToIMessageConverterWithoutDictionaryTest : AbstractTransportToIMessageConverterTest() {
    private val transportConverter = TransportToIMessageConverter()
    private val protoConverter = ProtoToIMessageConverter()

    @Test
    fun convertsMessage() {
        val message = ParsedMessage.builder().apply {
            val simple = mapOf("Field" to "A")
            val innerMessage = mapOf(
                "Simple" to "hello",
                "SimpleList" to listOf("1", "2"),
                "ComplexField" to simple,
                "ComplexList" to listOf(
                    hashMapOf<String, Any>(
                        "Index" to "0"
                    ).also { it.putAll(simple) },
                    hashMapOf<String, Any>(
                        "Index" to "1"
                    ).also { it.putAll(simple) },
                ),
            )
            setType("SomeMessage")
            bodyBuilder().apply {
                put("Simple", "hello")
                put("SimpleList", listOf("1", "2"))
                put("ComplexField", innerMessage)
                put(
                    "ComplexList", listOf(
                        hashMapOf<String, Any>(
                            "Index" to "0"
                        ).also { it.putAll(innerMessage) },
                        hashMapOf<String, Any>(
                            "Index" to "1"
                        ).also { it.putAll(innerMessage) },
                    )
                )
            }
        }.build()

        val simple = DEFAULT_MESSAGE_FACTORY.createMessage("Simple", transportConverter.namespace)
        simple.addField("Field", "A")
        val simple0 = simple.cloneMessage()
        simple0.addField("Index", "0")
        val simple1 = simple.cloneMessage()
        simple1.addField("Index", "1")
        val actualInnerMessage = DEFAULT_MESSAGE_FACTORY.createMessage("InnerMessage", transportConverter.namespace)
        actualInnerMessage.addField("Simple", "hello")
        actualInnerMessage.addField("SimpleList", listOf("1", "2"))
        actualInnerMessage.addField("ComplexField", simple)
        actualInnerMessage.addField("ComplexList", listOf(simple0, simple1))
        val actualInner0 = actualInnerMessage.cloneMessage()
        actualInner0.addField("Index", "0")
        val actualInner1 = actualInnerMessage.cloneMessage()
        actualInner1.addField("Index", "1")
        val expected = DEFAULT_MESSAGE_FACTORY.createMessage("SomeMessage", transportConverter.namespace)
        expected.addField("Simple", "hello")
        expected.addField("SimpleList", listOf("1", "2"))
        expected.addField("ComplexField", actualInnerMessage)
        expected.addField("ComplexList", listOf(actualInner0, actualInner1))
        val result = transportConverter.fromTransport(BOOK, SESSION_GROUP, message, false)
        assertPassed(expected, result)
    }

    @Test
    fun conversionByDictionaryThrowException() {
        val illegalStateException = Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            transportConverter.fromTransport(
                BOOK,
                SESSION_GROUP,
                ParsedMessage(type = "Test"),
                true
            )
        }
        Assertions.assertEquals("Cannot convert using dictionary without dictionary set", illegalStateException.message)
    }

    @Test
    fun convertsMetadataFilter() {
        val metadataFilter = MetadataFilter.newBuilder()
            .putPropertyFilters(
                "prop1", MetadataFilter.SimpleFilter.newBuilder()
                    .setOperation(FilterOperation.EQUAL)
                    .setValue("test")
                    .build()
            )
            .putPropertyFilters(
                "prop2", MetadataFilter.SimpleFilter.newBuilder()
                    .setOperation(FilterOperation.EQUAL)
                    .setValue("test")
                    .build()
            )
            .build()
        val message: IMessage = protoConverter.fromMetadataFilter(metadataFilter, "Metadata")
        Assertions.assertEquals("Metadata", message.name)
        Assertions.assertEquals(2, message.fieldCount)
        Assertions.assertTrue(
            setOf("prop1", "prop2").containsAll(message.fieldNames)
        ) { "Unknown fields: $message" }
        val prop1 = message.getField<Any>("prop1")
        Assertions.assertTrue(prop1 is IComparisonFilter) { "Unexpected type: " + prop1.javaClass }
        val prop2 = message.getField<Any>("prop2")
        Assertions.assertTrue(prop2 is IComparisonFilter) { "Unexpected type: " + prop2.javaClass }
    }

    private fun inOperationFilter(): List<Arguments> {
        return listOf(
            Arguments.of("A", StatusType.PASSED, FilterOperation.IN),
            Arguments.of("D", StatusType.FAILED, FilterOperation.IN),
            Arguments.of("D", StatusType.PASSED, FilterOperation.NOT_IN),
            Arguments.of("A", StatusType.FAILED, FilterOperation.NOT_IN)
        )
    }

    @ParameterizedTest
    @MethodSource("inOperationFilter")
    fun testListContainsValueFilter(value: String?, status: StatusType?, operation: FilterOperation?) {
        val metadataFilter = MetadataFilter.newBuilder()
            .putPropertyFilters(
                "prop1", MetadataFilter.SimpleFilter.newBuilder()
                    .setOperation(operation)
                    .setSimpleList(
                        SimpleList.newBuilder()
                            .addAllSimpleValues(listOf("A", "B", "C"))
                    )
                    .build()
            )
            .build()
        val actual = ParsedMessage.builder().apply {
            setType("Metadata")
            bodyBuilder().apply {
                put("prop1", value)
            }
        }.build()
        val actualIMessage: MessageWrapper =
            transportConverter.fromTransport("test-book", "test-session-group", actual, false)
        val message: IMessage = protoConverter.fromMetadataFilter(metadataFilter, "Metadata")
        val result = MessageComparator.compare(actualIMessage, message, ComparatorSettings())
        Assertions.assertEquals(status, result.getResult("prop1").status)
        Assertions.assertEquals("Metadata", message.name)
        Assertions.assertEquals(1, message.fieldCount)
        Assertions.assertTrue(
            setOf("prop1").containsAll(message.fieldNames)
        ) { "Unknown fields: $message" }
        val prop1 = message.getField<Any>("prop1")
        Assertions.assertTrue(prop1 is IOperationFilter) { "Unexpected type: " + prop1.javaClass }
    }
}