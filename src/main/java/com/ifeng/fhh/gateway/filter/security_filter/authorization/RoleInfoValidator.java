package com.ifeng.fhh.gateway.filter.security_filter.authorization;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ifeng.fhh.gateway.util.GatewayPropertyUtil;
import com.ifeng.fhh.gateway.util.httpclient.HttpClientTemplate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
@Component
public class RoleInfoValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleInfoValidator.class);
    @Autowired
    private CompositeRoleInfoRepository repository;

    @Autowired
    private HttpClientTemplate httpClientTemplate;

    private static CircuitBreaker breaker;

    private static final CircuitBreakerConfig defaultBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED) /*固定大小，不做限流就简单点*/
            .slidingWindowSize(10) /*每100次计算一次,如果是时间类型的：单位就是秒*/
            .minimumNumberOfCalls(10) /*最少调用100次才能进行统计*/
            .failureRateThreshold(80) /*80%失败率*/
            .waitDurationInOpenState(Duration.ofSeconds(30)) /*维持熔断状态30秒*/
            .permittedNumberOfCallsInHalfOpenState(20) /*半打开状态下，尝试多少次请求*/
            .build();

    static {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultBreakerConfig);
        breaker = registry.circuitBreaker("RoleInfoValidator");
    }

    @Value("${authority_management_system_url}")
    private String authority_management_system_url;

    public Mono<Boolean> validate(String serverId, String uri, String token) {
        String roleId = repository.matchRoleId(serverId, uri);
        LOGGER.info("********** validate serverId : {}, uri : {}, roleId : {}", serverId, uri, roleId);
        if (Objects.isNull(roleId)) {
            return Mono.just(true);
        } else {
            return checkToken(token, roleId);
        }
    }

    private Mono<Boolean> checkToken(String token, String roleId) {
        LOGGER.info("********** checkToken token: {}, roleId : {}", token, roleId);
        if (Objects.isNull(token)) {
            return Mono.just(false);
        }
        Map<String, String> headers = new HashMap<>();
        headers = new HashMap<>();
        headers.put(GatewayPropertyUtil.AUTHORITY_MANAGEMENT_SYSTEM_TOKEN, token);
        long start = System.nanoTime();

        try {
            breaker.acquirePermission();
        } catch (CallNotPermittedException e) {
            LOGGER.warn("********** checkToken 熔断!!!!!");
            return Mono.error(new ServiceUnavailableException());
        }

        return httpClientTemplate.get(authority_management_system_url, headers, 2000, 500, 500)
                .onErrorResume(new Function<Throwable, Mono<String>>() {
            @Override
            public Mono<String> apply(Throwable throwable) {
                long durationInNanos = System.nanoTime() - start;
                breaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
                LOGGER.error("{} get roleId from authority system error ",authority_management_system_url, throwable);
                return Mono.error(throwable);
            }
        }).doOnSuccess(new Consumer<String>() {
            @Override
            public void accept(String string) {
                long durationInNanos = System.nanoTime() - start;
                breaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
            }
        }).map(resp -> checkRole(resp, roleId));
    }

    private boolean checkRole(String resp, String roleId) {
        LOGGER.info("********** checkRole resp: {}, roleId : {}", resp, roleId);
        JSONObject jsonObject = JSON.parseObject(resp);
        JSONObject dataJson = jsonObject.getJSONObject("data");
        if (dataJson.containsKey("role")) {
            List<String> list = dataJson.getJSONArray("role").toJavaList(String.class);
            for (String role : list) {
                if (Objects.equals(role, roleId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public CompositeRoleInfoRepository getRepository() {
        return repository;
    }

    public void setRepository(CompositeRoleInfoRepository repository) {
        this.repository = repository;
    }

    public HttpClientTemplate getHttpClientTemplate() {
        return httpClientTemplate;
    }

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public String getAuthority_management_system_url() {
        return authority_management_system_url;
    }

    public void setAuthority_management_system_url(String authority_management_system_url) {
        this.authority_management_system_url = authority_management_system_url;
    }
}
