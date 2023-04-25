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

fun getSimpleFieldCountRecursive(message: IMessage): Int {
    var fieldCount = 0
    for (fieldName in message.fieldNames) {
        if (message.isFieldSet(fieldName)) {
            when (val field = message.getField<Any>(fieldName)) {
                is IMessage -> fieldCount += getSimpleFieldCountRecursive(field)
                is List<*> -> fieldCount += getSimpleFieldCountRecursive(field)
                else -> fieldCount++
            }
        }
    }
    return fieldCount
}

fun getSimpleFieldCountRecursive(list: List<*>): Int {
    var fieldCount = 0
    if (list.isNotEmpty()) {
        val firstValue = list[0]!!
        fieldCount += if (firstValue is IMessage) {
            list.stream()
                .mapToInt { element: Any? -> getSimpleFieldCountRecursive(element as IMessage) }
                .sum()
        } else {
            list.size
        }
    }
    return fieldCount
}
