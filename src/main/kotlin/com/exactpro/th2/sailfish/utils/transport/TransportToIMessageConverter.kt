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
import com.exactpro.sf.common.messages.messageProperties
import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.IFieldStructure
import com.exactpro.sf.comparison.conversion.ConversionException
import com.exactpro.sf.comparison.conversion.IConverter
import com.exactpro.sf.comparison.conversion.impl.*
import com.exactpro.sf.configuration.suri.SailfishURI
import com.exactpro.sf.externalapi.IMessageFactoryProxy
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.toProto
import com.exactpro.th2.sailfish.utils.MessageConvertException
import com.exactpro.th2.sailfish.utils.MessageWrapper
import com.exactpro.th2.sailfish.utils.ToSailfishParameters
import com.exactpro.th2.sailfish.utils.UnknownEnumException
import mu.KotlinLogging
import org.apache.commons.lang3.BooleanUtils
import java.util.*
import java.util.function.BiFunction

class TransportToIMessageConverter @JvmOverloads constructor(
    private val messageFactory: IMessageFactoryProxy,
    private val dictionary: IDictionaryStructure? = null,
    private val dictionaryURI: SailfishURI? = null,
    private val parameters: ToSailfishParameters = ToSailfishParameters.DEFAULT,
) {
    fun fromTransport(
        book: String,
        sessionGroup: String,
        receivedMessage: ParsedMessage,
        useDictionary: Boolean
    ): MessageWrapper {
        K_LOGGER.debug { "Converting message $receivedMessage, use dictionary $useDictionary" }
        return MessageWrapper(
            with(receivedMessage) {
                require(type.isNotBlank()) { "Cannot convert message with blank message type" }
                if (useDictionary) {
                    require(dictionary != null) { "Cannot convert using dictionary without dictionary set" }
                    convertByDictionary(dictionary)
                } else {
                    body.convertWithoutDictionary(type)
                }
            }
        ).apply {
            messageId = receivedMessage.id.toProto(book, sessionGroup)
            if (receivedMessage.metadata.isNotEmpty()) {
                metaData.messageProperties = receivedMessage.metadata
            }
        }
    }

    private fun ParsedMessage.convertByDictionary(dictionary: IDictionaryStructure): IMessage {
        return dictionary.messages[type]?.let { messageStructure ->
            try {
                runCatching {
                    body.convertByDictionary(messageStructure)
                }.onFailure {
                    throw MessageConvertException(messageStructure.name, it)
                }.getOrThrow()
            } catch (e: Exception) {
                throw e
            }
        } ?: error("Message '$type' hasn't been found in dictionary")
    }

    private fun Any.convertByDictionary(messageStructure: IFieldStructure): IMessage {
        require(this is Map<*, *>) { "Expected '${Map::class.java}' value but got '${this::class.java}' for field '${messageStructure.name}'" }
        return convertByDictionary(messageStructure)
    }

    private fun Map<*, *>.convertByDictionary(parentStructure: IFieldStructure): IMessage =
        messageFactory.createMessage(
            dictionaryURI,
            parentStructure.referenceName ?: parentStructure.name
        ).apply {
            forEach { (fieldName, fieldValue) ->
                require(fieldName is String) {
                    "Field name should be string type instead of ${if (fieldName != null) fieldName::class.java else "null"} type, parent field name ${parentStructure.name}"
                }
                if (fieldValue == null) {
                    addField(fieldName, null)
                    return@forEach
                }
                val fieldStructure: IFieldStructure = parentStructure.fields[fieldName]
                    ?: error("Field '$fieldName' hasn't been found in message structure: ${parentStructure.name}")
                try {
                    runCatching {
                        val convertedValue = if (fieldStructure.isComplex) {
                            processComplex(fieldValue, fieldStructure)
                        } else {
                            fieldValue.convertSimple(fieldStructure)
                        }
                        addField(fieldName, convertedValue)

                        traverseField(this, fieldName, fieldValue, fieldStructure)
                    }.onFailure {
                        throw MessageConvertException(fieldStructure.name, it)
                    }.getOrThrow()
                } catch (e: Exception) {
                    throw e
                }
            }
            K_LOGGER.debug { "Converted message by dictionary: $this" }
        }

    private fun Map<*, *>.convertWithoutDictionary(messageType: String): IMessage =
        messageFactory.createMessage(dictionaryURI, messageType).apply {
            forEach { (fieldName, fieldValue) ->
                require(fieldName is String) {
                    "Field name should be string type instead of ${if (fieldName != null) fieldName::class.java else "null"} type, message type = $messageType"
                }
                require(fieldValue != null) { "$messageType message contains with null value in $fieldName filed" }
                val traverseField = fieldValue.traverseField(fieldName)
                addField(fieldName, traverseField)
            }
            K_LOGGER.debug { "Converted message without dictionary: $this" }
        }

    private fun Any.traverseField(fieldName: String): Any {
        return when (this) {
            is String -> this
            is Map<*, *> -> convertWithoutDictionary(fieldName)
            is List<*> -> convertList(fieldName)
            else -> error("The field '$fieldName' cannot be traversed, because it has an unrecognized type '${fieldName::class.java}'")
        }
    }

    private fun List<*>.convertList(fieldName: String): MutableList<*> {
        return this@convertList.asSequence()
            .mapIndexed { index, value ->
                require(value != null) { "$fieldName filed contains list with null value at $index index" }
                value.traverseField(fieldName)
            }
            .toMutableList()
    }

    private fun traverseField(
        message: IMessage,
        fieldName: String,
        value: Any,
        fieldStructure: IFieldStructure
    ) {
        val convertedValue =
            (if (fieldStructure.isComplex) processComplex(value, fieldStructure) else value.convertSimple(
                fieldStructure
            ))
        message.addField(fieldName, convertedValue)
    }

    private fun Any?.convertSimple(parentStructure: IFieldStructure): Any? {
        if (parentStructure.isCollection) {
            require(this is List<*>) {
                "Expected '${List::class.java}' value but got '${if (this != null) this::class.java else null}' for field '${parentStructure.name}'"
            }
            return this.convertList(parentStructure) { value: Any?, fieldStructure: IFieldStructure ->
                value.convertToTarget(fieldStructure)
            }
        }
        return this.convertToTarget(parentStructure)
    }

    private fun Any?.convertToTarget(fieldStructure: IFieldStructure): Any? {
        return when (this) {
            null -> null
            is String -> {
                val targetValue = if (fieldStructure.isEnum) convertEnumValue(fieldStructure, this) else this
                // TODO may be place its logic into the MultiConverter
                if (fieldStructure.javaType == JavaType.JAVA_LANG_BOOLEAN) {
                    BooleanUtils.toBooleanObject(targetValue)
                } else convertJavaType<Any>(
                    targetValue,
                    fieldStructure.javaType
                )
            }

            else -> error("Expected '${String::class.java}' value but got '${this::class.java}' for field '${fieldStructure.name}'")
        }
    }

    private fun convertEnumValue(fieldStructure: IFieldStructure, value: String): String {
        for ((key, value1) in fieldStructure.values) {
            val enumValue = value1.value
            if (key == value || enumValue == value) {
                return enumValue
            }
        }
        if (parameters.allowUnknownEnumValues) {
            return value
        }
        throw UnknownEnumException(fieldStructure.name, value, fieldStructure.namespace)
    }

    private fun processComplex(value: Any, fieldStructure: IFieldStructure): Any {
        if (fieldStructure.isCollection) {
            require(value is List<*>) { "Expected '${List::class.java}' value but got '${value::class.java}' for field '${fieldStructure.name}'" }
            return value.convertComplexList(fieldStructure)
        }
        return value.convertByDictionary(fieldStructure)
    }

    private fun List<*>.convertComplexList(fieldStructure: IFieldStructure): List<IMessage?> = convertList(
        fieldStructure
    ) { value: Any?, messageStructure: IFieldStructure ->
        value?.convertByDictionary(
            messageStructure
        )
    }

    private fun <T> List<*>.convertList(
        fieldStructure: IFieldStructure,
        mapper: BiFunction<Any?, IFieldStructure, T?>
    ): MutableList<T?> = ArrayList<T?>(size).apply {
        this@convertList.forEachIndexed { index, element ->
            runCatching {
                add(mapper.apply(element, fieldStructure))
            }.onFailure {
                throw MessageConvertException("[$index]", it)
            }
        }
    }

    companion object {
        private val K_LOGGER = KotlinLogging.logger {}
        private val CONVERTERS = initConverters()
        private fun initConverters(): Map<JavaType, IConverter<*>> {
            val target: MutableMap<JavaType, IConverter<*>> = EnumMap(JavaType::class.java)
            target[JavaType.JAVA_LANG_BOOLEAN] = BooleanConverter()
            target[JavaType.JAVA_LANG_BYTE] =
                ByteConverter()
            target[JavaType.JAVA_LANG_SHORT] = ShortConverter()
            target[JavaType.JAVA_LANG_INTEGER] = IntegerConverter()
            target[JavaType.JAVA_LANG_LONG] = LongConverter()
            target[JavaType.JAVA_LANG_FLOAT] = FloatConverter()
            target[JavaType.JAVA_LANG_DOUBLE] = DoubleConverter()
            target[JavaType.JAVA_MATH_BIG_DECIMAL] =
                BigDecimalConverter()
            target[JavaType.JAVA_LANG_CHARACTER] =
                CharacterConverter()
            target[JavaType.JAVA_LANG_STRING] =
                StringConverter()
            target[JavaType.JAVA_TIME_LOCAL_DATE] = LocalDateConverter()
            target[JavaType.JAVA_TIME_LOCAL_TIME] = LocalTimeConverter()
            target[JavaType.JAVA_TIME_LOCAL_DATE_TIME] = LocalDateTimeConverter()
            return target
        }

        private fun <T> convertJavaType(value: Any, javaType: JavaType): T {
            val converter = CONVERTERS[javaType]
                ?: throw ConversionException("No converter for type: " + javaType.value())
            return converter.convert(value) as T
        }
    }
}
