/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.th2.common.grpc.*;
import com.google.common.collect.ImmutableList;
import org.openjdk.jmh.annotations.*;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static com.exactpro.sf.comparison.MessageComparator.compare;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
//@BenchmarkMode({Mode.AverageTime, Mode.SingleShotTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Fork(1)
public class SampleBenchmark {
    public static final Logger LOGGER = LoggerFactory.getLogger(SampleBenchmark.class);

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class StateMy{
        public IDictionaryStructure dictionary;
        { try { dictionary = new XmlDictionaryStructureLoader().
                load(Files.newInputStream(Path.of("src", "test", "resources", "dictionary.xml")));
        } catch (IOException e) { e.printStackTrace(); } }
        public SailfishURI dictionaryURI = SailfishURI.unsafeParse(dictionary.getNamespace());
        public ProtoToIMessageConverter converter = new ProtoToIMessageConverter
                (new DefaultMessageFactoryProxy(), dictionary, dictionaryURI);
        public Message protoMessage = getMessage();
    }

    public static Message getMessage() {
        Message.Builder builder = createMessage();
        builder.getMetadataBuilder().putAllProperties(Map.of("key", "value"));
        //LOGGER.info("getMessage(): \n{}", builder);
        return builder.build();
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SampleBenchmark.class.getSimpleName()).measurementIterations(10).build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testMethod(StateMy state, Blackhole bh){
        //bh.consume(convertByDictionaryPositive(state));
        bh.consume(state.converter.fromProtoMessage(state.protoMessage, true));
    }


    public static Message.Builder createMessage() {
        return createMessageBuilder("RootWithNestedComplex")
                .putFields("string", getSimpleValue("StringValue"))
                .putFields("byte", getSimpleValue("0"))
                .putFields("short", getSimpleValue("1"))
                .putFields("int", getSimpleValue("2"))
                .putFields("long", getSimpleValue("3"))
                .putFields("float", getSimpleValue("1.1"))
                .putFields("double", getSimpleValue("2.2"))
                .putFields("decimal", getSimpleValue("3.3"))
                .putFields("char", getSimpleValue("A"))
                .putFields("bool", getSimpleValue("true"))
                .putFields("boolY", getSimpleValue("Y"))
                .putFields("boolN", getSimpleValue("n"))
                .putFields("enumInt", getSimpleValue("MINUS_ONE"))
                .putFields("complex", getComplex("SubMessage", Map.of(
                        "field1", "field1",
                        "field2", "field2"
                )))
                .putFields("nullField", Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
                .putFields("complexList", Value.newBuilder().setMessageValue(
                        Message.newBuilder().putFields("list", getComplexList())
                ).build());
    }

    public static Value getComplexList() {
        return Value.newBuilder().setListValue(ListValue.newBuilder()
                .addValues(0, getComplex("SubMessage", Map.of(
                        "field1", "field1",
                        "field2", "field2"
                )))
                .addValues(1, getComplex("SubMessage", Map.of(
                        "field1", "field3",
                        "field2", "field4"
                )))
                .build()
        ).build();
    }

    public static Message.Builder createMessageBuilder(String messageType) {
        return Message.newBuilder()
                .setMetadata(MessageMetadata.newBuilder()
                        .setMessageType(messageType)
                        .build());
    }

    public static Value getComplex(String messageType, Map<String, String> values) {
        Message.Builder messageBuilder = createMessageBuilder(messageType);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            messageBuilder.putFields(entry.getKey(), getSimpleValue(entry.getValue()));
        }
        return Value.newBuilder().setMessageValue(messageBuilder).build();
    }

    public static Value getSimpleValue(String value) {
        return Value.newBuilder().setSimpleValue(value).build();
    }



    private MessageWrapper convertByDictionaryPositive(StateMy state) {
        MessageWrapper actualIMessage = state.converter.fromProtoMessage(state.protoMessage, true);
        MessageWrapper expectedIMessage = createExpectedIMessage(state);
        ComparisonResult comparisonResult = compare(expectedIMessage, actualIMessage, new ComparatorSettings());
        //LOGGER.info("Message comparison result: \n{}", comparisonResult);
        return actualIMessage;
    }

    private MessageWrapper createExpectedIMessage(StateMy state) {
        IMessage message = new DefaultMessageFactoryProxy().createMessage(state.dictionaryURI, "RootWithNestedComplex");
        message.addField("string", "StringValue");
        message.addField("byte", (byte)0);
        message.addField("short", (short)1);
        message.addField("int", 2);
        message.addField("long", (long)3);
        message.addField("float", 1.1f);
        message.addField("double", 2.2);
        message.addField("decimal", new BigDecimal("3.3"));
        message.addField("char", 'A');
        message.addField("bool", true);
        message.addField("boolY", true);
        message.addField("boolN", false);
        message.addField("enumInt", -1);
        IMessage nestedComplex = new DefaultMessageFactoryProxy().createMessage(state.dictionaryURI, "SubMessage");
        nestedComplex.addField("field1", "field1");
        nestedComplex.addField("field2", "field2");
        IMessage nestedComplexSecond = new DefaultMessageFactoryProxy().createMessage(state.dictionaryURI, "SubMessage");
        nestedComplexSecond.addField("field1", "field3");
        nestedComplexSecond.addField("field2", "field4");
        message.addField("complex", nestedComplex);
        IMessage nestedComplexList = new DefaultMessageFactoryProxy().createMessage(state.dictionaryURI, "SubComplexList");
        nestedComplexList.addField("list", ImmutableList.of(nestedComplex, nestedComplexSecond));
        message.addField("complexList", nestedComplexList);
        return new MessageWrapper(message);
    }

}
