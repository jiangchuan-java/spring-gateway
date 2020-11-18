package com.ifeng.fhh.gateway.filter;

import com.ifeng.fhh.gateway.filter.breaker_filter.BreakerGlobalGatewayFilter;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.LoadbalanceGlobalGatewayFilter;
import com.ifeng.fhh.gateway.filter.monitor_filter.MonitorGlobalGatewayFilter;
import com.ifeng.fhh.gateway.filter.stripPrefix_filter.ServerIdExtractGlobalGatewayFilter;

/**
 * @Des: filter在这里进行配置，并由filterRegister负责注入进spring context中
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
public enum FilterConfiguration {

    //提取serverId的filter
    STRIPPREFIX_GLOBAL_GATEWAYFILTER(ServerIdExtractGlobalGatewayFilter.class, 1),
    //prometheus指标采集filter
    MONITOR_GLOBAL_GATEWAYFILTER(MonitorGlobalGatewayFilter.class, 2),
    //熔断器filter
    BREAKER_GLOBAL_GATEWAYFILTER(BreakerGlobalGatewayFilter.class, 3),
    //自定义负载均衡filter
    LOADBALANCE_GLOBAL_GATEWAYFILTER(LoadbalanceGlobalGatewayFilter.class, (10150 - 1)/*10150是gateway自带lb的filer*/);

    private Class clazz;

    private int order;

    FilterConfiguration(Class clazz, int order) {
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
