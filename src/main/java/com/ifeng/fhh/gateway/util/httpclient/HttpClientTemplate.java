package com.ifeng.fhh.gateway.util.httpclient;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-9-15
 */
public interface HttpClientTemplate {

    /**
     * http get请求
     *
     * @param url url
     * @return 响应
     */
    Mono<String> get(String url);

    /**
     * http get请求
     *
     * @param url     url
     * @param socketTimeout 读取timeout
     * @param connectTimeout 连接timeout
     * @param connectionRequestTimeout 连接池获取连接的timeout
     * @return 响应
     */
    Mono<String> get(String url, Map<String, String> headers, int socketTimeout, int connectTimeout, int connectionRequestTimeout);

    /**
     * http post请求
     *
     * @param url    url
     * @param params 请求体参数
     * @return 响应
     */
    Mono<String> post(String url, String params);

    /**
     * http post请求
     *
     * @param url     url
     * @param params  请求体参数
     * @param headers 请求头
     * @param socketTimeout 读取timeout
     * @param connectTimeout 连接timeout
     * @param connectionRequestTimeout 连接池获取连接的timeout
     * @return 响应
     */
    Mono<String> post(String url, String params, Map<String, String> headers, int socketTimeout, int connectTimeout, int connectionRequestTimeout);
}
