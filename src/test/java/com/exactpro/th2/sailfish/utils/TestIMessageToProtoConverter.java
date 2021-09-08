/******************************************************************************
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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
 ******************************************************************************/

package com.exactpro.th2.sailfish.utils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.Value.KindCase;
import com.exactpro.th2.sailfish.utils.IMessageToProtoConverter.Parameters;

class TestIMessageToProtoConverter extends AbstractConverterTest {

    @Test
    void convertsEmptyCollectionInField() {
        IMessage message = createMessage("Test");
        message.addField("emptyCol", Collections.emptyList());
        IMessageToProtoConverter converter = new IMessageToProtoConverter();
        Message convertedMessage = converter.toProtoMessage(message).build();
        assertEquals("Test", convertedMessage.getMetadata().getMessageType(), () -> "Converted message: " + convertedMessage);
        Map<String, Value> fieldsMap = convertedMessage.getFieldsMap();
        assertEquals(1, fieldsMap.size(), "Unexpected fields count: " + fieldsMap);

        Value emptyColValue = fieldsMap.get("emptyCol");
        assertNotNull(emptyColValue, () -> "Field doesn't have 'emptyCol' field: " + fieldsMap);
        assertEquals(KindCase.LIST_VALUE, emptyColValue.getKindCase(), () -> "Unexpected kind case: " + emptyColValue);

        ListValue listValue = emptyColValue.getListValue();
        assertNotNull(listValue, () -> "Null list value: " + emptyColValue);
        assertTrue(listValue.getValuesList().isEmpty(), () -> "List is not empty: " + listValue);
    }

    @Test
    void stripsTrailingZeroes() {
        IMessage message = createMessage("test");
        message.addField("bd", new BigDecimal("0.0000000"));
        message.addField("bdCollection", List.of(new BigDecimal("0.00000000")));
        Message protoMessage = new IMessageToProtoConverter(Parameters.builder().setStripTrailingZeros(true).build())
                .toProtoMessage(message).build();
        assertAll(
                () -> {
                    Value bd = protoMessage.getFieldsMap().get("bd");
                    assertNotNull(bd, () -> "Missing field in " + protoMessage);
                    assertEquals(getSimpleValue("0"), bd, () -> "Unexpected value: " + bd);
                },
                () -> {
                    Value bd = protoMessage.getFieldsMap().get("bdCollection");
                    assertNotNull(bd, () -> "Missing field in " + protoMessage);
                    assertEquals(getListValue(getSimpleValue("0")), bd, () -> "Unexpected value: " + bd);
                }
        );
    }

    @Test
    void convertsBigDecimalInPlainFormat() {
        IMessage message = createMessage("test");
        message.addField("bd", new BigDecimal("0.0000000"));
        message.addField("bdCollection", List.of(new BigDecimal("0.00000000")));
        Message protoMessage = new IMessageToProtoConverter()
                .toProtoMessage(message).build();

        assertAll(
                () -> {
                    Value bd = protoMessage.getFieldsMap().get("bd");
                    assertNotNull(bd, () -> "Missing field in " + protoMessage);
                    assertEquals("0.0000000", bd.getSimpleValue(), () -> "Unexpected value: " + bd);
                },
                () -> {
                    Value bd = protoMessage.getFieldsMap().get("bdCollection");
                    assertNotNull(bd, () -> "Missing field in " + protoMessage);
                    assertEquals(getListValue(getSimpleValue("0.00000000")), bd, () -> "Unexpected value: " + bd);
                }
        );
    }

    private static IMessage createMessage(String name) {
        return DefaultMessageFactory.getFactory().createMessage(name, "test");
    }
}