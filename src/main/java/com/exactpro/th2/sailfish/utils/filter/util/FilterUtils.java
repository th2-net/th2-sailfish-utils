/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.sailfish.utils.filter.util;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class FilterUtils {
	public static Comparable<?> convertValue(String value) {
		if (value.contains(String.valueOf(DecimalFormatSymbols.getInstance().getDecimalSeparator()))) {
			return new BigDecimal(value);
		}
		return Long.parseLong(value);
	}

	@Nullable
	public static Comparable<?> convertValue(Object value) {
		if (value instanceof String) {
			return convertValue((String) value);
		} else if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		} else if (value instanceof Double) {
			return (Double) value;
		}
		return null;
	}

	public static Comparable<?> convertDateValue(String value) {
		if (value.contains(":")) {
			if (value.contains("-")) {
				return LocalDateTime.parse(value);
			}
			return LocalTime.parse(value);
		}
		return LocalDate.parse(value);
	}

	@Nullable
	public static Comparable<?> convertDateValue(Object value) {
		if (value instanceof String) {
			return convertDateValue((String) value);
		} else if (value instanceof LocalDateTime) {
			return (LocalDateTime) value;
		} else if (value instanceof LocalTime) {
			return (LocalTime) value;
		} else if (value instanceof LocalDate) {
			return (LocalDate) value;
		}
		return null;
	}
}
