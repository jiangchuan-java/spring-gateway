package com.ifeng.fhh.gateway.global;

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
import org.springframework.web.server.ServerWebExchange;
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
public class GloabllLoadBalancer extends NacosServerDiscoverer implements ReactorServiceInstanceLoadBalancer{




    @Value("${nacos.serverAddr}")
    private String serverAddr;

    @Value("${nacos.namespace.gateway}")
    private String namespace;

    //负载均衡器
    private AbstractLoadBalance loadBalance = new RandomLoadBalance();



    @PostConstruct
    public void initMethod() throws Exception{
        subscribe(serverName, clusterName, serverAddr, namespace);
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {

        GlobalLBRequest lbRequest = null;
        if(request instanceof GlobalLBRequest){
            lbRequest = (GlobalLBRequest) request;
        }
        ServerWebExchange exchange = lbRequest.getExchange();


        List<ServiceInstance> instanceList = getCurrentServiceInstances();
        ServiceInstance instance = loadBalance.select(instanceList);

        return Mono.just(new DefaultResponse(instance));
    }

    @Override
    public Mono<Response<ServiceInstance>> choose() {
        return null;
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
