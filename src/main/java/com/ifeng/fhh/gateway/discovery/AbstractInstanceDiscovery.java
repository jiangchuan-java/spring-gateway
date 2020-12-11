package com.ifeng.fhh.gateway.discovery;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Des: 抽象层，与具体实现解耦
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
public abstract class AbstractInstanceDiscovery{

    //不用子类用什么注册中心，这里就是要根据域名获取实例
    private ConcurrentHashMap<String/*host 域名*/, List<ServiceInstance>/*可用实例实例*/> serverInstanceCache = new ConcurrentHashMap<>();
    /**
     * 外部刷新
     * @param host 域名
     */
    public void refreshInstanceCache(String host) {
        List<ServiceInstance> serviceInstances = doRefresh(host);
        if(!CollectionUtils.isEmpty(serviceInstances)){
            serverInstanceCache.put(host, serviceInstances);
        }
    }

    /**
     * 内部刷新
     * @param host
     * @param list
     */
    protected final void internalRefresh(String host, List<ServiceInstance> list) {
        if(!CollectionUtils.isEmpty(list)){
            serverInstanceCache.put(host, list);
        }
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
