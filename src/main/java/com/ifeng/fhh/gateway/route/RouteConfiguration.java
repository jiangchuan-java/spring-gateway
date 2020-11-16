package com.ifeng.fhh.gateway.route;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
                .route(r-> r.path("/zmt-service/**")
                        .filters(f->f.stripPrefix(1).filters(new GatewayFilter() {
                            @Override
                            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                                System.out.println(1);
                                return chain.filter(exchange);
                            }
                        }))
                        .uri("lb://zmt-service")) // forward to httpbin
                .build();
    }

    @Bean
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