/*
 *  Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.messages.IMessage;

import java.util.List;

public class Messages {
    static int getSimpleFieldCountRecursive(IMessage message) {
        int fieldCount = 0;
        for (String fieldName : message.getFieldNames()) {
            if (message.isFieldSet(fieldName)) {
                Object field = message.getField(fieldName);
                if (field instanceof IMessage) {
                    fieldCount += getSimpleFieldCountRecursive((IMessage)field);
                } else if (field instanceof List<?>) {
                    fieldCount += getSimpleFieldCountRecursive((List<?>)field);
                } else {
                    fieldCount++;
                }
            }
        }
        return fieldCount;
    }

    static int getSimpleFieldCountRecursive(List<?> list) {
        int fieldCount = 0;
        if (!list.isEmpty()) {
            Object firstValue = list.get(0);
            if (firstValue instanceof IMessage) {
                fieldCount += list.stream()
                        .mapToInt(element -> getSimpleFieldCountRecursive((IMessage)element))
                        .sum();
            } else {
                fieldCount += list.size();
            }
        }
        return fieldCount;
    }
}
