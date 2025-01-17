package com.redis;

import com.redis.flink.RedisMessage;
import com.redis.flink.sink.RedisSinkConfig;
import com.redis.flink.sink.RedisPassthroughSerializer;
import com.redis.flink.sink.RedisSinkBuilder;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Main {
    public static void main(String[] args) throws Exception {
        // Set up the Flink execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Create a Kafka source
        KafkaSource<RedisMessage> kafkaSource = KafkaSource.<RedisMessage>builder()
                .setBootstrapServers("kafka:9092") // Replace with your Kafka broker address
                .setTopics("random-data") // Kafka topic
                .setGroupId("flink-consumer-group") // Consumer group ID
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(new ConsumerRecordToRedisMessageDeserializer())
                .build();

        // Add the Kafka source to the Flink environment
        DataStream<RedisMessage> kafkaStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.noWatermarks(), // Replace with your WatermarkStrategy if required
                "KafkaSource"
        );

        RedisSinkConfig sinkConfig = RedisSinkConfig.builder()
                .host("redis")
                .topicName("test")
                .numPartitions(4)
                .build();

        RedisSinkBuilder<RedisMessage> sinkBuilder = new RedisSinkBuilder<>(new RedisPassthroughSerializer(), sinkConfig);

        // Sink the processed data into your custom sink
        kafkaStream.sinkTo(sinkBuilder.build());

        env.enableCheckpointing(50);
        env.setBufferTimeout(50);
        // Execute the Flink job
        env.execute("Kafka to Redis");
    }
}
