package com.redis;

import com.redis.flink.RedisMessage;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.slf4j.Logger;

public class FailoverSimulatingMapFunction<T> extends RichMapFunction<T, T> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FailoverSimulatingMapFunction.class);
    private int counter = 0;
    private static final int numSuccessesBetweenFailover = 250;

    @Override
    public T map(T t) throws Exception {
        counter++;
        if(counter % numSuccessesBetweenFailover == 0){
            throw new RuntimeException("Simulating failover");
        }
        return t;
    }
}