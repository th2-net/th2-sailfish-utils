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
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage.Companion.newMutable
import com.exactpro.th2.sailfish.utils.FromSailfishParameters
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime

class IMessageToTransportConverter @JvmOverloads constructor(private val parameters: FromSailfishParameters = FromSailfishParameters.DEFAULT) {

    fun toTransport(message: IMessage): ParsedMessage = newMutable().apply {
        type = message.name
        message.fieldNames.forEach { fieldName ->
            message.getField<Any?>(fieldName)?.let { fieldValue ->
                body[fieldName] = fieldValue.toTransport()
            }
        }
    }

    private fun Any.toTransport(): Any = when (this) {
        is IMessage -> toTransportSubMessage()
        is List<*> -> toTransportList()
        else -> toTransportString()
    }

    // FIXME: add check that all elements have the same type
    private fun List<*>.toTransportList(): MutableList<Any?> = asSequence().map { value ->
        when (value) {
            is IMessage -> value.toTransportSubMessage()
            else -> value?.toTransportString()
        }
    }.toMutableList()

    private fun Any.toTransportString(): String = when (this) {
        is BigDecimal -> (if (parameters.stripTrailingZeros) stripTrailingZeros() else this).toPlainString()
        is LocalDateTime -> format(parameters.dateTimeFormatter)
        is LocalTime -> format(parameters.timeFormatter)
        else -> toString()
    }

    private fun IMessage.toTransportSubMessage(): MutableMap<String, Any> = mutableMapOf<String, Any>().apply {
        fieldNames.forEach { fieldName ->
            put(fieldName, getField<Any?>(fieldName).toTransport())
        }
    }
}