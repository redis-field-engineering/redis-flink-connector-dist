package com.redis;

import com.redis.flink.RedisMessage;
import com.redis.flink.sink.RedisSinkConfig;
import com.redis.flink.sink.RedisPassthroughSerializer;
import com.redis.flink.sink.RedisSinkBuilder;
import com.redis.flink.source.partitioned.RedisSourceBuilder;
import com.redis.flink.source.partitioned.RedisSourceConfig;
import com.redis.flink.source.partitioned.reader.deserializer.RedisMessageDeserializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;

import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {


        Configuration globalConfig = new Configuration();
        System.out.println("Setting global config");

        globalConfig.setString("redis.host", "redis");
        globalConfig.setInteger("redis.port", 6379);

        JedisPooled jedis = new JedisPooled("redis", 6379);
        jedis.unlink("test:0");
        jedis.unlink("test:1");
        jedis.unlink("test:2");
        jedis.unlink("test:3");

        RedisSinkConfig sinkConfig = RedisSinkConfig.builder()
                .host("redis")
                .topicName("output")
                .numPartitions(4)
                .build();

        RedisSourceConfig sourceConfig = RedisSourceConfig.builder()
                .host("redis")
                .port(6379)
                .consumerGroup("test-group")
                .topicName("test")
                .numPartitions(4)
                .build();


        RedisSourceBuilder<RedisMessage> sourceBuilder = new RedisSourceBuilder<>(sourceConfig, new RedisMessageDeserializer());
        RedisSinkBuilder<RedisMessage> sinkBuilder = new RedisSinkBuilder<>(new RedisPassthroughSerializer(), sinkConfig);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(globalConfig);
        env.enableCheckpointing(5000);
        env.setParallelism(4);
        TypeInformation<RedisMessage> typeInfo = TypeInformation.of(RedisMessage.class);
        String sourceName = "Redis to Redis";
        env.fromSource(sourceBuilder.build(), WatermarkStrategy.noWatermarks(), sourceName, typeInfo).sinkTo(sinkBuilder.build());

        env.execute(sourceName);
    }
}
