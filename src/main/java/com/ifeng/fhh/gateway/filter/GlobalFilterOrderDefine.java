package com.ifeng.fhh.gateway.filter;

import com.ifeng.fhh.gateway.filter.breaker_filter.BreakerGlobalGatewayFilter;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.LoadbalanceGlobalGatewayFilter;
import com.ifeng.fhh.gateway.filter.monitor_filter.MonitorGlobalGatewayFilter;
import com.ifeng.fhh.gateway.filter.security_filter.TokenValidateGlobalGatewayFilter;
import com.ifeng.fhh.gateway.filter.stripPrefix_filter.ServerIdExtractGlobalGatewayFilter;

/**
 * @Des: filter的 order 定义，在这里可以调整filter的整体顺序
 *    注意： filter中必须含有 int order 属性才可以在这里调整顺序
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
public enum GlobalFilterOrderDefine {

    /*
    RemoveCachedBodyFilter - (Integer.MIN_VALUE)
    AdaptCachedBodyGlobalFilter - (Integer.MIN_VALUE + 1000)
    NettyWriteResponseFilter - (-1)
    ForwardPathFilter - (0)
    */
    //prometheus指标采集filter
    MONITOR_GLOBAL_GATEWAYFILTER(MonitorGlobalGatewayFilter.class, 1),
    //提取serverId的filter
    STRIPPREFIX_GLOBAL_GATEWAYFILTER(ServerIdExtractGlobalGatewayFilter.class, 2),
    //安全校验filter
    SECURITY_GLOBAL_GATEWAYFILTER(TokenValidateGlobalGatewayFilter.class, 3),
    /*
    RouteToRequestUrlFilter - (1000)
    */
    //自定义负载均衡filter
    LOADBALANCE_GLOBAL_GATEWAYFILTER(LoadbalanceGlobalGatewayFilter.class, (10150 - 2)),
    //熔断器filter
    BREAKER_GLOBAL_GATEWAYFILTER(BreakerGlobalGatewayFilter.class, (10150 - 1));
    /*
    ReactiveLoadBalancerClientFilter - (10150)
    WebsocketRoutingFilter - (Integer.MAX_VALUE - 1)
    NettyRoutingFilter - (Integer.MAX_VALUE)
    ForwardRoutingFilter - (Integer.MAX_VALUE)
    */

    private Class clazz;

    private int order;

    GlobalFilterOrderDefine(Class clazz, int order) {
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
