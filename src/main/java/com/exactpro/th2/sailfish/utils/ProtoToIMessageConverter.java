/*
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
 */
package com.exactpro.th2.sailfish.utils;

import static com.exactpro.sf.common.impl.messages.xml.configuration.JavaType.JAVA_LANG_BOOLEAN;
import static com.google.protobuf.TextFormat.shortDebugString;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.externalapi.IMessageFactoryProxy;
import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.ListValueFilter;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.MetadataFilter;
import com.exactpro.th2.common.grpc.MetadataFilter.SimpleFilter;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.Value.KindCase;
import com.exactpro.th2.common.grpc.ValueFilter;
import com.google.protobuf.InvalidProtocolBufferException;

public class ProtoToIMessageConverter {
    private static final Logger logger = LoggerFactory.getLogger(ProtoToIMessageConverter.class.getName());
    private final IMessageFactoryProxy messageFactory;
    private final IDictionaryStructure dictionary;
    private final SailfishURI dictionaryURI;

    public ProtoToIMessageConverter(@NotNull IMessageFactoryProxy messageFactory,
                                    @Nullable IDictionaryStructure dictionaryStructure,
                                    SailfishURI dictionaryURI) {
        this.messageFactory = requireNonNull(messageFactory, "'Message factory' parameter");
        this.dictionary = dictionaryStructure;
        this.dictionaryURI = dictionaryURI;
    }

    public MessageWrapper fromProtoMessage(byte[] messageData, boolean useDictionary) throws InvalidProtocolBufferException {
        Message receivedMessage = Message.parseFrom(messageData);
        return fromProtoMessage(receivedMessage, useDictionary);
    }

    @NotNull
    public MessageWrapper fromProtoMessage(Message receivedMessage, boolean useDictionary) {
        if (logger.isDebugEnabled()) {
            logger.debug("Converting message {} {} dictionary", shortDebugString(receivedMessage), useDictionary ? "using" : "without");
        }

        String messageType = requireNonNull(receivedMessage.getMetadata().getMessageType(),
                "'Metadata.messageType' must not be null");
        if (messageType.isBlank()) {
            throw new IllegalArgumentException("Cannot convert message with blank message type");
        }
        IMessage convertedMessage = useDictionary
                ? convertByDictionary(receivedMessage.getFieldsMap(), messageType)
                : convertWithoutDictionary(receivedMessage.getFieldsMap(), messageType);
        MessageWrapper messageWrapper = new MessageWrapper(convertedMessage);
        messageWrapper.setMessageId(receivedMessage.getMetadata().getId());
        return messageWrapper;
    }

    public IMessage fromProtoFilter(MessageFilter messageFilter, String messageName) {
        if (logger.isDebugEnabled()) {
            logger.debug("Converting filter {} as {}", shortDebugString(messageFilter), messageName);
        }
        IMessage message = messageFactory.createMessage(dictionaryURI, messageName);
        for (Entry<String, ValueFilter> filterEntry : messageFilter.getFieldsMap().entrySet()) {
            message.addField(filterEntry.getKey(), traverseFilterField(filterEntry.getKey(), filterEntry.getValue()));
        }

        logger.debug("Filter '{}' converted {}", messageName, message);
        return message;
    }

    public IMessage fromMetadataFilter(MetadataFilter filter, String messageName) {
        if (logger.isTraceEnabled()) {
            logger.trace("Converting filter {} as {}", shortDebugString(filter), messageName);
        }
        IMessage message = messageFactory.createMessage(dictionaryURI, messageName);
        for (Entry<String, SimpleFilter> filterEntry : filter.getPropertyFiltersMap().entrySet()) {
            SimpleFilter propertyFilter = filterEntry.getValue();
            message.addField(filterEntry.getKey(), toSimpleFilter(propertyFilter.getOperation(), propertyFilter.getValue()));
        }

        logger.trace("Metadata filter converted to '{}': {}", messageName, message);
        return message;
    }

    private Object traverseFilterField(String fieldname, ValueFilter value) {
        if (value.hasListFilter()) {
            return traverseCollection(fieldname, value.getListFilter());
        }
        if (value.hasMessageFilter()) {
            return fromProtoFilter(value.getMessageFilter(), fieldname);
        }
        return toSimpleFilter(value.getOperation(), value.getSimpleFilter());
    }

    private Object toSimpleFilter(FilterOperation operation, String simpleFilter) {
        switch (operation) {
            case EQUAL:
                return StaticUtil.simpleFilter(0, null, StringUtil.enclose(StringEscapeUtils.escapeJava(simpleFilter)));
            case NOT_EQUAL:
                // Enclose value to single quotes isn't required for arguments
                return StaticUtil.filter(0, null, "x != value", "value", simpleFilter);
            case EMPTY:
                return StaticUtil.nullFilter(0, null);
            case NOT_EMPTY:
                return StaticUtil.notNullFilter(0, null);
            default:
                throw new IllegalArgumentException("Unsupported operation " + operation);
        }
    }

    private Object traverseCollection(String fieldName, ListValueFilter listFilter) {
        return listFilter.getValuesList().stream()
                .map(value -> traverseFilterField(fieldName, value))
                .collect(Collectors.toList());
    }

    private IMessage convertByDictionary(Map<String, Value> fieldsMap, String messageType) {
        if (dictionary == null) {
            throw new IllegalStateException("Cannot convert using dictionary without dictionary set");
        }
        IMessageStructure messageStructure = dictionary.getMessages().get(messageType);
        if (messageStructure == null) {
            throw new MessageConvertException(messageType, "Unknown message: " + messageType);
        }
        try {
            return convertByDictionary(fieldsMap, messageStructure);
        } catch (RuntimeException e) {
            throw new MessageConvertException(messageStructure.getName(), e);
        }
    }

