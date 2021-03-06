package com.ifeng.fhh.gateway.route;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
import static org.springframework.cloud.gateway.support.RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Configuration
public class RouteConfiguration {

    //@Bean
    public RouteLocator myLbRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Add a simple re-route from: /get to: http://httpbin.org:80
                // Add a simple "Hello:World" HTTP Header
                .route("fhh-service",r-> r.path("/fhh-service/**")
                        .uri("http://www.baidu.com").metadata(RESPONSE_TIMEOUT_ATTR,100).metadata(CONNECT_TIMEOUT_ATTR,100)) // forward to httpbin
                .build();
    }

    //@Bean
    public GlobalFilter postGlobalFilter() {
        return new GlobalFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

                return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                    long contentLength = exchange.getResponse().getHeaders().getContentLength();
                    System.out.println(contentLength);
                }));
            }
        };
    }
}
