import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-10
 */
public class ResilienceTest {

    @Test
    public void circuitBreakerAsync() throws Exception{
        CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(4) // 断路器关闭状态下队列的大小，队列满了之后，触发失败比例的计算
                .failureRateThreshold(50) //失败比例
                .waitDurationInOpenState(Duration.ofSeconds(5)) //断路器开启后保持多久
                .ringBufferSizeInHalfOpenState(2) //在试探时的队列大小，满了计算失败比例
                .build();

        TimeLimiterConfig timeConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .cancelRunningFuture(true)
                .build();
        TimeLimiter timeLimiter = TimeLimiter.of(timeConfig);


        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(breakerConfig);
        CircuitBreaker breaker = registry.circuitBreaker("dao");


        Supplier<CompletionStage<String>> completionStageSupplier = CircuitBreaker.decorateCompletionStage(breaker, () -> {
            CompletableFuture<String> completableFuture = new CompletableFuture<>();
            completableFuture.completeExceptionally(new RuntimeException("异步任务失败了"));
            return completableFuture;
        });



        for(int i=0;i<15;i++){
            try {
                CompletionStage<String> stringCompletionStage = completionStageSupplier.get();
                stringCompletionStage.whenComplete((str,throwable)->{
                    System.out.println(throwable);
                    System.out.println(str);
                });

                TimeUnit.SECONDS.sleep(1);
            }catch (Exception e){
                e.printStackTrace();
            }

        }


        System.in.read();
    }


    @Test
    public void circuitBreakerSync() throws Exception{
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .ringBufferSizeInClosedState(4) // 断路器关闭状态下队列的大小，队列满了之后，触发失败比例的计算
                .failureRateThreshold(50) //失败比例
                .waitDurationInOpenState(Duration.ofSeconds(5)) //断路器开启后保持多久
                .ringBufferSizeInHalfOpenState(2) //在试探时的队列大小，满了计算失败比例
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker breaker1 = registry.circuitBreaker("dao");


        Runnable runnable = CircuitBreaker.decorateRunnable(breaker1, new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName());
                    throw new RuntimeException("主动失败");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        for(int i=0;i<15;i++){
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println(i + " " + breaker1.getState());
                runnable.run();
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        System.in.read();
    }


    @Test
    public void circuitTimeLimiter() throws Exception{


        TimeLimiterConfig timeConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .cancelRunningFuture(true)
                .build();
        TimeLimiter timeLimiter = TimeLimiter.of(timeConfig);

        System.in.read();
    }


    @Test
    public void circuitBulkhead() throws Exception{

        BulkheadConfig bulkconfig = BulkheadConfig.custom()
                .maxConcurrentCalls(5)
                .build();

        Bulkhead bulkhead = Bulkhead.of("bulk", bulkconfig);

        Runnable runnable = Bulkhead.decorateRunnable(bulkhead, new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(Thread.currentThread().getName());
                    TimeUnit.SECONDS.sleep(5); //阻塞在这里
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });


        for(int i=0;i<15;i++){
            try {
                Thread t = new Thread(runnable);
                t.start();
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        System.in.read();
    }


    @Test
    public void rateLimiter() throws Exception {
        // 创建限流器配置
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(2000))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        // 创建限流器注册器RateLimiterRegistry
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);

        // 通过RateLimiterRegistry来创建限流器
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("rateLimiter", config);

        RateLimiter.Metrics metrics = rateLimiter.getMetrics();

        rateLimiter.getEventPublisher().onSuccess(event -> {
            System.out.println(event.getEventType() + ":::可用令牌数: " + metrics.getAvailablePermissions() + ", 等待线程数: "
                    + metrics.getNumberOfWaitingThreads());
        }).onFailure(event -> {
            System.out.println(event.getEventType() + ":::可用令牌数: " + metrics.getAvailablePermissions() + ", 等待线程数: "
                    + metrics.getNumberOfWaitingThreads());
        });


        TimeUnit.SECONDS.sleep(3);
        for(int i=0;i<10;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean permission = rateLimiter.acquirePermission(1);
                    if(permission){
                        System.out.println(Thread.currentThread().getName()+" acquire success");
                    }
                }
            }).start();
        }


        System.in.read();

    }
}
