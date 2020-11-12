import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.Test;

import java.time.Duration;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-10
 */
public class ResilienceTest {

    @Test
    public void circuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .ringBufferSizeInHalfOpenState(2)
                .ringBufferSizeInClosedState(2)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker breaker1 = registry.circuitBreaker("dao");
        CircuitBreaker breaker2 = registry.circuitBreaker("redis");

        breaker1.onError(0, new RuntimeException());
        System.out.println(breaker1.getState());
        breaker1.onError(0, new RuntimeException());
        System.out.println(breaker1.getState());
    }
}
