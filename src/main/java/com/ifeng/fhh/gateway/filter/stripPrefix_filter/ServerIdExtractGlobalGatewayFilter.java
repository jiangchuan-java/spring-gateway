package com.ifeng.fhh.gateway.filter.stripPrefix_filter;

import com.ifeng.fhh.gateway.filter.OrderedGlobalFilter;
import com.ifeng.fhh.gateway.util.GatewayPropertyUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
@Component
public class ServerIdExtractGlobalGatewayFilter extends OrderedGlobalFilter {


    private static final int DEFAULT_PART = 1; /*第一个uri作为serverId*/


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        addOriginalRequestUrl(exchange, request.getURI());
        String path = request.getURI().getRawPath();

        String serverId = Arrays.stream(StringUtils.tokenizeToStringArray(path, "/"))
                .limit(DEFAULT_PART).collect(Collectors.joining("/"));

        String newPath = "/"
                + Arrays.stream(StringUtils.tokenizeToStringArray(path, "/"))
                .skip(DEFAULT_PART).collect(Collectors.joining("/"));
        newPath += (newPath.length() > 1 && path.endsWith("/") ? "/" : "");
        ServerHttpRequest newRequest = request.mutate().path(newPath).build();

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR,
                newRequest.getURI());
        exchange.getAttributes().put(GatewayPropertyUtil.SERVER_ID, serverId);

        return chain.filter(exchange.mutate().request(newRequest).build());
    }
}
