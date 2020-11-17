package com.ifeng.fhh.gateway.controller;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Des: 实现 prometheus 的 endpoint逻辑，参考 servelet / vertx 相关实现
 *
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-16
 */
@Controller
@ResponseBody
public class MetricsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsController.class);

    private ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();

    private CollectorRegistry registry = CollectorRegistry.defaultRegistry;



    @RequestMapping("/metrics")
    public Mono<String> testResilience4j(ServerHttpRequest request, ServerHttpResponse response){

        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                final BufferWriter writer = new BufferWriter();
                TextFormat.write004(writer, registry.filteredMetricFamilySamples(parse(request)));
                return writer.bufferToString();
            }catch (Exception e){
                LOGGER.error("bufferToString error", e);
            }
            return "error";

        }, singleThreadPool);

        response.getHeaders().add("Content-Type", TextFormat.CONTENT_TYPE_004);


        return Mono.fromFuture(completableFuture);
    }

    private Set<String> parse(ServerHttpRequest request) {

        List<String> list = request.getQueryParams().get("name[]");
        if(Objects.isNull(list)){
            return new HashSet();
        }
        return new HashSet(list);

    }

    private static class BufferWriter extends Writer {

        StringBuffer buffer = new StringBuffer();

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            buffer.append(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            // NO-OP
        }

        @Override
        public void close() throws IOException {
            // NO-OP
        }

        String bufferToString() {
            return buffer.toString();
        }
    }

}
