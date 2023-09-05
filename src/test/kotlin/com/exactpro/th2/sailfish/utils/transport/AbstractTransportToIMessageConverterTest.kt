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
import com.exactpro.sf.scriptrunner.StatusType
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.sailfish.utils.MessageWrapper
import com.exactpro.th2.sailfish.utils.transport.TransportToIMessageConverter.Companion.DEFAULT_MESSAGE_FACTORY
import com.google.common.collect.ImmutableList
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path

open class AbstractTransportToIMessageConverterTest {
    protected val dictionary: IDictionaryStructure = XmlDictionaryStructureLoader().load(
        Files.newInputStream(Path.of("src", "test", "resources", "dictionary.xml"))
    )

    protected fun assertPassed(expected: IMessage, actual: IMessage) {
        val comparisonResult = MessageComparator.compare(actual, expected, ComparatorSettings())
        K_LOGGER.debug { "Message comparison result: $comparisonResult, expected: ${expected.name}, actual: ${actual.name}" }
        Assertions.assertEquals(
            getSimpleFieldCountRecursive(expected),
            ComparisonUtil.getResultCount(comparisonResult, StatusType.PASSED)
        )
        Assertions.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED))
        Assertions.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.CONDITIONALLY_FAILED))
        Assertions.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.CONDITIONALLY_PASSED))
    }

    protected fun createMessage(): ParsedMessage.FromMapBuilder = ParsedMessage.builder().apply {
        setType("RootWithNestedComplex")
        setBody(
            hashMapOf(
                "string" to "StringValue",
                "byte" to "0",
                "short" to "1",
                "int" to "2",
                "long" to "3",
                "float" to "1.1",
                "double" to "2.2",
                "decimal" to "3.3",
                "char" to "A",
                "bool" to "true",
                "boolY" to "Y",
                "boolN" to "n",
                "enumInt" to "5",
                "enumInt" to "MINUS_ONE",
                "complex" to hashMapOf("field1" to "field1", "field2" to "field2"),
                "nullField" to null,
                "complexList" to hashMapOf(
                    "list" to listOf(
                        hashMapOf("field1" to "field1", "field2" to "field2"),
                        hashMapOf("field1" to "field3", "field2" to "field4")
                    )
                )
            )
        )
    }

    protected fun createExpectedIMessage(): MessageWrapper {
        val message = DEFAULT_MESSAGE_FACTORY.createMessage("RootWithNestedComplex", dictionary.namespace)
        message.addField("string", "StringValue")
        message.addField("byte", 0.toByte())
        message.addField("short", 1.toShort())
        message.addField("int", 2)
        message.addField("long", 3L)
        message.addField("float", 1.1f)
        message.addField("double", 2.2)
        message.addField("decimal", BigDecimal("3.3"))
        message.addField("char", 'A')
        message.addField("bool", true)
        message.addField("boolY", true)
        message.addField("boolN", false)
        message.addField("enumInt", -1)
        val nestedComplex = DEFAULT_MESSAGE_FACTORY.createMessage("SubMessage", dictionary.namespace)
        nestedComplex.addField("field1", "field1")
        nestedComplex.addField("field2", "field2")
        val nestedComplexSecond = DEFAULT_MESSAGE_FACTORY.createMessage("SubMessage", dictionary.namespace)
        nestedComplexSecond.addField("field1", "field3")
        nestedComplexSecond.addField("field2", "field4")
        message.addField("complex", nestedComplex)
        val nestedComplexList = DEFAULT_MESSAGE_FACTORY.createMessage("SubComplexList", dictionary.namespace)
        nestedComplexList.addField("list", ImmutableList.of(nestedComplex, nestedComplexSecond))
        message.addField("complexList", nestedComplexList)
        return MessageWrapper(message)
    }

    companion object {
        private val K_LOGGER = KotlinLogging.logger {}

        const val BOOK = "book"
        const val SESSION_GROUP = "session-group"
    }
}