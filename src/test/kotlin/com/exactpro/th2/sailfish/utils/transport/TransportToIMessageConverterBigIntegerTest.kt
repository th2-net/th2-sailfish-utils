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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType
import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.common.messages.structures.StructureType
import com.exactpro.sf.common.messages.structures.impl.DictionaryStructure
import com.exactpro.sf.common.messages.structures.impl.FieldStructure
import com.exactpro.sf.common.messages.structures.impl.MessageStructure
import com.exactpro.th2.common.grpc.FilterOperation
import com.exactpro.th2.common.grpc.MessageFilter
import com.exactpro.th2.common.grpc.ValueFilter
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.sailfish.utils.ProtoToIMessageConverter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class TransportToIMessageConverterBigIntegerTest : AbstractTransportToIMessageConverterTest() {

    @Test
    fun `converts big integer without dictionary`() {
        val converter = TransportToIMessageConverter()

        val message = ParsedMessage.builder()
            .addField("bigInt", "18446744073705051616".toBigInteger())
            .setId(MessageId.DEFAULT)
            .setType("test")
            .build()

        val wrapper: IMessage = converter.fromTransport(BOOK, SESSION_GROUP, message, false)
        Assertions.assertEquals(
            "18446744073705051616",
            wrapper.getField("bigInt"),
        ) {
            "unexpected value in message $wrapper"
        }
    }

    @Test
    fun `converts big integer with dictionary`() {
        val dictionary = DictionaryStructure(
            "test",
            "description",
            emptyMap(), // no attributes
            mapOf(
                "test" to MessageStructure(
                    "test",
                    "test",
                    "description",
                    mapOf(
                        "bigInt" to FieldStructure(
                            "bitInt",
                            "test",
                            JavaType.JAVA_MATH_BIG_DECIMAL,
                            false, // not a collection
                            StructureType.SIMPLE,
                        )
                    ),
                    emptyMap(), // no attributes
                    null, // no reference
                )
            ),
            emptyMap(), // no fields
        )
        val converter = TransportToIMessageConverter(dictionary = dictionary)

        val message = ParsedMessage.builder()
            .addField("bigInt", "18446744073705051616".toBigInteger())
            .setId(MessageId.DEFAULT)
            .setType("test")
            .build()

        val wrapper: IMessage = converter.fromTransport(BOOK, SESSION_GROUP, message, true)
        Assertions.assertEquals(
            "18446744073705051616".toBigDecimal(),
            wrapper.getField("bigInt"),
        ) {
            "unexpected value in message $wrapper"
        }
    }

    @TestFactory
    fun `compare filter handles big integer`(): List<DynamicTest> {
        val protoConverter = ProtoToIMessageConverter()
        val converter = TransportToIMessageConverter()

        return listOf(
            "18446744073705051617" to FilterOperation.LESS,
            "18446744073705051616" to FilterOperation.NOT_MORE,
            "18446744073705051615" to FilterOperation.MORE,
            "18446744073705051616" to FilterOperation.NOT_LESS,
            "18446744073705051616" to FilterOperation.EQ_DECIMAL_PRECISION,
        ).map { (filterValue, filterOp) ->
            DynamicTest.dynamicTest("$filterValue $filterOp") {
                val filter = protoConverter.fromProtoFilter(
                    MessageFilter.newBuilder()
                        .putFields(
                            "bigInt", ValueFilter.newBuilder()
                                .setOperation(filterOp)
                                .setSimpleFilter(filterValue)
                                .build()
                        ).build(),
                    "test",
                )
                val message = ParsedMessage.builder()
                    .addField("bigInt", "18446744073705051616".toBigInteger())
                    .setId(MessageId.DEFAULT)
                    .setType("test")
                    .build()

                val wrapper = converter.fromTransport(BOOK, SESSION_GROUP, message, false)

                assertPassed(filter, wrapper)
            }
        }
    }
}