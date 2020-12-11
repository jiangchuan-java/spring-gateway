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
import java.util.concurrent.ConcurrentHashMap;

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

    //不用子类用什么注册中心，这里就是要根据域名获取实例
    private ConcurrentHashMap<String/*host 域名*/, List<ServiceInstance>/*可用实例实例*/> serverInstanceCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void lookforDiscovery() {
        Map<String, AbstractInstanceDiscovery> beansOfType = applicationContext.getBeansOfType(AbstractInstanceDiscovery.class);
        for (AbstractInstanceDiscovery discovery : beansOfType.values()) {
            discoveryList.add(discovery);
        }
    }

    /**
     * 返回当前的实例列表
     *
     * @return
     */
    public List<ServiceInstance> getCurrentServiceInstances(String host) {
        List<ServiceInstance> serverInstanceList = serverInstanceCache.get(host);
        return serverInstanceList;
    }

    /**
     * 从所有的实例仓库中获取到最新的实例列表
     * @param host
     */
    private synchronized void fetchServiceList(String host) {
        discoveryList.forEach(discovery -> {
            List<ServiceInstance> newInstanceList = discovery.fetchServiceList(host);
            if(!CollectionUtils.isEmpty(newInstanceList)){
                List<ServiceInstance> instanceList = serverInstanceCache.get(host);
                if(CollectionUtils.isEmpty(instanceList)){
                    instanceList = new LinkedList<>();
                }
                LOGGER.info("********** RefreshInstancesEvent host : {}, size : {}", host, instanceList.size());
                instanceList.addAll(instanceList);
            }
        });
    }


    /**
     * 监听路由定义刷新事件
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(RefreshInstancesEvent event) {
        RefreshInstancesEvent refreshInstancesEvent = (RefreshInstancesEvent) event;
        String host = refreshInstancesEvent.getHost();
        fetchServiceList(host);
        LOGGER.info("********** RefreshInstancesEvent host : {}", host);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
