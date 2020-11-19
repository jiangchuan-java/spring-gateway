package com.ifeng.fhh.gateway.filter.breaker_filter;

import com.ifeng.fhh.gateway.filter.PropertiesUtil;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.LoadbalanceGlobalGatewayFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
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


    private static final Logger LOGGER = LoggerFactory.getLogger(LoadbalanceGlobalGatewayFilter.class);

    private ConcurrentHashMap<String/*serverId*/, CircuitBreaker/*熔断器*/> breakerMap = new ConcurrentHashMap<>();

    private int order;


    private static final CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED) /*固定大小，不做限流就简单点*/
            .slidingWindowSize(100) /*每100次计算一次,如果是时间类型的：单位就是秒*/
            .minimumNumberOfCalls(100) /*最少调用100次才能进行统计*/
            .failureRateThreshold(80) /*80%失败率*/
            .waitDurationInOpenState(Duration.ofSeconds(3)) /*维持熔断状态3秒*/
            .permittedNumberOfCallsInHalfOpenState(20) /*半打开状态下，尝试多少次请求*/
            .build();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String requestPath = exchange.getRequest().getPath().value();

        String serverId = exchange.getAttribute(PropertiesUtil.SERVER_ID);

        CircuitBreaker circuitBreaker = breakerMap.get(serverId);
        if(circuitBreaker == null){
            circuitBreaker = threadSafeInitBreaker(serverId);
        }

        long start = System.nanoTime();
        try {
            circuitBreaker.acquirePermission();
        } catch (CallNotPermittedException e) {
            LOGGER.warn(requestPath + " 熔断!!!!!");
            return Mono.error(new ServiceUnavailableException());
        }

        final CircuitBreaker finalBreaker = circuitBreaker; /*内部类，变量需要提前确定好*/

        return chain.filter(exchange).onErrorResume(new Function<Throwable, Mono<Void>>() {
            @Override
            public Mono<Void> apply(Throwable throwable) {
                long durationInNanos = System.nanoTime() - start;
                finalBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
                LOGGER.error("{} filter error ",serverId, throwable);
                return Mono.error(throwable);
            }
        }).doOnSuccess(new Consumer<Void>() {
            @Override
            public void accept(Void aVoid) {
                long durationInNanos = System.nanoTime() - start;
                finalBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
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


    /**
     * 线程安全的构建单例对象
     * @param serverId
     * @return
     */
    private CircuitBreaker threadSafeInitBreaker(String serverId){
        CircuitBreaker breaker = breakerMap.get(serverId);
        if(breaker == null){
            synchronized (String.class) {
                breaker = breakerMap.get(serverId);
                if(breaker == null){
                    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(breakerConfig);
                    breaker= registry.circuitBreaker(serverId);
                    breakerMap.put(serverId, breaker);
                    LOGGER.info("init {} breaker", serverId);
                }
            }

        }
        return breaker;

    }
}
