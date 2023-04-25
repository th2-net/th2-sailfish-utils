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
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.sailfish.utils.MessageWrapper
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy
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
    protected val dictionaryURI: SailfishURI = SailfishURI.unsafeParse(dictionary.namespace)

    protected fun assertPassed(expected: IMessage, actual: IMessage) {
        val comparisonResult = MessageComparator.compare(actual, expected, ComparatorSettings())
        K_LOGGER.debug { "Message comparison result: $comparisonResult" }
        Assertions.assertEquals(
            getSimpleFieldCountRecursive(expected),
            ComparisonUtil.getResultCount(comparisonResult, StatusType.PASSED)
        )
        Assertions.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED))
        Assertions.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.CONDITIONALLY_FAILED))
        Assertions.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.CONDITIONALLY_PASSED))
    }

    protected fun createMessage(): ParsedMessage = ParsedMessage.newSoftMutable().apply {
        type = "RootWithNestedComplex"
        with(body) {
            put("string", "StringValue")
            put("byte", "0")
            put("short", "1")
            put("int", "2")
            put("long", "3")
            put("float", "1.1")
            put("double", "2.2")
            put("decimal", "3.3")
            put("char", "A")
            put("bool", "true")
            put("boolY", "Y")
            put("boolN", "n")
            put("enumInt", "5")
            put("enumInt", "MINUS_ONE")
            put("complex", mapOf("field1" to "field1", "field2" to "field2"))
            put(
                "complexList", mapOf(
                    "list" to listOf(
                        mapOf("field1" to "field1", "field2" to "field2"),
                        mapOf("field1" to "field3", "field2" to "field4")
                    )
                )
            )
        }
    }

    protected fun createExpectedIMessage(): MessageWrapper {
        val message = DefaultMessageFactoryProxy().createMessage(dictionaryURI, "RootWithNestedComplex")
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
        val nestedComplex = DefaultMessageFactoryProxy().createMessage(dictionaryURI, "SubMessage")
        nestedComplex.addField("field1", "field1")
        nestedComplex.addField("field2", "field2")
        val nestedComplexSecond = DefaultMessageFactoryProxy().createMessage(dictionaryURI, "SubMessage")
        nestedComplexSecond.addField("field1", "field3")
        nestedComplexSecond.addField("field2", "field4")
        message.addField("complex", nestedComplex)
        val nestedComplexList = DefaultMessageFactoryProxy().createMessage(dictionaryURI, "SubComplexList")
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