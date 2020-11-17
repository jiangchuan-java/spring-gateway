package com.ifeng.fhh.gateway.filter;

import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Des: 用于在收到请求以及响应请求时，记录请求的耗时
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Component
public class GlobalMonitorGatewayFilter implements GlobalFilter, Ordered {


    private static final Log LOGGER = LogFactory.getLog(GlobalMonitorGatewayFilter.class);


    private static final Histogram requestLatency = Histogram.build()
            .name("FHH_GATEWAY_REQUEST").labelNames("requestPath").help("Request latency in seconds.").register();


    static {
        //gc, memory pools, classloading, and thread counts.
        DefaultExports.initialize();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().value();
        Histogram.Timer requestTimer = requestLatency.labels(requestPath).startTimer();

        return chain.filter(exchange).then(Mono.fromRunnable(()->{
            requestTimer.observeDuration();
        }));
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
