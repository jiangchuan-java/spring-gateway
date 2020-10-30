package com.ifeng.fhh.gateway;

import com.ifeng.fhh.gateway.global.GlobalBGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Configuration
public class RouteConfiguration {

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Add a simple re-route from: /get to: http://httpbin.org:80
                // Add a simple "Hello:World" HTTP Header
                .route(r-> r.path("/get").filters(f->f.stripPrefix(1))
                        .uri("http://www.baidu.com")) // forward to httpbin
                .build();
    }


    @Bean
    public RouteLocator myLbRoutes(RouteLocatorBuilder builder, GlobalBGatewayFilter globalBGatewayFilter) {
        return builder.routes()
                // Add a simple re-route from: /get to: http://httpbin.org:80
                // Add a simple "Hello:World" HTTP Header
                .route(r-> r.path("/**")
                        .filters(f->f.filters(globalBGatewayFilter).stripPrefix(1))
                        .uri("lb://all")) // forward to httpbin
                .build();
    }
}
