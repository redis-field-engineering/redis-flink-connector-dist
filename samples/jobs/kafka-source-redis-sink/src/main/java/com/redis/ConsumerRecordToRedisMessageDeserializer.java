package com.redis;

import com.redis.flink.RedisMessage;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConsumerRecordToRedisMessageDeserializer implements KafkaRecordDeserializationSchema<RedisMessage> {
    private static Logger Log = org.slf4j.LoggerFactory.getLogger(ConsumerRecordToRedisMessageDeserializer.class);
    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord, Collector<RedisMessage> collector) throws IOException {
        if(consumerRecord == null){
            throw new IOException("Consumer record is null");
        }
        if(consumerRecord.value() == null){
            throw new IOException("Consumer record value is null");
        }
        if(consumerRecord.key() == null){
            throw new IOException("Consumer record key is null");
        }

        RedisMessage msg = RedisMessage.of(new String(consumerRecord.key()), new HashMap<>(Map.of("data", new String(consumerRecord.value()))));
        collector.collect(msg);
    }

    @Override
    public TypeInformation<RedisMessage> getProducedType() {
        return TypeInformation.of(RedisMessage.class);
    }
}
