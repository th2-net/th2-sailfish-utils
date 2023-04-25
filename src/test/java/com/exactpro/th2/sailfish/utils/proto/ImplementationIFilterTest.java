/*
 * Copyright 2021-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.th2.sailfish.utils.proto;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.NullValue;
import com.exactpro.th2.common.grpc.RootComparisonSettings;
import com.exactpro.th2.common.grpc.RootMessageFilter;
import com.exactpro.th2.common.grpc.SimpleList;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.ValueFilter;
import com.exactpro.th2.sailfish.utils.FilterSettings;
import com.exactpro.th2.sailfish.utils.MessageWrapper;
import com.exactpro.th2.sailfish.utils.ProtoToIMessageConverter;
import com.exactpro.th2.sailfish.utils.RootComparisonSettingsUtils;
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy;
import com.google.protobuf.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ImplementationIFilterTest extends AbstractConverterTest {
    private static final String MESSAGE_TYPE = "TestMsg";
    private static final DefaultMessageFactoryProxy MESSAGE_FACTORY = new DefaultMessageFactoryProxy();
    private final static SailfishURI DICTIONARY_URI = SailfishURI.unsafeParse("test");
    private final ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
            MESSAGE_FACTORY, null, DICTIONARY_URI
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

    private static List<Arguments> twoDecimalFilterOperationWithPrecision() {
        return List.of(
                Arguments.of("10.0", "10.005", "0.005", StatusType.PASSED),
                Arguments.of("10", "10.005", "0.005", StatusType.PASSED),
                Arguments.of("10.0", "10.005", "5E-3", StatusType.PASSED),
                Arguments.of("10.0", "10.006", "0.005", StatusType.FAILED),
                Arguments.of("10.0", "10.006", "5E-3", StatusType.FAILED),
                Arguments.of("10", "10.005", "0.005", StatusType.PASSED),
                Arguments.of("10", "10.005", "0.004", StatusType.FAILED),
                Arguments.of("10.1", "10.005", "0.095", StatusType.PASSED),
                Arguments.of("10.1", "10", "0", StatusType.FAILED),
                Arguments.of("101E-1", "10005E-3", "95E-3", StatusType.PASSED)
        );
    }

    @ParameterizedTest
    @MethodSource("twoDecimalFilterOperationWithPrecision")
    void testDecimalFilterOperationFilterWithPrecision(String filterValue, String messageValue, String precision, StatusType status) {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("compareFilter", ValueFilter.newBuilder()
                                .setSimpleFilter(filterValue)
                                .setOperation(FilterOperation.EQ_DECIMAL_PRECISION)
                                .build())
                        .build())
                .setComparisonSettings(RootComparisonSettings.newBuilder()
                        .setDecimalPrecision(precision)
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("compareFilter", getSimpleValue(messageValue))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(status, result.getResult("compareFilter").getStatus());
    }

    @Test
    void testDecimalFilterWithPrecisionOnComplexMessage() {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(
                        messageFilter(
                                Map.of(
                                        "compareFilter",
                                        listValueFilter(messageFilter(
                                                        Map.of(
                                                                "Simple in rep group",
                                                                simpleValueFilter("2.22", FilterOperation.EQ_DECIMAL_PRECISION)
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .setComparisonSettings(RootComparisonSettings.newBuilder()
                        .setDecimalPrecision("0.01")
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields(
                        "compareFilter",
                        getListValue(
                                getComplex(
                                        "Test",
                                        Map.of("Simple in rep group", "2.221")
                                )
                        )
                ).build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertNotNull(result, "Result cannot be null");
        Assertions.assertEquals(StatusType.PASSED, ComparisonUtil.getStatusType(result), "Result should be passed");
    }

    private static List<Arguments> twoTimeFilterOperationWithPrecision() {
        return List.of(
                Arguments.of("2007-12-03T10:15:30", "2007-12-03T10:15:35", createDuration(5), StatusType.PASSED),
                Arguments.of("2007-12-03T10:15:30", "2007-12-03T10:15:36", createDuration(5), StatusType.FAILED),
                Arguments.of("2007-12-03T10:15:30", "2007-12-03T10:15:35.100000000", createDuration(5, 100000000), StatusType.PASSED),
                Arguments.of("2007-12-03T10:15:30", "2007-12-03T10:15:35.100000001", createDuration(5, 100000000), StatusType.FAILED)
        );
    }

    @ParameterizedTest
    @MethodSource("twoTimeFilterOperationWithPrecision")
    void testTimeFilterOperationFilterWithPrecision(String filterValue, String messageValue, Duration precision, StatusType status) {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("compareFilter", ValueFilter.newBuilder()
                                .setSimpleFilter(filterValue)
                                .setOperation(FilterOperation.EQ_TIME_PRECISION)
                                .build())
                        .build())
                .setComparisonSettings(RootComparisonSettings.newBuilder()
                        .setTimePrecision(precision)
                        .build())
                .build();

        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("compareFilter", getSimpleValue(messageValue))
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertEquals(status, result.getResult("compareFilter").getStatus());
    }

    private static List<Arguments> equalityFilterData() {
        return List.of(
                Arguments.of(
                        simpleValueFilter("1", FilterOperation.NOT_EQUAL),
                        getListValue(
                                Value.newBuilder()
                                        .setMessageValue(
                                                createMessageBuilder("test")
                                                        .putFields("A", getSimpleValue("1"))
                                                        .build()
                                        ).build(),
                                getSimpleValue("2")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Collection of Messages, expected: String"
                ),
                Arguments.of(
                        simpleValueFilter("text", FilterOperation.EQUAL),
                        getSimpleValue("text"),
                        StatusType.PASSED,
                        ""
                ),
                Arguments.of(
                        simpleValueFilter("1", FilterOperation.NOT_EQUAL),
                        getSimpleValue("1"),
                        StatusType.FAILED,
                        ""
                ),
                Arguments.of(
                        simpleValueFilter("1", FilterOperation.NOT_EQUAL),
                        getSimpleValue("2"),
                        StatusType.PASSED,
                        ""
                ),
                Arguments.of(
                        simpleValueFilter("1", FilterOperation.EQUAL),
                        getSimpleValue("2"),
                        StatusType.FAILED,
                        ""
                ),
                Arguments.of(
                        simpleValueFilter("4.5", FilterOperation.EQUAL),
                        getSimpleValue("4.5"),
                        StatusType.PASSED,
                        ""
                ),
                Arguments.of(
                        simpleValueFilter("1", FilterOperation.EQUAL),
                        getListValue(getSimpleValue("1"), getSimpleValue("2")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Collection of Strings, expected: String"
                ),
                Arguments.of(
                        simpleValueFilter("text", FilterOperation.EQUAL),
                        getListValue(getSimpleValue("1"), getSimpleValue("2")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Collection of Strings, expected: String"
                ),
                Arguments.of(
                        listValueFilter(FilterOperation.EQUAL, "1", "2", "3"),
                        getComplex("test", Collections.singletonMap("A", "1")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Message, expected: Collection"
                ),
                Arguments.of(
                        listValueFilter(
                                messageFilter(Map.of(
                                        "A", simpleValueFilter("1", FilterOperation.EQUAL)
                                )),
                                messageFilter(Map.of(
                                        "B", simpleValueFilter("2", FilterOperation.EQUAL)
                                ))
                        ),
                        getComplex("test", Collections.singletonMap("A", "1")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Message, expected: Collection of Messages"
                ),
                Arguments.of(
                        messageValueFilter(messageFilter(Map.of(
                                "A", simpleValueFilter("1", FilterOperation.EQUAL)
                        ))),
                        getListValue(getSimpleValue("1"), getSimpleValue("2")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Collection of Strings, expected: Message"
                ),
                Arguments.of(
                        simpleValueFilter("1", FilterOperation.NOT_EQUAL),
                        getListValue(getSimpleValue("1"), getSimpleValue("2")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Collection of Strings, expected: String"
                ),
                Arguments.of(
                        simpleValueFilter("10", FilterOperation.EQUAL),
                        getComplex("test", Collections.singletonMap("A", "1")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Message, expected: String"
                ),
                Arguments.of(
                        simpleValueFilter("10", FilterOperation.NOT_EQUAL),
                        getComplex("test", Collections.singletonMap("A", "1")),
                        StatusType.FAILED,
                        "Value type mismatch - actual: Message, expected: String"
                ),
                Arguments.of(
                        simpleValueFilter("10", FilterOperation.EQUAL),
                        Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build(),
                        StatusType.FAILED,
                        ""
                )
        );
    }

    @ParameterizedTest
    @MethodSource("equalityFilterData")
    void testEqualityFilter(ValueFilter valueFilter, Value messageValue, StatusType status, String exceptionMessage) {
        RootMessageFilter filter = RootMessageFilter.newBuilder()
                .setMessageType(MESSAGE_TYPE)
                .setMessageFilter(MessageFilter.newBuilder()
                        .putFields("A", valueFilter)
                        .build())
                .build();
        
        Message actual = createMessageBuilder(MESSAGE_TYPE)
                .putFields("A", messageValue)
                .build();

        ComparisonResult result = getResult(actual, filter);

        Assertions.assertNotNull(result, "Result cannot be null");
        ComparisonResult actualFields = result.getResult("A");
        Assertions.assertNotNull(actualFields, "Fields cannot be null");
        Assertions.assertEquals(status, actualFields.getStatus());
        Assertions.assertEquals(exceptionMessage, actualFields.getExceptionMessage());
    }

    private static Stream<Arguments> nullFilterPairs() {
        return Stream.of(
                Arguments.of(FilterOperation.EMPTY, StatusType.FAILED, false),
                Arguments.of(FilterOperation.NOT_EMPTY, StatusType.PASSED, false),
                Arguments.of(FilterOperation.EMPTY, StatusType.PASSED, true),
                Arguments.of(FilterOperation.NOT_EMPTY, StatusType.FAILED, true)
        );
    }
    
    @ParameterizedTest
    @MethodSource("nullFilterPairs")
    void testNullFilter(FilterOperation operation, StatusType status, boolean checkNullValueAsEmpty) {
        ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
                MESSAGE_FACTORY, null, DICTIONARY_URI, ProtoToIMessageConverter.createParameters().setUseMarkerForNullsInMessage(true)
        );
        Message message = createMessageBuilder("Test")
                .putFields("A", nullValue())
                .build();
        ValueFilter filter = ValueFilter.newBuilder().setOperation(operation).build();
        MessageFilter messageFilter = MessageFilter.newBuilder()
                .putFields("A", filter)
                .build();

        FilterSettings filterSettings = new FilterSettings();
        filterSettings.setCheckNullValueAsEmpty(checkNullValueAsEmpty);
        IMessage expected = converter.fromProtoFilter(messageFilter, filterSettings,"Test");
        IMessage actual = converter.fromProtoMessage(message, false);

        ComparisonResult result = MessageComparator.compare(actual, expected, new ComparatorSettings());
        assertNotNull(result, "Result cannot be null");
        assertEquals(status, ComparisonUtil.getStatusType(result), () -> "Unexpected result: " + result);
    }

    private ComparisonResult getResult(Message actual, RootMessageFilter filter) {
        MessageWrapper actualIMessage = converter.fromProtoMessage(actual, false);
        IMessage filterIMessage;
        if (filter.hasComparisonSettings()) {
            filterIMessage = converter.fromProtoFilter(
                    filter.getMessageFilter(),
                    RootComparisonSettingsUtils.convertToFilterSettings(filter.getComparisonSettings()),
                    filter.getMessageType());
        } else {
            filterIMessage = converter.fromProtoFilter(filter.getMessageFilter(), filter.getMessageType());
        } 

        return MessageComparator.compare(actualIMessage, filterIMessage, new ComparatorSettings());
    }
    
    private static Duration createDuration(long seconds, int nanos) {
        return Duration.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();
    }

    private static Duration createDuration(long seconds) {
        return createDuration(seconds, 0);
    }
}
