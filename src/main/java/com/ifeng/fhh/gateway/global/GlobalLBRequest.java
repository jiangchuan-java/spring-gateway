package com.ifeng.fhh.gateway.global;


import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-30
 */
public class GlobalLBRequest implements Request {

    private ServerWebExchange exchange;


    public GlobalLBRequest(ServerWebExchange exchange) {
        this.exchange = exchange;
    }

    public ServerWebExchange getExchange() {
        return exchange;
    }

    public void setExchange(ServerWebExchange exchange) {
        this.exchange = exchange;
    }
}
