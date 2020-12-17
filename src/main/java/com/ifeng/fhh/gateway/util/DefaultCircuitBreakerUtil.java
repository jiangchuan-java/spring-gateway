package com.ifeng.fhh.gateway.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-14
 */
public class DefaultCircuitBreakerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCircuitBreakerUtil.class);

    private static final CircuitBreakerConfig defaultBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED) /*固定大小，不做限流就简单点*/
            .slidingWindowSize(100) /*每100次计算一次,如果是时间类型的：单位就是秒*/
            .minimumNumberOfCalls(100) /*最少调用100次才能进行统计*/
            .failureRateThreshold(80) /*80%失败率*/
            .waitDurationInOpenState(Duration.ofSeconds(30)) /*维持熔断状态30秒*/
            .permittedNumberOfCallsInHalfOpenState(20) /*半打开状态下，尝试多少次请求*/
            .build();

    public static CircuitBreaker buildDefaultBreaker(String name){
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultBreakerConfig);
        CircuitBreaker breaker = registry.circuitBreaker(name);
        LOGGER.info("************* build breaker : {}, {}",name, breaker);
        return breaker;
    }

    public static CircuitBreaker buildBreaker(String name, CircuitBreakerConfig breakerConfig){
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(breakerConfig);
        CircuitBreaker breaker = registry.circuitBreaker(name);
        LOGGER.info("************ build breaker : {}, {}",name, breaker);
        return breaker;
    }
}
