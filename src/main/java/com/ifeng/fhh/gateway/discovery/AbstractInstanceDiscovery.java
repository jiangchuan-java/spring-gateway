package com.ifeng.fhh.gateway.discovery;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Des: 抽象层，与具体实现解耦
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
public abstract class AbstractInstanceDiscovery implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;

    //不用子类用什么注册中心，这里就是要根据域名获取实例
    private ConcurrentHashMap<String/*host 域名*/, List<ServiceInstance>/*可用实例实例*/> serverInstanceCache = new ConcurrentHashMap<>();


    public void publishRefreshInstanceEvent(String host){
        publisher.publishEvent(new RefreshInstancesEvent(this, host));
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    protected abstract List<ServiceInstance> fetchServiceList(String host);


}
