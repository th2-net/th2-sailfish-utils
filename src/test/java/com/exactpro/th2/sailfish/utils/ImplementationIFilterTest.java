/*
 *  Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.exactpro.th2.sailfish.utils;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.RootMessageFilter;
import com.exactpro.th2.common.grpc.SimpleList;
import com.exactpro.th2.common.grpc.ValueFilter;
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy;

public class ImplementationIFilterTest extends AbstractConverterTest {
    private static final String MESSAGE_TYPE = "TestMsg";
    private final DefaultMessageFactoryProxy messageFactory = new DefaultMessageFactoryProxy();
    private final SailfishURI dictionaryURI = SailfishURI.unsafeParse("test");
    private final ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
            messageFactory, null, dictionaryURI
    );

    @Test
    void listContainsValueFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("containFilter", ValueFilter.newBuilder()
                                .setSimpleList(SimpleList.newBuilder()
                                        .addAllSimpleValues(Arrays.asList("A", "B", "C")))
                                .setOperation(FilterOperation.IN)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("containFilter", getSimpleValue("A"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("containFilter").getStatus());
    }

    @Test
    void listDoesNotContainValueFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("containFilter", ValueFilter.newBuilder()
                                .setSimpleList(SimpleList.newBuilder()
                                        .addAllSimpleValues(Arrays.asList("A", "B", "C")))
                                .setOperation(FilterOperation.IN)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("containFilter", getSimpleValue("D"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.FAILED, result.getResult("containFilter").getStatus());
    }

    @Test
    void valueWasNotContainedInNotContainFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("notContain", ValueFilter.newBuilder()
                                .setSimpleList(SimpleList.newBuilder()
                                        .addAllSimpleValues(Arrays.asList("A", "B", "C")))
                                .setOperation(FilterOperation.NOT_IN)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("notContain", getSimpleValue("D"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("notContain").getStatus());
    }

    @Test
    void valueWasContainedInNotContainFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("notContain", ValueFilter.newBuilder()
                                .setSimpleList(SimpleList.newBuilder()
                                        .addAllSimpleValues(Arrays.asList("A", "B", "C")))
                                .setOperation(FilterOperation.NOT_IN)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("notContain", getSimpleValue("A"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.FAILED, result.getResult("notContain").getStatus());
    }

    @Test
    void regExGreedyFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("regexFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("A.+a")
                                .setOperation(FilterOperation.LIKE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("regexFilter", getSimpleValue("Abbbb Abba Abbbbabbba"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("regexFilter").getStatus());
    }

    @Test
    void regExPossessiveFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("regexFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("A.++a")
                                .setOperation(FilterOperation.LIKE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("regexFilter", getSimpleValue("Abbbb Abba Abbbbabbba"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.FAILED, result.getResult("regexFilter").getStatus());
    }

    @Test
    void regExLazyFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("regexFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("A.+?a")
                                .setOperation(FilterOperation.LIKE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("regexFilter", getSimpleValue("Abbbb Abba Abbbbabbba"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("regexFilter").getStatus());
    }

    @Test
    void notLikeFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("regexFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("A.+?a")
                                .setOperation(FilterOperation.NOT_LIKE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("regexFilter", getSimpleValue("Abbbb Abba Abbbbabbb"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("regexFilter").getStatus());
    }

    @Test
    void numberMoreFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("numberMoreFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("10.1")
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("numberMoreFilter", getSimpleValue("102"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("numberMoreFilter").getStatus());
    }

    @Test
    void numberMoreFilterWithError() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("numberMoreFilterWithError", ValueFilter.newBuilder()
                                .setSimpleFilter("10.1")
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("numberMoreFilterWithError", getSimpleValue("10,2"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.FAILED, result.getResult("numberMoreFilterWithError").getStatus());
        Assertions.assertEquals("Failed to parse value to Number. Value = 10,2", result.getResult("numberMoreFilterWithError").getExceptionMessage());

    }

    @Test
    void numberMoreFilterException() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("numberMoreFilterException", ValueFilter.newBuilder()
                                .setSimpleFilter("10,1")
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("numberMoreFilterException", getSimpleValue("102"))
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> getResult(actual, filter));
    }


    @Test
    void numberNotMoreFilterPositive() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("numberNotMoreFilterPositive", ValueFilter.newBuilder()
                                .setSimpleFilter("101")
                                .setOperation(FilterOperation.NOT_MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("numberNotMoreFilterPositive", getSimpleValue("10.2"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("numberNotMoreFilterPositive").getStatus());
    }

    @Test
    void numberNotMoreFilterEqual() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("numberNotMoreFilterEqual", ValueFilter.newBuilder()
                                .setSimpleFilter("101")
                                .setOperation(FilterOperation.NOT_MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("numberNotMoreFilterEqual", getSimpleValue("101"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("numberNotMoreFilterEqual").getStatus());
    }

    @Test
    void mathNotMoreFilterNegative() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("mathNotMoreFilterNegative", ValueFilter.newBuilder()
                                .setSimpleFilter("101")
                                .setOperation(FilterOperation.NOT_MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("mathNotMoreFilterNegative", getSimpleValue("100"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("mathNotMoreFilterNegative").getStatus());
    }

    @Test
    void mathLessFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("mathLessFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("101")
                                .setOperation(FilterOperation.LESS)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("mathLessFilter", getSimpleValue("10.0"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("mathLessFilter").getStatus());
    }

    @Test
    void mathNotLessFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("mathLessFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("10.1")
                                .setOperation(FilterOperation.NOT_LESS)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("mathLessFilter", getSimpleValue("100"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("mathLessFilter").getStatus());
    }

    @Test
    void dateTimeNotLessFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("dateTimeNotLessFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("2007-12-03T10:15:30")
                                .setOperation(FilterOperation.NOT_LESS)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("dateTimeNotLessFilter", getSimpleValue("2007-12-03T10:15:31"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("dateTimeNotLessFilter").getStatus());
    }

    @Test
    void dateMoreFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("dateMoreFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("2007-12-03")
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("dateMoreFilter", getSimpleValue("2007-12-04"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("dateMoreFilter").getStatus());
    }

    @Test
    void timeMoreFilter() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("timeMoreFilter", ValueFilter.newBuilder()
                                .setSimpleFilter("10:15:30")
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("timeMoreFilter", getSimpleValue("10:15:31"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("timeMoreFilter").getStatus());
    }

    @Test
    void dateTimeMoreFilterException() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("dateTimeMoreFilterException", ValueFilter.newBuilder()
                                .setSimpleFilter("2007-12-03T10-15:30")
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("dateTimeMoreFilterException", getSimpleValue("2007-12-03T10:15:30"))
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, ()->getResult(actual, filter));
    }

    @Test
    void dateTimeMoreFilterError() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("dateTimeMoreFilterNegative", ValueFilter.newBuilder()
                                .setSimpleFilter("2007-12-03T10:15:30")
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("dateTimeMoreFilterNegative", getSimpleValue("2007-12-03T10-15-30"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.FAILED, result.getResult("dateTimeMoreFilterNegative").getStatus());
        Assertions.assertEquals("Failed to parse value to Date. Value = 2007-12-03T10-15-30", result.getResult("dateTimeMoreFilterNegative").getExceptionMessage());
    }

    @Test
    void differentTemporalTypesTest() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("dateTimeMoreFilterNegative", ValueFilter.newBuilder()
                                .setSimpleFilter("10:15:30")
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("dateTimeMoreFilterNegative", getSimpleValue("2007-12-03T10:15:30"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.FAILED, result.getResult("dateTimeMoreFilterNegative").getStatus());
        Assertions.assertEquals("Failed to compare Temporal values {2007-12-03T10:15:30}, {10:15:30}", result.getResult("dateTimeMoreFilterNegative").getExceptionMessage());
    }

    @Test
    void wilcardFilterPossitive() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("wilcardFilterPossitive", ValueFilter.newBuilder()
                                .setSimpleFilter("c.*")
                                .setOperation(FilterOperation.WILDCARD)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("wilcardFilterPossitive", getSimpleValue("c.txt"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("wilcardFilterPossitive").getStatus());
    }

    @Test
    void wilcardFilterNegative() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("wilcardFilterNegative", ValueFilter.newBuilder()
                                .setSimpleFilter("*.?")
                                .setOperation(FilterOperation.WILDCARD)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("wilcardFilterNegative", getSimpleValue("c.txt"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.FAILED, result.getResult("wilcardFilterNegative").getStatus());
    }

    @Test
    void notWilcardFilterPossitive() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("notWilcardFilterPossitive", ValueFilter.newBuilder()
                                .setSimpleFilter("c.?")
                                .setOperation(FilterOperation.NOT_WILDCARD)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("notWilcardFilterPossitive", getSimpleValue("c.txt"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.PASSED, result.getResult("notWilcardFilterPossitive").getStatus());
    }

    @Test
    void notWilcardFilterNegative() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("notWilcardFilterNegative", ValueFilter.newBuilder()
                                .setSimpleFilter("?.*")
                                .setOperation(FilterOperation.NOT_WILDCARD)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("notWilcardFilterNegative", getSimpleValue("c.txt"))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(StatusType.FAILED, result.getResult("notWilcardFilterNegative").getStatus());
    }



    private ComparisonResult getResult(Message actual, RootMessageFilter filter) {
        MessageWrapper actualIMessage = converter.fromProtoMessage(actual, false);
        IMessage filterIMessage = converter.fromProtoFilter(filter.getMessageFilter(), filter.getMessageType());

        return MessageComparator.compare(actualIMessage, filterIMessage, new ComparatorSettings());
    }
}
