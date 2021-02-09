package com.ifeng.fhh.gateway.filter.monitor_filter;

import com.ifeng.fhh.gateway.filter.OrderedGlobalFilter;
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

import java.net.URI;
import java.util.function.Consumer;

/**
 * @Des: 用于在收到请求以及响应请求时，记录请求的耗时
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-28
 */
@Component
public class MonitorGlobalGatewayFilter extends OrderedGlobalFilter {


    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorGlobalGatewayFilter.class);

    private static final Histogram requestLatency = Histogram.build()
            .name("FHH_GATEWAY_REQUEST").labelNames("requestPath").help("Request latency in seconds.").register();


    static {
        //gc, memory pools, classloading, and thread counts.
        DefaultExports.initialize();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI uri = exchange.getRequest().getURI();
        String path = uri.getPath();

        Histogram.Timer requestTimer = requestLatency.labels(path).startTimer();

        long begin = System.currentTimeMillis();
        return chain.filter(exchange).doFinally(new Consumer<SignalType>() {
            @Override
            public void accept(SignalType signalType) {
                requestTimer.observeDuration();
                long end = System.currentTimeMillis();
                int statusCode = exchange.getResponse().getStatusCode().value();
                LOGGER.info("uri : {}, statusCode : {}, cos : {}", uri, statusCode, (end-begin));
            }
        });
    }

}
