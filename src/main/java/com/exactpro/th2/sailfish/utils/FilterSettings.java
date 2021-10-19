/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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


import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;

public class FilterSettings {

    public static final FilterSettings DEFAULT_FILTER = new FilterSettings();

    private double decimalPrecision;
    private Duration timePrecision = Duration.ofSeconds(0L, 0L);


    public void setDecimalPrecision(double decimalPrecision) {
        this.decimalPrecision = decimalPrecision;
    }

    public double getDecimalPrecision() {
        return decimalPrecision;
    }

    public void setTimePrecision(@NotNull Duration timePrecision) {
        this.timePrecision = Objects.requireNonNull(timePrecision, "Time precision cannot be null");
    }

    public Duration getTimePrecision() {
        return timePrecision;
    }
}
