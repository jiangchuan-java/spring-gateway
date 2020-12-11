package com.ifeng.fhh.gateway.breaker;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ifeng.fhh.gateway.filter.breaker_filter.BreakerGlobalGatewayFilter;
import com.ifeng.fhh.gateway.util.JackSonUtils;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * todo 这里应解耦，参考路由定义与注册中心
 *
 * @Des: breaker breaker配置中心
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-23
 */
@Repository
public class ApolloBreakerConfigRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloBreakerConfigRepository.class);

    @Value("${apollo.namespace.breaker-config}")
    private String namespace;

    @Autowired
    private BreakerGlobalGatewayFilter breakerGlobalGatewayFilter;

    private ConcurrentHashMap<String/*serverId*/, CircuitBreaker> breakerCache = new ConcurrentHashMap<>();

    private static final String SLIDING_WINDOW_TYPE_TIME = "TIME";

    private static final String SLIDING_WINDOW_TYPE_COUNT = "COUNT";

    private Config apolloConfig;


    @PostConstruct
    private void initRepository() throws Exception {
        apolloConfig = ConfigService.getConfig(namespace);
        Set<String> serviceIdSet = apolloConfig.getPropertyNames();
        for (String serviceId : serviceIdSet) {
            String config = apolloConfig.getProperty(serviceId, null);
            CircuitBreaker breaker = buildCircuitBreaker(serviceId, config);
            if (Objects.nonNull(breaker)) {
                breakerCache.put(serviceId, breaker);
                noticeBreakerFilterUpdate(serviceId, breaker);
            }
        }
        apolloConfig.addChangeListener(new BreakerConfigChangeListener());
    }

    /**
     * 更新breaker配置
     *
     * @param config
     */
    private void updateRepository(String serviceId, String config) {
        CircuitBreaker breaker = buildCircuitBreaker(serviceId, config);
        if (Objects.nonNull(breaker)) {
            breakerCache.put(serviceId, breaker);
            noticeBreakerFilterUpdate(serviceId, breaker);
        }
    }

    /**
     * 更新filter中目前使用的filter
     *
     * @param serviceId
     * @param breaker
     */
    private void noticeBreakerFilterUpdate(String serviceId, CircuitBreaker breaker) {
        breakerGlobalGatewayFilter.updateBreakerMap(serviceId, breaker);
    }

    /**
     * 为每个route构建独立的熔断器
     *
     * @param
     * @return
     */
    private CircuitBreaker buildCircuitBreaker(String serviceId, String config) {
        try {
            ApolloBreakerModel breakerModel = JackSonUtils.json2Bean(config, ApolloBreakerModel.class);
            CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom();
            if (Objects.equals(breakerModel.getSlidingWindowType(), SLIDING_WINDOW_TYPE_TIME)) {
                builder = builder.slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
            } else {
                builder = builder.slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
            }
            builder = builder.minimumNumberOfCalls(breakerModel.minimumNumberOfCalls);
            builder = builder.failureRateThreshold(breakerModel.failureRateThreshold);
            builder = builder.waitDurationInOpenState(Duration.ofSeconds(breakerModel.waitDurationInOpenState));
            builder = builder.permittedNumberOfCallsInHalfOpenState(breakerModel.permittedNumberOfCallsInHalfOpenState);
            CircuitBreakerConfig breakerConfig = builder.build();
            CircuitBreaker breaker = CircuitBreakerRegistry.of(breakerConfig).circuitBreaker(serviceId);
            LOGGER.info("build new breaker {} : {}", serviceId, breaker);

            return breaker;
        } catch (Exception e) {
            LOGGER.error("serviceId : {}, config : {} build error: {}", e);
        }
        return null;

    }


    private class BreakerConfigChangeListener implements ConfigChangeListener {

        @Override
        public void onChange(ConfigChangeEvent changeEvent) {
            try {
                for (String serviceId : changeEvent.changedKeys()) {
                    String newValue = changeEvent.getChange(serviceId).getNewValue();
                    String oldValue = changeEvent.getChange(serviceId).getOldValue();

                    LOGGER.info("BreakerConfigChangeListener, serverId : {} changed, oldValue: {}, newValue: {}"
                            , serviceId, oldValue, newValue);

                    updateRepository(serviceId, newValue);

                }
            } catch (Exception e) {
                LOGGER.error("BreakerConfigChangeListener failed exception: {}", e);
            }
        }
    }

    private static class ApolloBreakerModel {
        //time or count
        private String slidingWindowType;
        //多少次一个统计周期,如果是时间类型的：单位就是秒*/
        private int slidingWindowSize;
        //最少调用多少次才能进行统计
        private int minimumNumberOfCalls;
        //失败率百分比
        private int failureRateThreshold;
        //维持熔断状态时间，单位秒
        private int waitDurationInOpenState;
        //打开状态下，尝试多少次请求
        private int permittedNumberOfCallsInHalfOpenState;

        public String getSlidingWindowType() {
            return slidingWindowType;
        }

        public void setSlidingWindowType(String slidingWindowType) {
            this.slidingWindowType = slidingWindowType;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }

        public int getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(int failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public int getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        public void setWaitDurationInOpenState(int waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public int getPermittedNumberOfCallsInHalfOpenState() {
            return permittedNumberOfCallsInHalfOpenState;
        }

        public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
        }
    }
}
