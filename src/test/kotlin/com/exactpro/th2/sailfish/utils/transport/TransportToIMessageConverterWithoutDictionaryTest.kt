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

import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TransportToIMessageConverterWithoutDictionaryTest : AbstractTransportToIMessageConverterTest() {
    private val messageFactory = DefaultMessageFactoryProxy()
    private val converter = TransportToIMessageConverter(
        DefaultMessageFactoryProxy(), null, dictionaryURI
    )

    @Test
    fun convertsMessage() {
        val message = ParsedMessage.newSoftMutable().apply {
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
            type = "SomeMessage"
            with(body) {
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
        }

        val simple = messageFactory.createMessage(dictionaryURI, "Simple")
        simple.addField("Field", "A")
        val simple0 = simple.cloneMessage()
        simple0.addField("Index", "0")
        val simple1 = simple.cloneMessage()
        simple1.addField("Index", "1")
        val actualInnerMessage = messageFactory.createMessage(dictionaryURI, "InnerMessage")
        actualInnerMessage.addField("Simple", "hello")
        actualInnerMessage.addField("SimpleList", listOf("1", "2"))
        actualInnerMessage.addField("ComplexField", simple)
        actualInnerMessage.addField("ComplexList", listOf(simple0, simple1))
        val actualInner0 = actualInnerMessage.cloneMessage()
        actualInner0.addField("Index", "0")
        val actualInner1 = actualInnerMessage.cloneMessage()
        actualInner1.addField("Index", "1")
        val expected = messageFactory.createMessage(dictionaryURI, "SomeMessage")
        expected.addField("Simple", "hello")
        expected.addField("SimpleList", listOf("1", "2"))
        expected.addField("ComplexField", actualInnerMessage)
        expected.addField("ComplexList", listOf(actualInner0, actualInner1))
        val result = converter.fromTransport(BOOK, SESSION_GROUP, message, false)
        assertPassed(expected, result)
    }

    @Test
    fun conversionByDictionaryThrowException() {
        val illegalStateException = Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { converter.fromTransport(BOOK, SESSION_GROUP, ParsedMessage.newSoftMutable().apply { type = "Test" }, true) }
        Assertions.assertEquals("Cannot convert using dictionary without dictionary set", illegalStateException.message)
    }
}