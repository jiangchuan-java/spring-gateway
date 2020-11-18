package com.ifeng.fhh.gateway.filter.breaker_filter;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @Des: 全局断路器，根据serverId独立断路器
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
public class BreakerGlobalGatewayFilter implements GlobalFilter, Ordered {


    private static final Log LOGGER = LogFactory.getLog(BreakerGlobalGatewayFilter.class);

    private int order;


    CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED) /*固定大小，不做限流就简单点*/
            .slidingWindowSize(100) /*每100次计算一次,如果是时间类型的：单位就是秒*/
            .minimumNumberOfCalls(100) /*最少调用100次才能进行统计*/
            .failureRateThreshold(80) /*80%失败率*/
            .waitDurationInOpenState(Duration.ofSeconds(3)) /*维持熔断状态3秒*/
            .permittedNumberOfCallsInHalfOpenState(20) /*半打开状态下，尝试多少次请求*/
            .build();

    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(breakerConfig);
    CircuitBreaker circuitBreaker = registry.circuitBreaker("api-breaker");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String requestPath = exchange.getRequest().getPath().value();

        long start = System.nanoTime();
        try {
            circuitBreaker.acquirePermission();
        } catch (CallNotPermittedException e) {
            LOGGER.warn(requestPath + " 熔断!!!!!");
            return Mono.error(new ServiceUnavailableException());
        }

        return chain.filter(exchange).onErrorResume(new Function<Throwable, Mono<Void>>() {
            @Override
            public Mono<Void> apply(Throwable throwable) {
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
                return Mono.error(new ServiceUnavailableException());
            }
        }).doOnSuccess(new Consumer<Void>() {
            @Override
            public void accept(Void aVoid) {
                long durationInNanos = System.nanoTime() - start;
                circuitBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
            }
        });
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
