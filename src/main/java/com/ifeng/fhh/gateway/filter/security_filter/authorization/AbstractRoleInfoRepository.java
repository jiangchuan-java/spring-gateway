package com.ifeng.fhh.gateway.filter.security_filter.authorization;


import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
public abstract class AbstractRoleInfoRepository implements ApplicationEventPublisherAware {


    private ApplicationEventPublisher publisher;

    protected abstract ConcurrentHashMap<String, String> fetchRoleInfoMap(String serviceId);

    public void publishRefreshRoleInfoEvent(String serviceId){
        publisher.publishEvent(new RefreshRoleInfoEvent(this, serviceId));
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}
