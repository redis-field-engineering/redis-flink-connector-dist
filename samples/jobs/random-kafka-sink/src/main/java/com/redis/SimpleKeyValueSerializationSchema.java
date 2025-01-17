package com.redis;

import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nullable;

public class SimpleKeyValueSerializationSchema implements KafkaRecordSerializationSchema<String> {

    @Nullable
    @Override
    public ProducerRecord<byte[], byte[]> serialize(String s, KafkaSinkContext kafkaSinkContext, Long aLong) {
        return new ProducerRecord<>("random-data", s.getBytes(), s.getBytes());
    }
}