    private IMessage convertByDictionary(Map<String, Value> fieldsMap, @NotNull IFieldStructure messageStructure) {
        IMessage message = messageFactory.createMessage(dictionaryURI, messageStructure.getName());
        for (Entry<String, Value> fieldEntry : fieldsMap.entrySet()) {
            String fieldName = fieldEntry.getKey();
            Value fieldValue = fieldEntry.getValue();

            IFieldStructure fieldStructure = messageStructure.getFields().get(fieldName);
            if (fieldStructure == null) {
                throw new MessageConvertException(fieldName, "Filed '" + fieldName + "' isn't found in message structure");
            }
            try {
                traverseField(message, fieldName, fieldValue, fieldStructure);
            } catch (RuntimeException e) {
                throw new MessageConvertException(fieldStructure.getName(), e);
            }
        }
        logger.debug("Converted message by dictionary: {}", message);
        return message;
    }

    private IMessage convertWithoutDictionary(Map<String, Value> fieldsMap, String messageType) {
        IMessage message = messageFactory.createMessage(dictionaryURI, messageType);
        for (Entry<String, Value> fieldEntry : fieldsMap.entrySet()) {
            String fieldName = fieldEntry.getKey();
            Value fieldValue = fieldEntry.getValue();
            Object traverseField = traverseField(fieldName, fieldValue);
            message.addField(fieldName, traverseField);
        }
        logger.debug("Converted message without dictionary: {}", message);
        return message;
    }

    private Object traverseField(String fieldName, Value fieldValue) {
        if (fieldValue.hasMessageValue()) {
            return convertWithoutDictionary(fieldValue.getMessageValue().getFieldsMap(), fieldName);
        } else if (fieldValue.hasListValue()) {
            return convertList(fieldName, fieldValue.getListValue());
        }
        return fieldValue.getSimpleValue();
    }

    private List<?> convertList(String fieldName, ListValue list) {
        return list.getValuesList().stream()
                .map(value -> traverseField(fieldName, value))
                .collect(Collectors.toList());
    }

    private void traverseField(IMessage message, String fieldName, Value value, @NotNull IFieldStructure fieldStructure) {
        Object convertedValue = fieldStructure.isComplex()
                ? processComplex(value, fieldStructure)
                : convertSimple(value, fieldStructure);
        message.addField(fieldName, convertedValue);
    }

    private Object convertSimple(Value value, IFieldStructure fieldStructure) {
        if (fieldStructure.isCollection()) {
            checkKind(value, fieldStructure.getName(), KindCase.LIST_VALUE);
            return value.getListValue().getValuesList()
                    .stream()
                    .map(element -> convertToTarget(element, fieldStructure))
                    .collect(Collectors.toList());
        }
        return convertToTarget(value, fieldStructure);
    }

    @Nullable
    private Object convertToTarget(Value value, IFieldStructure fieldStructure) {
        try {
            KindCase kindCase = value.getKindCase();
            if (kindCase == KindCase.NULL_VALUE || kindCase == KindCase.KIND_NOT_SET) {
                return null; // skip null value conversion
            }
            checkKind(value, fieldStructure.getName(), KindCase.SIMPLE_VALUE);
            String simpleValue = value.getSimpleValue();
            if (fieldStructure.isEnum()) {
                simpleValue = convertEnumValue(fieldStructure, simpleValue);
            }
            // TODO may be place its logic into the MultiConverter
            if (fieldStructure.getJavaType() == JAVA_LANG_BOOLEAN) {
                return BooleanUtils.toBooleanObject(simpleValue);
            }
            return MultiConverter.convert(simpleValue,
                        Class.forName(fieldStructure.getJavaType().value()));
        } catch (ClassNotFoundException e) {
            logger.error("Could not convert {} value", value, e);
            throw new RuntimeException(e);
        }
    }

    private String convertEnumValue(IFieldStructure fieldStructure, String value) {
        for(Entry<String, IAttributeStructure> enumEntry : fieldStructure.getValues().entrySet()) {
            String enumValue = enumEntry.getValue().getValue();
            if (enumEntry.getKey().equals(value) || enumValue.equals(value)) {
                return enumValue;
            }
        }
        throw new UnknownEnumException(fieldStructure.getName(), value, fieldStructure.getNamespace());
    }

    private Object processComplex(Value value, IFieldStructure fieldStructure) {
        if (fieldStructure.isCollection()) {
            checkKind(value, fieldStructure.getName(), KindCase.LIST_VALUE);
            return convertComplexList(value.getListValue(), fieldStructure);
        }
        checkKind(value, fieldStructure.getName(), KindCase.MESSAGE_VALUE);
        return convertByDictionary(value.getMessageValue().getFieldsMap(), fieldStructure);
    }

    private void checkKind(Value value, String fieldName, KindCase expected) {
        if (value.getKindCase() != expected) {
            throw new IllegalArgumentException(String.format("Expected '%s' value but got '%s' for field '%s'", expected, value.getKindCase(), fieldName));
        }
    }

    private List<IMessage> convertComplexList(ListValue listValue, IFieldStructure fieldStructure) {
        List<Value> valuesList = listValue.getValuesList();
        int size = valuesList.size();
        List<IMessage> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Value value = valuesList.get(i);
            try {
                checkKind(value, fieldStructure.getName(), KindCase.MESSAGE_VALUE);
                result.add(convertByDictionary(value.getMessageValue().getFieldsMap(), fieldStructure));
            } catch (RuntimeException e) {
                throw new MessageConvertException("[" + i + "]", e);
            }
        }
        return result;
    }
}
