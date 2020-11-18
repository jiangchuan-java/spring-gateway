package com.ifeng.fhh.gateway.filter;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscription;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

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
@Component
public class BreakerGlobalGatewayFilter implements GlobalFilter, Ordered {


    private static final Log LOGGER = LogFactory.getLog(BreakerGlobalGatewayFilter.class);

    CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(4) // 断路器关闭状态下队列的大小，队列满了之后，触发失败比例的计算
            .failureRateThreshold(50) //失败比例
            .waitDurationInOpenState(Duration.ofSeconds(5)) //断路器开启后保持多久
            .ringBufferSizeInHalfOpenState(2) //在试探时的队列大小，满了计算失败比例
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
        return 2;
    }
}
