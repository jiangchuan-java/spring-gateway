package com.ifeng.fhh.gateway.authority;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    public Mono<Boolean> validate(String serverId, String path, String token){
        String roleId = authorityRepository.matchRoleId(serverId, path);
        if(Objects.isNull(roleId)){
            return Mono.just(true);
        } else {
            return Mono.just(false);
        }
    }

    public AbstractUriAuthorityRepository getAuthorityRepository() {
        return authorityRepository;
    }

    public void setAuthorityRepository(AbstractUriAuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }
}
