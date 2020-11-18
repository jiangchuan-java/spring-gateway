package com.ifeng.fhh.gateway.filter.loadbalance_filter;

import com.ifeng.fhh.gateway.filter.loadbalance_filter.loadbalance.LBInstanceChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
public class LoadbalanceGlobalGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadbalanceGlobalGatewayFilter.class);

    private int order;

    @Override
    public int getOrder() {
        return order;
    }



    private ReactorLoadBalancer lBInstanceChooser = new LBInstanceChooser();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        URI url = route.getUri();
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


    public static class Config {
        //Put the configuration properties for your filter here
    }


    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }

    private Mono<Response<ServiceInstance>> choose(ServerWebExchange exchange) {

        URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (lBInstanceChooser == null) {
            throw new NotFoundException("No loadbalancer available for " + uri.getHost());
        }
        return lBInstanceChooser.choose(createRequest(exchange));
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

    public ReactorLoadBalancer getlBInstanceChooser() {
        return lBInstanceChooser;
    }

    public void setlBInstanceChooser(ReactorLoadBalancer lBInstanceChooser) {
        this.lBInstanceChooser = lBInstanceChooser;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
