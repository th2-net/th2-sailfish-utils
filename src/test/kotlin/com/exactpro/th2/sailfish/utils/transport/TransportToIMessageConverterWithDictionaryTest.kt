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
import com.exactpro.sf.common.messages.messageProperties
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.sailfish.utils.MessageConvertException
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.util.*

internal class TransportToIMessageConverterWithDictionaryTest : AbstractTransportToIMessageConverterTest() {

    private val converter = TransportToIMessageConverter(
        DefaultMessageFactoryProxy(), dictionary, dictionaryURI
    )

    @Test
    fun convertByDictionaryPositive() {
        val properties = mapOf("key" to "value")
        val transportMessage = createMessage().apply {
            metadata.putAll(properties)
        }
        val actualIMessage = converter.fromTransport(BOOK, SESSION_GROUP, transportMessage, true)
        val expectedIMessage = createExpectedIMessage()
        Assertions.assertAll(
            Executable { assertPassed(expectedIMessage, actualIMessage) },
            Executable {
                assertComplexFieldsHasCorrectNames(
                    actualIMessage, mapOf(
                        "complex" to "SubMessage",
                        "list" to "SubMessage",
                        "complexList" to "SubComplexList",
                    )
                )
            },
            Executable { Assertions.assertEquals(properties, actualIMessage.metaData.messageProperties) }
        )
    }

