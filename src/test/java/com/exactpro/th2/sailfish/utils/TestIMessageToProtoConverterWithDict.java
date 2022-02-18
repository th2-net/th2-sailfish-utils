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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Message.Builder;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.sailfish.utils.IMessageToProtoConverter.Parameters;

public class TestIMessageToProtoConverterWithDict extends AbstractConverterTest {
    private static IMessageToProtoConverter converter;
    @BeforeAll
    static void beforeAll() {
        try {
            IDictionaryStructure dictionary = new XmlDictionaryStructureLoader().load(
                    Files.newInputStream(Path.of("src", "test", "resources", "dictionary.xml")));
            converter = new IMessageToProtoConverter(dictionary, Parameters.builder().setReplaceValuesWithEnumNames(true).build());
        } catch (IOException e) {
            throw new RuntimeException("could not create converter", e);
        }
    }

    @Test
    void replacesEnumWithNames() {
        IMessage message = createMessage("Enums");
        message.addField("enumInt", 1);
        message.addField("enumIntCol", List.of(-1));
        IMessage inner = createMessage("Enums");
        inner.addField("enumInt", 1);
        inner.addField("enumIntCol", List.of(2));
        message.addField("enumMsg", inner);
        message.addField("enumMsgCol", List.of(inner));

        Message msg = converter.toProtoMessage(message).build();
        var expectedBuilder = createMessageBuilder("Enums");
        expectedBuilder.putFields("enumInt", getSimpleValue("ONE"));
        expectedBuilder.putFields("enumIntCol", getListValue(getSimpleValue("MINUS_ONE")));

        var expectedInnerBuilder = Message.newBuilder();
        expectedInnerBuilder.putFields("enumInt", getSimpleValue("ONE"));
        expectedInnerBuilder.putFields("enumIntCol", getListValue(getSimpleValue("2")));

        Value expectedInner = Value.newBuilder().setMessageValue(expectedInnerBuilder.build()).build();
        expectedBuilder.putFields("enumMsg", expectedInner);
        expectedBuilder.putFields("enumMsgCol", getListValue(expectedInner));
        Assertions.assertEquals(expectedBuilder.build(), msg);
    }
}
