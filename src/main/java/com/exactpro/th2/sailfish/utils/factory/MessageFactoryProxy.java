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
package com.exactpro.th2.sailfish.utils.factory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.externalapi.IMessageFactoryProxy;
import com.exactpro.sf.externalapi.impl.StrictMessageFactoryWrapper;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public class MessageFactoryProxy implements IMessageFactoryProxy {

    private final IMessageFactory factory;
    private final StrictMessageFactoryWrapper strictFactory;

    public MessageFactoryProxy(
            @Nonnull IMessageFactory factory,
            @Nonnull SailfishURI uri,
            @Nonnull IDictionaryStructure dictionary
    ) {
        this.factory = requireNonNull(factory, "'factory' parameter");
        strictFactory = new StrictMessageFactoryWrapper(factory, dictionary);
        factory.init(uri,dictionary);
        strictFactory.init(uri, dictionary);
    }

    public MessageFactoryProxy(
            @Nonnull IMessageFactory factory,
            @Nonnull IDictionaryStructure dictionary
    ) throws SailfishURIException {
        this(factory, SailfishURI.parse(dictionary.getNamespace()), dictionary);
    }

    @Override
    public IMessage createMessage(SailfishURI dictionary, String name) {
        return factory.createMessage(name);
    }

    @Override
    public IMessage createStrictMessage(SailfishURI dictionary, String name) {
        return strictFactory.createMessage(name);
    }
}
