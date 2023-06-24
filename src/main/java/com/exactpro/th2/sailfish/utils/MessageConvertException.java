/*
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

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import java.util.stream.Stream;

/**
 * This exception contains an original cause which isn't an instance of the current class
 * and a path in a message related to the problem item.
 */
public class MessageConvertException extends RuntimeException {

    private final List<String> path;

    public MessageConvertException(String path, Throwable cause) {
        super(null, getOriginCause(cause), true, false);
        requireNonNull(path, "Path can't be null");

        if (cause instanceof MessageConvertException) {
            this.path = Stream.concat(Stream.of(path), ((MessageConvertException)cause).path.stream()).collect(toUnmodifiableList());
        } else {
            this.path = singletonList(path);
        }
    }

    private static Throwable getOriginCause(Throwable cause) {
        if (cause instanceof MessageConvertException) {
            return cause.getCause();
        }
        return requireNonNull(cause, "Origin cause can't be null");
    }

    @Override
    public String getMessage() {
        return "Message path: " + String.join(".", path) + ", cause: " + getCause().getMessage();
    }
}
