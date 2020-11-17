package com.ifeng.fhh.gateway.filter.loadbalance_filter.discover;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
public abstract class AbstractInstanceDiscover {


    /**
     * 返回当前的实例列表
     * @return
     */
    public abstract List<ServiceInstance> getCurrentServiceInstances(String host);
}
