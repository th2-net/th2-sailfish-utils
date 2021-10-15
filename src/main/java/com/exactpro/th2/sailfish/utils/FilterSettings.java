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

package com.exactpro.th2.sailfish.utils;


import java.time.Duration;

public class FilterSettings {

	private double decimalPrecision;
	private Duration timePrecision;


	public void setDecimalPrecision(String decimalPrecision) {
		if (decimalPrecision.isBlank()) {
			return;
		}
		this.decimalPrecision = Double.parseDouble(decimalPrecision);
	}

	public void setDecimalPrecision(double decimalPrecision) {
		this.decimalPrecision = decimalPrecision;
	}

	public double getDecimalPrecision() {
		return decimalPrecision;
	}

	public void setTimePrecision(Duration timePrecision) {
		this.timePrecision = timePrecision;
	}

	public Duration getTimePrecision() {
		return timePrecision;
	}
}
