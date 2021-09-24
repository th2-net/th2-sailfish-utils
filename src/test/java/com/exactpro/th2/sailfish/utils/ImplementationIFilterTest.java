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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    private static List<Arguments> inOperationFilter() {
        return List.of(
                Arguments.of("A", StatusType.PASSED, FilterOperation.IN),
                Arguments.of("D", StatusType.FAILED, FilterOperation.IN),
                Arguments.of("D", StatusType.PASSED, FilterOperation.NOT_IN),
                Arguments.of("A", StatusType.FAILED, FilterOperation.NOT_IN)
        );
    }

    @ParameterizedTest
    @MethodSource("inOperationFilter")
    void testListContainsValueFilter(String value, StatusType status, FilterOperation operation) {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("containFilter", ValueFilter.newBuilder()
                                .setSimpleList(SimpleList.newBuilder()
                                        .addAllSimpleValues(List.of("A", "B", "C")))
                                .setOperation(operation)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("containFilter", getSimpleValue(value))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(status, result.getResult("containFilter").getStatus());
    }

    private static List<Arguments> twoSimpleValuesFilterOperation() {
        return List.of(
                Arguments.of("10.1", "11", StatusType.PASSED, FilterOperation.MORE),
                Arguments.of("2007-12-03T10:15:30", "2007-12-03T10:15:30", StatusType.FAILED, FilterOperation.MORE),
                Arguments.of("10.2", "10.2", StatusType.PASSED, FilterOperation.NOT_MORE),
                Arguments.of("10:15:30", "10:15:31", StatusType.FAILED, FilterOperation.NOT_MORE),
                Arguments.of("10.2", "10", StatusType.PASSED, FilterOperation.NOT_MORE),
                Arguments.of("10.1", "10", StatusType.PASSED, FilterOperation.LESS),
                Arguments.of("2007-12-03", "2008-12-03", StatusType.FAILED, FilterOperation.LESS),
                Arguments.of("2007-12-03T10:15:30", "2007-12-03T10:15:30", StatusType.PASSED, FilterOperation.NOT_LESS),
                Arguments.of("10.1", "10.2", StatusType.PASSED, FilterOperation.NOT_LESS),
                Arguments.of("10.1", "10", StatusType.FAILED, FilterOperation.NOT_LESS),
                Arguments.of("c.*", "c.txt", StatusType.PASSED, FilterOperation.WILDCARD),
                Arguments.of("*.?", "c.txt", StatusType.FAILED, FilterOperation.WILDCARD),
                Arguments.of("??.*", "c.txt", StatusType.PASSED, FilterOperation.NOT_WILDCARD),
                Arguments.of("?*", "c.txt", StatusType.FAILED, FilterOperation.NOT_WILDCARD),
                Arguments.of("A.+a", "Abbbb Abba Abbbbabbba", StatusType.PASSED, FilterOperation.LIKE),
                Arguments.of("A.++a", "Abbbb Abba Abbbbabbba", StatusType.FAILED, FilterOperation.LIKE),
                Arguments.of("A.+?a", "Abbbb Abba Abbbbabbba", StatusType.PASSED, FilterOperation.LIKE),
                Arguments.of("A.+?b", "Abbbb Abba Abbbbabbba", StatusType.PASSED, FilterOperation.NOT_LIKE),
                Arguments.of("A.+?a", "Abbbb Abba Abbbbabbba", StatusType.FAILED, FilterOperation.NOT_LIKE)

        );
    }

    @ParameterizedTest
    @MethodSource("twoSimpleValuesFilterOperation")
    void testTwoSimpleValuesFilterOperationFilter(String filterValue, String messageValue, StatusType status, FilterOperation operation) {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("compareFilter", ValueFilter.newBuilder()
                                .setSimpleFilter(filterValue)
                                .setOperation(operation)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("compareFilter", getSimpleValue(messageValue))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(status, result.getResult("compareFilter").getStatus());
    }

    private static List<Arguments> filterError() {
        return List.of(
                Arguments.of("10.1", "10,2", StatusType.FAILED, FilterOperation.LESS, "Failed to parse value to Number. Value = 10,2"),
                Arguments.of("2007-12-03T10:15:30", "2007-12-03T10-15-30", StatusType.FAILED, FilterOperation.MORE, "Failed to parse value to Date. Value = 2007-12-03T10-15-30"),
                Arguments.of("2007-12-03", "2007-12-03T10:15:30", StatusType.FAILED, FilterOperation.NOT_MORE, "Failed to compare Temporal values {2007-12-03T10:15:30}, {2007-12-03}")
        );
    }

    @ParameterizedTest
    @MethodSource("filterError")
    void testFilterWithError(String first, String second, StatusType status, FilterOperation operation, String errorMessage) {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("errorFilter", ValueFilter.newBuilder()
                                .setSimpleFilter(first)
                                .setOperation(operation)
                                .build())
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("errorFilter", getSimpleValue(second))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(status, result.getResult("errorFilter").getStatus());
        Assertions.assertEquals(errorMessage, result.getResult("errorFilter").getExceptionMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10,1", "2007-12-03T10-15:30"})
    void testFilterWithException(String first) {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("filterException", ValueFilter.newBuilder()
                                .setSimpleFilter(first)
                                .setOperation(FilterOperation.MORE)
                                .build())
                        .build())
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> converter.fromProtoFilter(filter.getMessageFilter(), filter.getMessageType()));
    }

    private ComparisonResult getResult(Message actual, RootMessageFilter filter) {
        MessageWrapper actualIMessage = converter.fromProtoMessage(actual, false);
        IMessage filterIMessage = converter.fromProtoFilter(filter.getMessageFilter(), filter.getMessageType());

        return MessageComparator.compare(actualIMessage, filterIMessage, new ComparatorSettings());
    }
}
