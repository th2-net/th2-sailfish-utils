/*
 *  Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageConvertException extends RuntimeException {

    private final List<String> path;

    public MessageConvertException(String message) {
        super(message);

        this.path = Collections.emptyList();
    }

    public MessageConvertException(String path, String message) {
        super(message);

        this.path = Collections.singletonList(path);
    }

    public MessageConvertException(String path, Throwable cause) {
        super(cause.getMessage(), cause);

        if (cause instanceof MessageConvertException) {
            this.path = Stream.concat(Stream.of(path), ((MessageConvertException)cause).path.stream()).collect(Collectors.toUnmodifiableList());
        } else {
            this.path = Collections.singletonList(path);
        }
    }

    public String getMessageWithPath() {
        return "Message path: " + String.join(".", path) + ", cause: " + getMessage();
    }
}
