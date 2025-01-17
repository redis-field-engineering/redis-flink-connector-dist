package com.redis;

import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Properties;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws Exception {

        // Set up the Flink execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Generate a stream of random data
        DataStream<String> randomDataStream = env.addSource(new RandomStringSource());

        // Kafka producer properties
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", "kafka:9092");

        // Kafka topic
        String kafkaTopic = "random-data";

        // Build Kafka sink
        KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
                .setBootstrapServers("kafka:9092") // Kafka broker address
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<String>builder()
                                .setTopic(kafkaTopic) // Kafka topic
                                .setKeySerializationSchema(String::getBytes) // Serialize key
                                .setValueSerializationSchema(String::getBytes) // Serialize value
                                .build()
                )
                .build();


        // Add a sink to write the random data to Kafka
        randomDataStream.sinkTo(kafkaSink);

        // Execute the Flink job
        env.execute("Random -> Kafka");
    }

    // Custom source to generate random strings
    private static class RandomStringSource extends org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction<String> {

        private volatile boolean running = true;
        private final Random random = new Random();
        private final String[] sampleWords = {"apple", "banana", "cherry", "date", "elderberry", "fig", "grape"};

        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            while (running) {
                // Generate a random string
                String randomString = sampleWords[random.nextInt(sampleWords.length)] + "-" + random.nextInt(1000);

                // Emit the random string
                ctx.collect(randomString);

                // Sleep for a bit to simulate streaming data
                Thread.sleep(50);
            }
        }

        @Override
        public void cancel() {
            running = false;
        }
    }
}
