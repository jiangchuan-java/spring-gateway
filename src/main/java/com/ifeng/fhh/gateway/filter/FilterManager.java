package com.ifeng.fhh.gateway.filter;

import com.ifeng.fhh.gateway.filter.loadbalance_filter.LoadbalanceGlobalGatewayFilter;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
public enum FilterManager {

    //prometheus指标采集filter
    MONITOR_GLOBAL_GATEWAYFILTER(MonitorGlobalGatewayFilter.class, 1),
    //熔断器filter
    BREAKER_GLOBAL_GATEWAYFILTER(BreakerGlobalGatewayFilter.class, 2),
    //自定义负载均衡filter
    LOADBALANCE_GLOBAL_GATEWAYFILTER(LoadbalanceGlobalGatewayFilter.class, (10150 - 1)/*10150是gateway自带lb的filer*/);

    private Class clazz;

    private int order;

    FilterManager(Class clazz, int order) {
        this.clazz = clazz;
        this.order = order;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }}
