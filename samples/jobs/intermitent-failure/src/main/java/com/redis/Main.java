package com.redis;

import com.redis.flink.RedisMessage;
import com.redis.flink.sink.RedisPassthroughSerializer;
import com.redis.flink.sink.RedisSinkBuilder;
import com.redis.flink.sink.RedisSinkConfig;
import com.redis.flink.source.partitioned.RedisSourceBuilder;
import com.redis.flink.source.partitioned.RedisSourceConfig;
import com.redis.flink.source.partitioned.reader.deserializer.RedisMessageDeserializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestartStrategyOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Configuration config = new Configuration();
        System.out.println("Setting global config");

        env.setBufferTimeout(500);

        config.setString("redis.host", "redis");
        config.setInteger("redis.port", 6379);
        config.set(RestartStrategyOptions.RESTART_STRATEGY, "fixed-delay");
        config.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS, 10000);
        config.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_DELAY, Duration.ofSeconds(3));

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


        RedisSourceBuilder<RedisMessage> builder = new RedisSourceBuilder<>(sourceConfig, new RedisMessageDeserializer());
        RedisSinkBuilder<RedisMessage> sinkBuilder = new RedisSinkBuilder<>(new RedisPassthroughSerializer(), sinkConfig);

        env.getConfig().setGlobalJobParameters(config);
        env.enableCheckpointing(500);
        env.setParallelism(4);
        TypeInformation<RedisMessage> typeInfo = TypeInformation.of(RedisMessage.class);
        String sourceName = "Redis to Redis";
        env
                .fromSource(builder.build(), WatermarkStrategy.noWatermarks(), sourceName, typeInfo)
                .map(new FailoverSimulatingMapFunction<>())
                .sinkTo(sinkBuilder.build());

        env.execute(sourceName);
    }
}
