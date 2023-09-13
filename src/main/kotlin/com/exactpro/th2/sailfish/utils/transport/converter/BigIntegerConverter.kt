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