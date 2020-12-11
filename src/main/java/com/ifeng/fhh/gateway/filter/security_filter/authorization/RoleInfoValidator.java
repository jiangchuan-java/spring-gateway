package com.ifeng.fhh.gateway.filter.security_filter.authorization;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ifeng.fhh.gateway.util.GatewayPropertyUtil;
import com.ifeng.fhh.gateway.util.httpclient.HttpClientTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
@Component
public class RoleInfoValidator {

    @Autowired
    private CompositeRoleInfoRepository repository;

    @Autowired
    private HttpClientTemplate httpClientTemplate;

    @Value("${authority_management_system_url}")
    private String authority_management_system_url;

    public Mono<Boolean> validate(String serverId, String uri, String token){
        String roleId = repository.matchRoleId(serverId, uri);
        if(Objects.isNull(roleId)){
            return Mono.just(true);
        } else {
            return checkToken(token, roleId);
        }
    }

    private Mono<Boolean> checkToken(String token, String roleId) {
        if(Objects.isNull(token)){
            return Mono.just(false);
        }
        Map<String, String> headers = new HashMap<>();
        headers = new HashMap<>();
        headers.put(GatewayPropertyUtil.AUTHORITY_MANAGEMENT_SYSTEM_TOKEN, token);
        return httpClientTemplate.get(authority_management_system_url, headers, 2000,500,500)
                .map(resp -> checkRole(resp, roleId));
    }

    private boolean checkRole(String resp, String roleId) {
        JSONObject jsonObject = JSON.parseObject(resp);
        JSONObject dataJson = jsonObject.getJSONObject("data");
        if (dataJson.containsKey("role")) {
            List<String> list =  dataJson.getJSONArray("role").toJavaList(String.class);
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
