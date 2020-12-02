package com.ifeng.fhh.gateway.filter.loadbalance_filter.lb_factory;

import com.ifeng.fhh.gateway.filter.loadbalance_filter.LoadbalanceGlobalGatewayFilter;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.instance_discover.AbstractInstanceDiscover;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.lb_algorithm.AbstractLBAlgorithm;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * @Des: 抽象工厂，生产负载均衡选择器及其必要依赖
 *  很多类被注入进来了，但使用哪些，就可以由工厂进行组成装，进而提供实例
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
@Component
public abstract class AbstractLoadBalancerFactory implements ApplicationContextAware {

    /**
     * 负载均衡算法，子类具体实现
     *
     * @return
     */
    protected abstract AbstractLBAlgorithm buildLBAlgorithm();

    /**
     * 实例发现工具，子类具体实现
     *
     * @return
     */
    protected abstract AbstractInstanceDiscover buildInstanceDiscover();

    /**
     * 通过负载均衡算法 +　实例发现工具 = 实例化一个负载均衡器
     *
     * @return
     */
    public ReactorServiceInstanceLoadBalancer buildLoadBalancer() {
        AbstractInstanceDiscover instanceDiscover = this.buildInstanceDiscover();
        AbstractLBAlgorithm lbAlgorithm = this.buildLBAlgorithm();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setInstanceDiscover(instanceDiscover);
        loadBalancer.setLbAlgorithm(lbAlgorithm);
        return loadBalancer;
    }


    private static class LoadBalancer implements ReactorServiceInstanceLoadBalancer{



        private AbstractLBAlgorithm lbAlgorithm;


        private AbstractInstanceDiscover instanceDiscover;


        @Override
        public Mono<Response<ServiceInstance>> choose(Request request) {


            LoadbalanceGlobalGatewayFilter.LBRequest lbRequest = null;
            if(request instanceof LoadbalanceGlobalGatewayFilter.LBRequest){
                lbRequest = (LoadbalanceGlobalGatewayFilter.LBRequest) request;
            }
            ServerWebExchange exchange = lbRequest.getExchange();

            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            URI url = route.getUri();
            String host = url.getHost();

            List<ServiceInstance> instanceList = instanceDiscover.getCurrentServiceInstances(host);
            ServiceInstance instance = lbAlgorithm.select(instanceList);

            return Mono.just(new DefaultResponse(instance));
        }


        @Override
        public Mono<Response<ServiceInstance>> choose() {
            return null;
        }

        public AbstractLBAlgorithm getLbAlgorithm() {
            return lbAlgorithm;
        }

        public void setLbAlgorithm(AbstractLBAlgorithm lbAlgorithm) {
            this.lbAlgorithm = lbAlgorithm;
        }

        public AbstractInstanceDiscover getInstanceDiscover() {
            return instanceDiscover;
        }

        public void setInstanceDiscover(AbstractInstanceDiscover instanceDiscover) {
            this.instanceDiscover = instanceDiscover;
        }
    }
}
