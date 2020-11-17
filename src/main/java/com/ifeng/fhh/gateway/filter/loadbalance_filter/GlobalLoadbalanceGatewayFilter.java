package com.ifeng.fhh.gateway.filter.loadbalance_filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
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
@Component
public class GlobalLoadbalanceGatewayFilter implements GlobalFilter, Ordered {


    private static final Log log = LogFactory
            .getLog(GlobalLoadbalanceGatewayFilter.class);

    @Override
    public int getOrder() {
        return 10150 - 1;
    }


    @Autowired
    private ReactorLoadBalancer<ServiceInstance> lBInstanceChooser;


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

        if (log.isTraceEnabled()) {
            log.trace(ReactiveLoadBalancerClientFilter.class.getSimpleName()
                    + " url before: " + url);
        }

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

            if (log.isTraceEnabled()) {
                log.trace("LoadBalancerClientFilter url chosen: " + requestUrl);
            }
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
            long end = System.currentTimeMillis();
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

    public ReactorLoadBalancer<ServiceInstance> getlBInstanceChooser() {
        return lBInstanceChooser;
    }

    public void setlBInstanceChooser(ReactorLoadBalancer<ServiceInstance> lBInstanceChooser) {
        this.lBInstanceChooser = lBInstanceChooser;
    }
}
