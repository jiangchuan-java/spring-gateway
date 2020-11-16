package com.ifeng.fhh.gateway.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-16
 */
@Controller
@ResponseBody
public class Controller_4j {

    private static final Logger LOGGER = LoggerFactory.getLogger(Controller_4j.class);

    static CircuitBreakerConfig breakerConfig;
    static CircuitBreakerRegistry registry;
    static CircuitBreaker breaker;

    static {
         breakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(10) // 断路器关闭状态下队列的大小，队列满了之后，触发失败比例的计算
                .failureRateThreshold(50) //失败比例
                .waitDurationInOpenState(Duration.ofSeconds(5)) //断路器开启后保持多久
                .ringBufferSizeInHalfOpenState(2) //在试探时的队列大小，满了计算失败比例
                .build();
         registry = CircuitBreakerRegistry.of(breakerConfig);
         breaker = registry.circuitBreaker("api");
    }
    private int i = 0;

    @RequestMapping("/4j")
    public Mono<String> testResilience4j(){

       /* LOGGER.info("4j : {}", Thread.currentThread().getName());

        Supplier<CompletionStage<String>> completionStageSupplier = CircuitBreaker.decorateCompletionStage(breaker, () -> {
            CompletableFuture<String> completableFuture = new CompletableFuture<>();
            completableFuture.complete("ok");
            return completableFuture;
        });

        CompletionStage<String> stringCompletionStage = completionStageSupplier.get();

        Mono<String> mono = Mono.fromCompletionStage(stringCompletionStage);
        return mono;*/
       synchronized (this) {
           i++;
       }

       return Mono.just(""+i);
    }
}
