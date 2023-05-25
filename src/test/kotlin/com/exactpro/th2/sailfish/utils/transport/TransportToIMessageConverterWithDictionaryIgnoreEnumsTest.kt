/*
 * Copyright 2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.th2.sailfish.utils.transport

import com.exactpro.th2.sailfish.utils.MessageWrapper
import com.exactpro.th2.sailfish.utils.ToSailfishParameters
import com.exactpro.th2.sailfish.utils.factory.DefaultMessageFactoryProxy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

internal class TransportToIMessageConverterWithDictionaryIgnoreEnumsTest : AbstractTransportToIMessageConverterTest() {
    private val converter = TransportToIMessageConverter(
        DefaultMessageFactoryProxy(),
        dictionary,
        dictionaryURI,
        ToSailfishParameters(allowUnknownEnumValues = true)
    )

    @Test
    fun unknownEnumExceptionTest() {
        val transportMessage = createMessage().apply {
            bodyBuilder().apply {
                put("enumInt", "5")
            }
        }.build()
        val message = assertDoesNotThrow<MessageWrapper>(
            { converter.fromTransport(BOOK, SESSION_GROUP, transportMessage, true) },
            "Unknown enum value should not fail"
        )
        Assertions.assertEquals(5, message.getField("enumInt")) { "Unexpected result: $message" }
    }
}