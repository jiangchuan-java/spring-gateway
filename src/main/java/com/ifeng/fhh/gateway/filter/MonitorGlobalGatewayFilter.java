package com.ifeng.fhh.gateway.filter;

import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.function.Consumer;

/**
 * @Des: 用于在收到请求以及响应请求时，记录请求的耗时
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Component
public class MonitorGlobalGatewayFilter implements GlobalFilter, Ordered {


    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorGlobalGatewayFilter.class);


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

        return chain.filter(exchange).doFinally(new Consumer<SignalType>() {
            @Override
            public void accept(SignalType signalType) {
                requestTimer.observeDuration();
                long contentLength = exchange.getResponse().getHeaders().getContentLength();
                LOGGER.info("requestPath : {}, signalType : {}, resp : {}", requestPath, signalType, contentLength);
            }
        });
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
