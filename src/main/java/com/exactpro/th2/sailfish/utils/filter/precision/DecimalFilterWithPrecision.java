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

package com.exactpro.th2.sailfish.utils.filter.precision;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.aml.scriptutil.MvelException;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.sailfish.utils.FilterSettings;
import com.exactpro.th2.sailfish.utils.filter.IOperationFilter;
import com.exactpro.th2.sailfish.utils.filter.util.FilterUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class DecimalFilterWithPrecision implements IOperationFilter {
	private final Comparable<?> value;
	private final FilterSettings filterSettings;


	public DecimalFilterWithPrecision(String simpleFilter, FilterSettings filterSettings) {
		this.value = convertValue(simpleFilter);
		this.filterSettings = filterSettings;
	}

	@Override
	public FilterOperation getOperation() {
		return FilterOperation.EQ_DECIMAL_PRECISION;
	}

	@Override
	public ExpressionResult validate(Object value) throws RuntimeException {
		Comparable<?> comparableValue = Objects.requireNonNull(convertValue(value));
		return ExpressionResult.create(compareValues(comparableValue, this.value));
	}

	@Override
	public String getCondition() {
		return "=" + getValue();
	}

	@Override
	public String getCondition(Object value) {
		return value + " " +  getCondition() + " " + getValue();
	}

	@Override
	public Object getValue() throws MvelException {
		return value;
	}

	@Override
	public boolean hasValue() {
		return true;
	}


	private boolean compareValues(Comparable<?> first, Comparable<?> second) {
		if (first.getClass() == second.getClass()) {
			if (first instanceof BigDecimal) {
				return ((BigDecimal) first).subtract((BigDecimal) second)
						.abs()
						.compareTo(BigDecimal.valueOf(filterSettings.getDecimalPrecision())) <= 0;
			}
			if (first instanceof Double) {
				return Math.abs((Double) first - (Double) second) <= filterSettings.getDecimalPrecision();
			}
		}

		throw new IllegalArgumentException(String.format("Failed to compare values {%s}, {%s} because it has an invalid types %s, %s", first, second, first.getClass(), second.getClass()));
	}


	private static Comparable<?> convertValue(Object value) {
		try {
			return FilterUtils.convertValue(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Failed to parse value to Number. Value = " + value, e);
		}
	}
}
