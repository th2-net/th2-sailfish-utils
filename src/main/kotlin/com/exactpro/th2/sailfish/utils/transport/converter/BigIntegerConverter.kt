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

package com.exactpro.th2.sailfish.utils.transport.converter

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType
import java.math.BigInteger

internal object BigIntegerConverter {
    fun convertToString(value: BigInteger): String = value.toString()

    fun convert(value: BigInteger, target: JavaType): Any {
        return when (target) {
            JavaType.JAVA_LANG_SHORT -> value.shortValueExact()
            JavaType.JAVA_LANG_INTEGER -> value.intValueExact()
            JavaType.JAVA_LANG_LONG -> value.longValueExact()
            JavaType.JAVA_LANG_BYTE -> value.byteValueExact()
            JavaType.JAVA_LANG_FLOAT -> value.toFloat()
            JavaType.JAVA_LANG_DOUBLE -> value.toDouble()
            JavaType.JAVA_LANG_STRING -> convertToString(value)
            JavaType.JAVA_MATH_BIG_DECIMAL -> value.toBigDecimal()
            JavaType.JAVA_TIME_LOCAL_DATE_TIME,
            JavaType.JAVA_TIME_LOCAL_DATE,
            JavaType.JAVA_TIME_LOCAL_TIME,
            JavaType.JAVA_LANG_CHARACTER,
            JavaType.JAVA_LANG_BOOLEAN,
            -> throw IllegalArgumentException("cannot convert from ${BigInteger::class.simpleName} to $target")
        }
    }
}