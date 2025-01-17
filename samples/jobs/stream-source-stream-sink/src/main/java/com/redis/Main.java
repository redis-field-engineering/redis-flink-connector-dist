package com.redis;

import com.redis.flink.RedisMessage;
import com.redis.flink.sink.RedisSinkConfig;
import com.redis.flink.sink.RedisPassthroughSerializer;
import com.redis.flink.sink.RedisSink;
import com.redis.flink.sink.RedisSinkBuilder;
import com.redis.flink.source.basic.RedisStreamSource;
import com.redis.flink.source.basic.RedisStreamSourceBuilder;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;

import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception{
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Configuration globalConfig = new Configuration();
        System.out.println("Setting global config");

        globalConfig.setString("redis.host", "redis");
        globalConfig.setInteger("redis.port", 6379);

        JedisPooled jedis = new JedisPooled("redis", 6379);
        jedis.unlink("test:0");

        jedis.xgroupCreate("test:0", "test-group", StreamEntryID.XGROUP_LAST_ENTRY, true);
        jedis.xadd("test:0", StreamEntryID.NEW_ENTRY, Map.of("key", "value"));


        env.getConfig().setGlobalJobParameters(globalConfig);
        env.enableCheckpointing(10);

        RedisStreamSourceBuilder<Map<String,String>> builder = new RedisStreamSourceBuilder<>("redis", 6379, "test", "test-group");

        RedisStreamSource<Map<String,String>> source = builder.build();
        TypeInformation<Map<String,String>> typeInfo = TypeInformation.of(new TypeHint<Map<String, String>>() {
        });
        DataStreamSource<Map<String,String>> redisStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Redis Stream Source", typeInfo);

        RedisSinkConfig sinkConfig = RedisSinkConfig.builder().host("redis").topicName("test").numPartitions(1).build();
        RedisSink<RedisMessage> sink = new RedisSinkBuilder<>(new RedisPassthroughSerializer(), sinkConfig).build();

//        redisStream
//                .sinkTo(sink);

        env.execute("Flink Redis Stream Source");
    }


    public static final class RedisSourceTokenizer extends RichFlatMapFunction<Map<String,String>, Integer>{

        private transient JedisPooled jedis;

        @Override
        public void flatMap(Map<String,String> value, Collector<Integer> collector) throws Exception {
            jedis.incr("count");
            if(value != null){
                jedis.xadd("test:0", StreamEntryID.NEW_ENTRY, value);
            }
        }

        @Override
        public void open(Configuration parameters) {
            ExecutionConfig.GlobalJobParameters globalParams = getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
            Map<String, String> paramMap = globalParams.toMap();

            jedis = new JedisPooled(paramMap.get("redis.host"), Integer.parseInt(paramMap.get( "redis.port")));
        }
    }
}
