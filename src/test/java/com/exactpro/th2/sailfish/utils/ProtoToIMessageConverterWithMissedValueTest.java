/*
 * Copyright 2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.ListValueFilter;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.ValueFilter;
import com.exactpro.th2.common.message.MessageUtils;
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.exactpro.th2.common.value.ValueFilterUtilsKt.toValueFilter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProtoToIMessageConverterWithMissedValueTest extends AbstractConverterTest {
    private final DefaultMessageFactoryProxy messageFactory = new DefaultMessageFactoryProxy();
    private final SailfishURI dictionaryURI = SailfishURI.unsafeParse("test");
    private final ProtoToIMessageConverter converter = new ProtoToIMessageConverter(
            messageFactory, null, dictionaryURI
    );

    @ParameterizedTest
    @EnumSource(value = FilterOperation.class, names = {"UNRECOGNIZED"}, mode = EnumSource.Mode.EXCLUDE)
    void testExpectedNullValue(FilterOperation operation) {
        ValueFilter filter = ValueFilter.newBuilder().setOperation(operation).build();
        MessageFilter messageFilter = MessageFilter.newBuilder()
                .putFields("A", filter)
                .putFields("B", ValueFilter.newBuilder().setListFilter(ListValueFilter.newBuilder().addValues(filter)).build())
                .putFields("C", toValueFilter(MessageFilter.newBuilder().putFields("A", filter)))
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.fromProtoFilter(messageFilter, "Test")
        );
        
        assertEquals(String.format("Value filter '%s' doesn't contain a value", MessageUtils.toJson(filter)), exception.getMessage());
    }
}
