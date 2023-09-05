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
package com.exactpro.th2.sailfish.utils.proto;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage;
import com.exactpro.th2.sailfish.utils.MessageWrapper;
import com.exactpro.th2.sailfish.utils.transport.TransportToIMessageConverter;
import com.google.common.collect.ImmutableList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.exactpro.sf.comparison.MessageComparator.compare;
import static com.exactpro.th2.sailfish.utils.transport.TransportToIMessageConverter.getDEFAULT_MESSAGE_FACTORY;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
//@BenchmarkMode({Mode.AverageTime, Mode.SingleShotTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Fork(1)
public class TransportBenchmark {
    public static final Logger LOGGER = LoggerFactory.getLogger(TransportBenchmark.class);

    @State(Scope.Benchmark)
    public static class StateMy {
        public IDictionaryStructure dictionary;

        {
            try {
                dictionary = new XmlDictionaryStructureLoader().
                        load(Files.newInputStream(Path.of("src", "test", "resources", "dictionary.xml")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public TransportToIMessageConverter converter = new TransportToIMessageConverter(getDEFAULT_MESSAGE_FACTORY(), dictionary);
        public ParsedMessage protoMessage = getMessage();
    }

    public static ParsedMessage getMessage() {
        ParsedMessage builder = createMessage();
        builder.getMetadata().put("key", "value");
        //LOGGER.info("getMessage(): \n{}", builder);
        return builder;
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TransportBenchmark.class.getSimpleName()).measurementIterations(10).build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testMethod(StateMy state, Blackhole bh) {
        //bh.consume(convertByDictionaryPositive(state));
        bh.consume(state.converter.fromTransport("book", "session-group", state.protoMessage, true));
    }


    public static ParsedMessage createMessage() {
        ParsedMessage parsedMessage = createMessageBuilder("RootWithNestedComplex");
        Map<String, Object> body = parsedMessage.getBody();
        body.put("string", "StringValue");
        body.put("byte", "0");
        body.put("short", "1");
        body.put("int", "2");
        body.put("long", "3");
        body.put("float", "1.1");
        body.put("double", "2.2");
        body.put("decimal", "3.3");
        body.put("char", "A");
        body.put("bool", "true");
        body.put("boolY", "Y");
        body.put("boolN", "n");
        body.put("enumInt", "MINUS_ONE");
        body.put("complex", Map.of(
                "field1", "field1",
                "field2", "field2"
        ));
        body.put("complexList", Map.of("list", getComplexList()));
        return parsedMessage;
    }

    public static List<Map<String, Object>> getComplexList() {
        return List.of(
                Map.of(
                        "field1", "field1",
                        "field2", "field2"
                ),
                Map.of(
                        "field1", "field3",
                        "field2", "field4"
                )
        );
    }

    public static ParsedMessage createMessageBuilder(String messageType) {
        ParsedMessage.FromMapBuilder parsedMessage = ParsedMessage.builder();
        parsedMessage.setType(messageType);
        return parsedMessage.build();
    }

    private MessageWrapper convertByDictionaryPositive(StateMy state) {
        MessageWrapper actualIMessage = state.converter.fromTransport("book", "session-group", state.protoMessage, true);
        MessageWrapper expectedIMessage = createExpectedIMessage(state);
        ComparisonResult comparisonResult = compare(expectedIMessage, actualIMessage, new ComparatorSettings());
        //LOGGER.info("Message comparison result: \n{}", comparisonResult);
        return actualIMessage;
    }

    private MessageWrapper createExpectedIMessage(StateMy state) {
        IMessage message = getDEFAULT_MESSAGE_FACTORY().createMessage("RootWithNestedComplex", state.dictionary.getNamespace());
        message.addField("string", "StringValue");
        message.addField("byte", (byte) 0);
        message.addField("short", (short) 1);
        message.addField("int", 2);
        message.addField("long", (long) 3);
        message.addField("float", 1.1f);
        message.addField("double", 2.2);
        message.addField("decimal", new BigDecimal("3.3"));
        message.addField("char", 'A');
        message.addField("bool", true);
        message.addField("boolY", true);
        message.addField("boolN", false);
        message.addField("enumInt", -1);
        IMessage nestedComplex = getDEFAULT_MESSAGE_FACTORY().createMessage("SubMessage", state.dictionary.getNamespace());
        nestedComplex.addField("field1", "field1");
        nestedComplex.addField("field2", "field2");
        IMessage nestedComplexSecond = getDEFAULT_MESSAGE_FACTORY().createMessage("SubMessage", state.dictionary.getNamespace());
        nestedComplexSecond.addField("field1", "field3");
        nestedComplexSecond.addField("field2", "field4");
        message.addField("complex", nestedComplex);
        IMessage nestedComplexList = getDEFAULT_MESSAGE_FACTORY().createMessage("SubComplexList", state.dictionary.getNamespace());
        nestedComplexList.addField("list", ImmutableList.of(nestedComplex, nestedComplexSecond));
        message.addField("complexList", nestedComplexList);
        return new MessageWrapper(message);
    }

}