    @Test
    fun unknownEnumExceptionTest() {
        val transportMessage = createMessage().apply {
            body["enumInt"] = "UNKNOWN_ALIAS"
        }
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            { converter.fromTransport(BOOK, SESSION_GROUP, transportMessage, true) },
            "Conversion for message with missing enum value should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex.enumInt, cause: Unknown 'enumInt' enum value/alias for 'UNKNOWN_ALIAS' field in the 'dictionary' dictionary",
            exception.message
        )
    }

    @Test
    fun convertUnknownMessageThrowException() {
        val exception = Assertions.assertThrows(
            IllegalStateException::class.java,
            {
                converter.fromTransport(
                    BOOK,
                    SESSION_GROUP,
                    ParsedMessage.newMutable().apply { type = "SomeUnknownMessage" },
                    true
                )
            },
            "Conversion for unknown message should fails"
        )
        Assertions.assertEquals("Message 'SomeUnknownMessage' hasn't been found in dictionary", exception.message)
    }

    @Test
    fun convertMessageWithoutMessageTypeThrowException() {
        val argumentException = Assertions.assertThrows(
            IllegalArgumentException::class.java,
            { converter.fromTransport(BOOK, SESSION_GROUP, ParsedMessage(), true) },
            "Conversion for message without message type should fails"
        )
        Assertions.assertEquals("Cannot convert message with blank message type", argumentException.message)
    }

    @Test
    fun unknownFieldInRoot() {
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            {
                val message = ParsedMessage.newMutable().apply {
                    type = "RootWithNestedComplex"
                    body["Fake"] = "fake"
                }
                converter.fromTransport(BOOK, SESSION_GROUP, message, true)
            },
            "Conversion for message with unknown field in root should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex, cause: Field 'Fake' hasn't been found in message structure: RootWithNestedComplex",
            exception.message
        )
    }

    @Test
    fun unknownFieldInSubMessage() {
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            {
                val message = ParsedMessage.newMutable().apply {
                    type = "RootWithNestedComplex"
                    body["complex"] = mapOf("Fake" to "fake")
                }
                converter.fromTransport(BOOK, SESSION_GROUP, message, true)
            },
            "Conversion for message with unknown field in sub-message should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex.complex, cause: Field 'Fake' hasn't been found in message structure: complex",
            exception.message
        )
    }

    @Test
    fun unknownFieldInMessageCollection() {
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            {
                val message = ParsedMessage.newMutable().apply {
                    type = "RootWithNestedComplex"
                    body["msgCollection"] = listOf(
                        mapOf("field1" to "field1"),
                        mapOf("Fake" to "fake"),
                    )
                }
                converter.fromTransport(BOOK, SESSION_GROUP, message, true)
            },
            "Conversion for message with unknown field in message collection should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex.msgCollection.[1], cause: Field 'Fake' hasn't been found in message structure: msgCollection",
            exception.message
        )
    }

    @Test
    fun unknownFieldInSimpleCollection() {
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            {
                val message = ParsedMessage.newMutable().apply {
                    type = "RootWithNestedComplex"
                    body["simpleCollection"] = listOf("1", "abc")
                }
                converter.fromTransport(BOOK, SESSION_GROUP, message, true)
            },
            "Conversion for message with unknown field in simple collection should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex.simpleCollection.[1], cause: Cannot convert from String to Integer - value: abc, reason: For input string: \"abc\"",
            exception.message
        )
    }

    @Test
    fun incorrectSimpleType() {
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            {
                val message = ParsedMessage.newMutable().apply {
                    type = "RootWithNestedComplex"
                    body["string"] = mapOf<String, Any>()
                }
                converter.fromTransport(BOOK, SESSION_GROUP, message, true)
            },
            "Conversion for message with incorrect type of field with simple value should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex.string, cause: Expected 'class java.lang.String' value but got 'class kotlin.collections.EmptyMap' for field 'string'",
            exception.message
        )
    }

    @Test
    fun incorrectComplexType() {
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            {
                val message = ParsedMessage.newMutable().apply {
                    type = "RootWithNestedComplex"
                    body["complex"] = "fake"
                }
                converter.fromTransport(BOOK, SESSION_GROUP, message, true)
            },
            "Conversion for message with incorrect type of field with complex value should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex.complex, cause: Expected 'interface java.util.Map' value but got 'class java.lang.String' for field 'complex'",
            exception.message
        )
    }

    @Test
    fun incorrectListOfComplexType() {
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            {
                val message = ParsedMessage.newMutable().apply {
                    type = "RootWithNestedComplex"
                    body["msgCollection"] = "fake"
                }
                converter.fromTransport(BOOK, SESSION_GROUP, message, true)
            },
            "Conversion for message with incorrect type of field with list complex value should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex.msgCollection, cause: Expected 'interface java.util.List' value but got 'class java.lang.String' for field 'msgCollection'",
            exception.message
        )
    }

    @Test
    fun incorrectTypeOfList() {
        val exception = Assertions.assertThrows(
            MessageConvertException::class.java,
            {
                val message = ParsedMessage.newMutable().apply {
                    type = "RootWithNestedComplex"
                    body["msgCollection"] = listOf("fake")
                }
                converter.fromTransport(BOOK, SESSION_GROUP, message, true)
            },
            "Conversion for message with incorrect type of field with list complex value should fails"
        )
        Assertions.assertEquals(
            "Message path: RootWithNestedComplex.msgCollection.[0], cause: Expected 'interface java.util.Map' value but got 'class java.lang.String' for field 'msgCollection'",
            exception.message
        )
    }

    private fun assertMessageName(fieldNameToMessageName: Map<String, String>, fieldName: String, msg: IMessage) {
        val expectedMessageName = Objects.requireNonNull(
            fieldNameToMessageName[fieldName]
        ) { "Field $fieldName is complex but is not specified to check" }
        Assertions.assertEquals(expectedMessageName, msg.name)
    }

    private fun assertComplexFieldsHasCorrectNames(message: IMessage, fieldNameToMessageName: Map<String, String>) {
        for (fieldName in message.fieldNames) {
            val field = message.getField<Any>(fieldName)
            if (field is IMessage) {
                assertMessageName(fieldNameToMessageName, fieldName, field)
                assertComplexFieldsHasCorrectNames(field, fieldNameToMessageName)
                continue
            }
            if (field is Collection<*>) {
                if (field.isEmpty()) {
                    continue
                }
                field.iterator().next()!! as? IMessage ?: continue
                for (obj in field) {
                    val msg = obj as IMessage
                    assertMessageName(fieldNameToMessageName, fieldName, msg)
                    assertComplexFieldsHasCorrectNames(msg, fieldNameToMessageName)
                }
            }
        }
    }
}