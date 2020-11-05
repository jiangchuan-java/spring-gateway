package com.ifeng.fhh.gateway.loadbalance;

import com.ifeng.fhh.gateway.discover.AbstractInstanceDiscover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static com.ifeng.fhh.gateway.filter.GlobalBGatewayFilterFactory.LBRequest;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * @Des: 解析url lb://{servername}, 获取servername
 * servername 作为nacos的servername进行获取实例，然后进行复杂均衡选择一个实例进行使用
 *
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Component
public class LBInstanceChooser implements ReactorServiceInstanceLoadBalancer{


    //负载均衡器
    @Autowired
    private AbstractLoadBalancer loadBalancer;

    //实例发现者
    @Autowired
    private AbstractInstanceDiscover instanceDiscover;



    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {


        LBRequest lbRequest = null;
        if(request instanceof LBRequest){
            lbRequest = (LBRequest) request;
        }
        ServerWebExchange exchange = lbRequest.getExchange();

        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        URI url = route.getUri();
        String host = url.getHost();

        List<ServiceInstance> instanceList = instanceDiscover.getCurrentServiceInstances(host);
        ServiceInstance instance = loadBalancer.select(instanceList);

        return Mono.just(new DefaultResponse(instance));
    }


    @Override
    public Mono<Response<ServiceInstance>> choose() {
        return null;
    }


}
