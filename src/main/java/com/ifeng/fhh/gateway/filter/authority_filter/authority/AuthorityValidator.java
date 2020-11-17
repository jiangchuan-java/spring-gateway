package com.ifeng.fhh.gateway.filter.authority_filter.authority;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
public class AuthorityValidator {

    @Autowired
    private AbstractUriAuthorityRepository authorityRepository;

    @Autowired
    private HttpClientTemplate httpClientTemplate;

    @Value("null")
    private String authorUrl;

    public Mono<Boolean> validate(String serverId, String path, String token){
        String roleId = authorityRepository.matchRoleId(serverId, path);
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
        headers.put("Authorization", token);
        return httpClientTemplate.get(authorUrl, headers, 2000,500,500)
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

    public AbstractUriAuthorityRepository getAuthorityRepository() {
        return authorityRepository;
    }

    public void setAuthorityRepository(AbstractUriAuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }
}
