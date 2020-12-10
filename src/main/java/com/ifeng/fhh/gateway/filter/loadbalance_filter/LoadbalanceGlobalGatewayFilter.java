package com.ifeng.fhh.gateway.filter.loadbalance_filter;

import com.ifeng.fhh.gateway.filter.OrderedGlobalFilter;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.lb_factory.AbstractLoadBalancerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @Des: 用于替代ReactiveLoadBalancerClientFilter, 所以排序是 10150 - 1 的逻辑
 * 之所以替换是因为原本的filter不是很完善，等后续完善了在使用吧
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Component
public class LoadbalanceGlobalGatewayFilter extends OrderedGlobalFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadbalanceGlobalGatewayFilter.class);

    @Resource(name = "randomLoadBalancerFactory")
    private AbstractLoadBalancerFactory abstractLoadBalancerFactory;


    private ReactorServiceInstanceLoadBalancer loadBalancer;

    @PostConstruct
    public void initLoadBalancer(){
        this.loadBalancer = abstractLoadBalancerFactory.buildLoadBalancer(); /*从工厂获取实例*/
        LOGGER.info("init loadBalancer {} ", loadBalancer);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        if (url == null
                || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) {
            return chain.filter(exchange);
        }
        // preserve the original url
        addOriginalRequestUrl(exchange, url);


        return choose(exchange).doOnNext(serverInstance -> {

            if (!serverInstance.hasServer()) {
                throw NotFoundException.create(true,
                        "Unable to find instance for " + url.getHost());
            }

            URI uri = exchange.getRequest().getURI();

            // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
            // if the loadbalancer doesn't provide one.
            String overrideScheme = null;
            if (schemePrefix != null) {
                overrideScheme = url.getScheme();
            }

            DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance(
                    serverInstance.getServer(), overrideScheme);

            URI requestUrl = reconstructURI(serviceInstance, uri);

            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
        }).then(chain.filter(exchange));

    }


    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }

    private Mono<Response<ServiceInstance>> choose(ServerWebExchange exchange) {

        URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (loadBalancer == null) {
            throw new NotFoundException("No loadbalancer available for " + uri.getHost());
        }
        return loadBalancer.choose(createRequest(exchange));
    }

    private Request createRequest(ServerWebExchange exchange) {
        return new LBRequest(exchange);
    }

    public static class LBRequest implements Request {

        private ServerWebExchange exchange;


        public LBRequest(ServerWebExchange exchange) {
            this.exchange = exchange;
        }

        public ServerWebExchange getExchange() {
            return exchange;
        }

        public void setExchange(ServerWebExchange exchange) {
            this.exchange = exchange;
        }
    }

    public AbstractLoadBalancerFactory getAbstractLoadBalancerFactory() {
        return abstractLoadBalancerFactory;
    }

    public void setAbstractLoadBalancerFactory(AbstractLoadBalancerFactory abstractLoadBalancerFactory) {
        this.abstractLoadBalancerFactory = abstractLoadBalancerFactory;
    }
}
