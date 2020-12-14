package com.ifeng.fhh.gateway.filter.security_filter.authorization;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ifeng.fhh.gateway.util.DefaultCircuitBreakerUtil;
import com.ifeng.fhh.gateway.util.GatewayPropertyUtil;
import com.ifeng.fhh.gateway.util.httpclient.HttpClientTemplate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * token验证三部曲
 * 1：获取 uri所需的角色id
 * 2：如果有角色id，则获取请求header中的Authorization属性值，请求权限管理系统，获取角色id
 * 3：将返回角色id 与 uri所需的id 进行比对，有相等说明验证通过返回true，无相等返回false
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

    private CircuitBreaker defaultBreaker = DefaultCircuitBreakerUtil.buildDefaultBreaker("token_validate_global_filter");


    @Value("${authority_management_system_url}")
    private String authority_management_system_url;

    /**
     * 验证角色id
     *
     * @param serverId 业务id
     * @param uri 资源路径
     * @param headers header
     * @return true-匹配， false-不匹配
     */
    public Mono<Boolean> validate(String serverId, String uri, HttpHeaders headers) {
        String token = headers.getFirst(GatewayPropertyUtil.AUTHORITY_MANAGEMENT_SYSTEM_TOKEN);
        String roleId = repository.matchRoleId(serverId, uri);
        LOGGER.info("********** matchRoleId serverId : {}, uri : {}, roleId : {}", serverId, uri, roleId);
        if (StringUtils.isEmpty(roleId)) {
            return Mono.just(true);
        } else {
            return checkToken(token, roleId);
        }
    }

    /**
     * 验证header 中 Authorization 属性值
     * 1：判断是否为空
     * 2：非空请求权限服务，获取token对应的roleId列表
     * @param token  Authorization属性值
     * @param roleId uri 预设的roleId
     * @return true-匹配， false-不匹配
     */
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
            defaultBreaker.acquirePermission();
        } catch (CallNotPermittedException e) {
            LOGGER.warn("********** RoleInfoValidator checkToken break!!!!!");
            return Mono.just(false);
        }
        return httpClientTemplate.get(authority_management_system_url, headers, 2000, 500, 500)
                .onErrorResume((Function<Throwable, Mono<String>>) throwable -> {
                    LOGGER.error("{} get roleId from authority system error ",authority_management_system_url, throwable);
                    long durationInNanos = System.nanoTime() - start;
                    defaultBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
                    return Mono.error(throwable);
                }).doOnSuccess(resp -> {
                    long durationInNanos = System.nanoTime() - start;
                    defaultBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
                }).map(resp -> checkRole(resp, roleId));
    }

    /**
     * 验证权限系统返回的 roleId列表与uri设置的权限列表是否有匹配
     * @param resp 权限系统返回的roleId列表
     * @param roleId uri预设的roleId值
     * @return true-匹配， false-不匹配
     */
    private boolean checkRole(String resp, String roleId) {
        try {
            LOGGER.info("********** checkRole resp: {}, roleId : {}", resp, roleId);
            JSONObject jsonObject = JSON.parseObject(resp);
            JSONObject dataJson = jsonObject.getJSONObject("data");
            if (Objects.nonNull(dataJson) && dataJson.containsKey("role")) {
                List<String> list = dataJson.getJSONArray("role").toJavaList(String.class);
                for (String role : list) {
                    if (Objects.equals(role, roleId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e){
            LOGGER.info("********** checkRole resp: {}, roleId : {} error: {}", resp, roleId, e);
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
