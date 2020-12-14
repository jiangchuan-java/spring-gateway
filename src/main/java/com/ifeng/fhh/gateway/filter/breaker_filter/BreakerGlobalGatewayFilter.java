package com.ifeng.fhh.gateway.filter.breaker_filter;

import com.ifeng.fhh.gateway.filter.OrderedGlobalFilter;
import com.ifeng.fhh.gateway.util.DefaultCircuitBreakerUtil;
import com.ifeng.fhh.gateway.util.GatewayPropertyUtil;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.LoadbalanceGlobalGatewayFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
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
public class BreakerGlobalGatewayFilter extends OrderedGlobalFilter {


    private static final Logger LOGGER = LoggerFactory.getLogger(LoadbalanceGlobalGatewayFilter.class);

    private ConcurrentHashMap<String/*serverId*/, CircuitBreaker/*熔断器*/> breakerMap = new ConcurrentHashMap<>();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String requestPath = exchange.getRequest().getPath().value();

        String serverId = exchange.getAttribute(GatewayPropertyUtil.SERVER_ID);

        CircuitBreaker circuitBreaker = breakerMap.get(serverId);
        if (circuitBreaker == null) {
            circuitBreaker = threadSafeDefaultBreaker(serverId);
        }

        long start = System.nanoTime();
        try {
            circuitBreaker.acquirePermission();
        } catch (CallNotPermittedException e) {
            LOGGER.warn(requestPath + " 熔断!!!!!");
            return Mono.error(new ServiceUnavailableException());
        }

        final CircuitBreaker finalBreaker = circuitBreaker; /*内部类，变量需要提前确定好*/

        return chain.filter(exchange)
                .onErrorResume(throwable -> {
                    LOGGER.error("{} filter error ", serverId, throwable);
                    long durationInNanos = System.nanoTime() - start;
                    finalBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
                    return Mono.error(throwable);
                }).doOnSuccess(Void -> {
                    long durationInNanos = System.nanoTime() - start;
                    finalBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
                });
    }

    /**
     * 线程安全的构建单例对象
     *
     * @param serverId
     * @return
     */
    private CircuitBreaker threadSafeDefaultBreaker(String serverId) {
        CircuitBreaker breaker = breakerMap.get(serverId);
        if (breaker == null) {
            synchronized (String.class) {
                breaker = breakerMap.get(serverId);
                if (breaker == null) {
                    breaker = DefaultCircuitBreakerUtil.buildDefaultBreaker(serverId);
                    breakerMap.put(serverId, breaker);
                    LOGGER.info("init {} breaker", serverId);
                }
            }

        }
        return breaker;

    }

    /**
     * 更新某个业务的breaker
     *
     * @param serverId
     * @param breaker
     */
    public void updateBreakerMap(String serverId, CircuitBreaker breaker) {
        breakerMap.put(serverId, breaker);
    }
}
