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

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory
import com.exactpro.sf.common.messages.IMessage
import com.exactpro.th2.sailfish.utils.FromSailfishParameters
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal class IMessageToTransportConverterTest {
    @Test
    fun convertsEmptyCollectionInField() {
        val message = createMessage("Test")
        message.addField("emptyCol", emptyList<Any>())
        val converter = IMessageToTransportConverter()
        val convertedMessage = converter.toTransport(message).build()
        assertEquals(
            "Test",
            convertedMessage.type
        ) { "Converted message: $convertedMessage" }
        val fieldsMap = convertedMessage.body
        assertEquals(1, fieldsMap.size, "Unexpected fields count: $fieldsMap")
        val emptyColValue = fieldsMap["emptyCol"]
        Assertions.assertNotNull(emptyColValue) { "Field doesn't have 'emptyCol' field: $fieldsMap" }
        Assertions.assertTrue(emptyColValue is List<*>) { "Unexpected kind case: $emptyColValue" }
        Assertions.assertTrue((emptyColValue as List<*>).isEmpty()) { "List is not empty: $emptyColValue" }
    }

    @Test
    fun stripsTrailingZeroes() {
        val message = createMessage("test")
        message.addField("bd", BigDecimal("0.0000000"))
        message.addField("bdCollection", listOf(BigDecimal("0.00000000")))
        val transportMessage =
            IMessageToTransportConverter(FromSailfishParameters(stripTrailingZeros = true))
                .toTransport(message).build()
        Assertions.assertAll(
            Executable {
                val bd = transportMessage.body["bd"]
                Assertions.assertNotNull(bd) { "Missing field in $transportMessage" }
                assertEquals("0", bd) { "Unexpected value: $bd" }
            },
            Executable {
                val bd = transportMessage.body["bdCollection"]
                Assertions.assertNotNull(bd) { "Missing field in $transportMessage" }
                assertEquals(listOf("0"), bd) { "Unexpected value: $bd" }
            }
        )
    }

    @Test
    fun convertsBigDecimalInPlainFormat() {
        val message = createMessage("test")
        message.addField("bd", BigDecimal("0.0000000"))
        message.addField("bdCollection", listOf(BigDecimal("0.00000000")))
        val protoMessage = IMessageToTransportConverter()
            .toTransport(message).build()
        Assertions.assertAll(
            Executable {
                val bd = protoMessage.body["bd"]
                Assertions.assertNotNull(bd) { "Missing field in $protoMessage" }
                assertEquals("0.0000000", bd) { "Unexpected value: $bd" }
            },
            Executable {
                val bd = protoMessage.body["bdCollection"]
                Assertions.assertNotNull(bd) { "Missing field in $protoMessage" }
                assertEquals(listOf("0.00000000"), bd) { "Unexpected value: $bd" }
            }
        )
    }

    @ParameterizedTest
    @MethodSource("times")
    fun keepsMillisecondsForTimeAndDateTime(timePart: LocalTime?, expectedResult: String) {
        val message = createMessage("test")
        message.addField("time", timePart)
        message.addField("dateTime", LocalDateTime.of(LocalDate.EPOCH, timePart))
        val protoMessage = IMessageToTransportConverter()
            .toTransport(message).build()
        Assertions.assertAll(
            Executable {
                val time = protoMessage.body["time"]
                Assertions.assertNotNull(time) { "Missing field in $protoMessage" }
                assertEquals(expectedResult, time) { "Unexpected value: $time" }
            },
            Executable {
                val dateTime = protoMessage.body["dateTime"]
                Assertions.assertNotNull(dateTime) { "Missing field in $protoMessage" }
                assertEquals(
                    "1970-01-01T$expectedResult",
                    dateTime
                ) { "Unexpected value: $dateTime" }
            }
        )
    }

    companion object {
        @JvmStatic
        fun times(): List<Arguments> {
            return java.util.List.of(
                Arguments.arguments(LocalTime.of(0, 0), "00:00:00.000"),
                Arguments.arguments(LocalTime.of(12, 0), "12:00:00.000"),
                Arguments.arguments(LocalTime.of(12, 42), "12:42:00.000"),
                Arguments.arguments(LocalTime.of(12, 42, 1), "12:42:01.000"),
                Arguments.arguments(LocalTime.of(12, 42, 1, 1000000), "12:42:01.001"),
                Arguments.arguments(LocalTime.of(12, 42, 1, 1000), "12:42:01.000001"),
                Arguments.arguments(LocalTime.of(12, 42, 1, 1), "12:42:01.000000001")
            )
        }

        private fun createMessage(name: String): IMessage {
            return DefaultMessageFactory.getFactory().createMessage(name, "test")
        }
    }
}