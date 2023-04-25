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
package com.exactpro.th2.sailfish.utils

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*

class FromSailfishParameters (
    val stripTrailingZeros: Boolean = false,
    val dateTimeFormatter: DateTimeFormatter = DATE_TIME_FORMATTER,
    val timeFormatter: DateTimeFormatter = TIME_FORMATTER
) {
    companion object {
        private val TIME_FORMATTER = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 3, 9, true)
            .toFormatter(Locale.getDefault(Locale.Category.FORMAT))

        private val DATE_TIME_FORMATTER = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(TIME_FORMATTER)
            .toFormatter(Locale.getDefault(Locale.Category.FORMAT))

        val DEFAULT = FromSailfishParameters()
    }
}
