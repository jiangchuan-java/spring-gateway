package com.ifeng.fhh.gateway.filter.mirror_filter;

import com.ifeng.fhh.gateway.filter.OrderedGlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-10
 */
public class MirrorRequestGlobalGatewayFilter extends OrderedGlobalFilter {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return null;
    }
}
