package com.ifeng.fhh.gateway.discovery;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Des: 注册中心解耦层，使业务与具体注册中心实现解耦
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
public abstract class AbstractInstanceDiscovery {

    private ConcurrentHashMap<String/*host 域名*/, List<ServiceInstance>/*可用实例实例*/> serverInstanceCache = new ConcurrentHashMap<>();
    /**
     * 外部刷新
     * @param host 域名
     */
    public void refreshInstanceCache(String host) {
        List<ServiceInstance> serviceInstances = doRefresh(host);
        serverInstanceCache.put(host, serviceInstances);
    }

    /**
     * 内部刷新
     * @param host
     * @param list
     */
    protected final void internalRefresh(String host, List<ServiceInstance> list) {
        serverInstanceCache.put(host, list);
    }

    protected abstract List<ServiceInstance> doRefresh(String host);

    /**
     * 获取实例
     * @param host
     * @return
     */
    public List<ServiceInstance> getCurrentServiceInstances(String host) {
        List<ServiceInstance> serverInstanceList = serverInstanceCache.get(host);
        return serverInstanceList;
    }

}
