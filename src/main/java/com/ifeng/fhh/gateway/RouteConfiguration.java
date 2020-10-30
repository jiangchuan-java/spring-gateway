package com.ifeng.fhh.gateway;

import com.ifeng.fhh.gateway.business.ZmtServiceLBGatewayFilter;
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
    public RouteLocator myLbRoutes(RouteLocatorBuilder builder, ZmtServiceLBGatewayFilter gatewayFilter) {
        return builder.routes()
                // Add a simple re-route from: /get to: http://httpbin.org:80
                // Add a simple "Hello:World" HTTP Header
                .route(r-> r.path("/zmt-service/**").filters(f->f.stripPrefix(1).filters(gatewayFilter))
                        .uri("lb://zmt-service")) // forward to httpbin
                .build();
    }
}
