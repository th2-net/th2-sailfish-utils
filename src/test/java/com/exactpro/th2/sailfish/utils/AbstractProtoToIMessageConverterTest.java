/**
 * Copyright 2020-2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.exactpro.sf.comparison.ComparisonUtil.getResultCount;
import static com.exactpro.sf.comparison.MessageComparator.compare;
import static com.exactpro.sf.scriptrunner.StatusType.CONDITIONALLY_FAILED;
import static com.exactpro.sf.scriptrunner.StatusType.CONDITIONALLY_PASSED;
import static com.exactpro.sf.scriptrunner.StatusType.FAILED;
import static com.exactpro.sf.scriptrunner.StatusType.PASSED;
import static com.exactpro.th2.sailfish.utils.Messages.getSimpleFieldCountRecursive;

public class AbstractProtoToIMessageConverterTest extends AbstractConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProtoToIMessageConverterTest.class);

    protected void assertPassed(IMessage expected, IMessage actual) {
        ComparisonResult comparisonResult = compare(actual, expected, new ComparatorSettings());
        LOGGER.debug("Message comparison result: {}", comparisonResult);
        Assertions.assertEquals(getSimpleFieldCountRecursive(expected),
                getResultCount(comparisonResult, PASSED));
        Assertions.assertEquals(0, getResultCount(comparisonResult, FAILED));
        Assertions.assertEquals(0, getResultCount(comparisonResult, CONDITIONALLY_FAILED));
        Assertions.assertEquals(0, getResultCount(comparisonResult, CONDITIONALLY_PASSED));
    }

}
