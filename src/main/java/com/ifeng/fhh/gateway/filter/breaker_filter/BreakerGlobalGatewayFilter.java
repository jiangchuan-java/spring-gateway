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

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @Des: 全局断路器，根据serverId独立断路器
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Component
public class BreakerGlobalGatewayFilter extends OrderedGlobalFilter {


    private static final Logger LOGGER = LoggerFactory.getLogger(LoadbalanceGlobalGatewayFilter.class);

    private ConcurrentHashMap<String/*serviceId_host*/, CircuitBreaker/*熔断器*/> breakerMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String/*serviceId*/, CircuitBreakerConfig/*熔断器*/> breakerConfigMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);

        String host = url.getHost(); // 负载均衡选择的 实例ip

        String serverId = exchange.getAttribute(GatewayPropertyUtil.SERVER_ID);

        String requestPath = exchange.getRequest().getPath().value();

        String key = buildKey(serverId, host);


        CircuitBreaker circuitBreaker = breakerMap.get(key);
        if (circuitBreaker == null) {
            circuitBreaker = threadSafeDefaultBreaker(serverId, host);
        }

        long start = System.nanoTime();
        try {
            circuitBreaker.acquirePermission();
        } catch (CallNotPermittedException e) {
            LOGGER.warn(requestPath + " break!!!!!");
            return Mono.error(new ServiceUnavailableException());
        }

        final CircuitBreaker finalBreaker = circuitBreaker; /*内部类，变量需要提前确定好*/

        return chain.filter(exchange)
                .onErrorResume(throwable -> {
                    LOGGER.error("{} request error ", url, throwable);
                    long durationInNanos = System.nanoTime() - start;
                    finalBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
                    return Mono.error(throwable);
                }).doOnSuccess(Void -> {
                    long durationInNanos = System.nanoTime() - start;
                    finalBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
                });
    }

    private String buildKey(String serviceId, String host){
        return serviceId+"_"+host;
    }

    /**
     * 线程安全的构建单例对象
     *
     * @return
     */
    private CircuitBreaker threadSafeDefaultBreaker(String serviceId, String host) {
        String key = buildKey(serviceId, host);
        CircuitBreaker breaker = breakerMap.get(key);
        if (breaker == null) {
            synchronized (String.class) {
                breaker = breakerMap.get(key);
                if (breaker == null) {
                    CircuitBreakerConfig breakerConfig = breakerConfigMap.get(serviceId);
                    if(Objects.nonNull(breakerConfig)){
                        breaker = DefaultCircuitBreakerUtil.buildBreaker(key,breakerConfig);
                    } else {
                        breaker = DefaultCircuitBreakerUtil.buildDefaultBreaker(key);
                    }
                    breakerMap.put(key, breaker);
                    LOGGER.info("apply {} breaker", key);
                }
            }

        }
        return breaker;
    }

    /**
     * 更新某个业务的breaker
     *
     * @param serverId
     * @param breakerConfig
     */
    public void updateBreakerMap(String serverId, CircuitBreakerConfig breakerConfig) {
        breakerConfigMap.put(serverId, breakerConfig);
        String prefix = serverId;
        breakerMap.keySet().forEach(key->{
            if(key.startsWith(prefix)){
                CircuitBreaker breaker = DefaultCircuitBreakerUtil.buildBreaker(key,breakerConfig);
                breakerMap.put(key, breaker);
            }
        });
    }
}
