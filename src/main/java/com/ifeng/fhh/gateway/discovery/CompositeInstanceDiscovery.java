package com.ifeng.fhh.gateway.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 聚合注册中心，使多种注册中心可以共存，且解耦
 *
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-10
 */
@Component
public class CompositeInstanceDiscovery implements ApplicationContextAware, ApplicationListener<RefreshInstancesEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeInstanceDiscovery.class);

    private ApplicationContext applicationContext;

    private List<AbstractInstanceDiscovery> discoveryList = new ArrayList<>(); //不用线程安全的List，由spring Init bean时保证线程安全

    @PostConstruct
    public void lookforDiscovery(){
        Map<String, AbstractInstanceDiscovery> beansOfType = applicationContext.getBeansOfType(AbstractInstanceDiscovery.class);
        for(AbstractInstanceDiscovery discovery : beansOfType.values()){
            discoveryList.add(discovery);
        }
    }

    /**
     * 返回当前的实例列表
     * @return
     */
    public List<ServiceInstance> getCurrentServiceInstances(String host) {
        List<ServiceInstance> serverInstanceList = null;
        for(AbstractInstanceDiscovery discovery : discoveryList){
            serverInstanceList = discovery.getCurrentServiceInstances(host);
            if(!CollectionUtils.isEmpty(serverInstanceList)){
                break;
            }
        }
        return serverInstanceList;
    }


    /**
     * 监听路由定义刷新事件
     * @param event
     */
    @Override
    public void onApplicationEvent(RefreshInstancesEvent event) {
        String host = event.getHost();
        LOGGER.info("********** Listen RefreshInstancesEvent host : {} **********", host);
        discoveryList.forEach(discovery->{
            discovery.refreshInstanceCache(host);
        });
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
