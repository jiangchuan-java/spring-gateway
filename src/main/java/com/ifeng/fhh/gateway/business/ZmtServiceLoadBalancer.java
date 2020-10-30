package com.ifeng.fhh.gateway.business;

import com.ifeng.fhh.gateway.loadbalance.AbstractLoadBalance;
import com.ifeng.fhh.gateway.loadbalance.RandomLoadBalance;
import com.ifeng.fhh.gateway.nacos.NacosServerDiscoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Component
public class ZmtServiceLoadBalancer extends NacosServerDiscoverer implements ReactorServiceInstanceLoadBalancer{


    @Value("${zmt.service.nacos.serverName}")
    private String serverName;

    @Value("${zmt.service.nacos.clusterName}")
    private String clusterName;

    @Value("${zmt.service.nacos.serverAddr}")
    private String serverAddr;

    @Value("${zmt.service.nacos.namespace}")
    private String namespace;

    //负载均衡器
    private AbstractLoadBalance loadBalance = new RandomLoadBalance();



    @PostConstruct
    public void initMethod() throws Exception{
        subscribe(serverName, clusterName, serverAddr, namespace);
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {

        List<ServiceInstance> instanceList = getCurrentServiceInstances();
        ServiceInstance instance = loadBalance.select(instanceList);

        return Mono.just(new DefaultResponse(instance));
    }

    @Override
    public Mono<Response<ServiceInstance>> choose() {
        return null;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String getClusterName() {
        return clusterName;
    }

    @Override
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public String getServerAddr() {
        return serverAddr;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }
}
