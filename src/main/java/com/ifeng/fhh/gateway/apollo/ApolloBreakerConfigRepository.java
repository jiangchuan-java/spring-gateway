package com.ifeng.fhh.gateway.apollo;

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
 * @Des: apollo breaker配置中心
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
            String routeDefinitionValue = apolloConfig.getProperty(serviceId, null);
            ApolloBreakerModel breakerModel = JackSonUtils.json2Bean(routeDefinitionValue, ApolloBreakerModel.class);
            CircuitBreaker breaker = buildCircuitBreaker(breakerModel);
            if (Objects.nonNull(breaker)) {
                breakerCache.put(serviceId, breaker);
                noticeBreakerFilterUpdate(serviceId, breaker);
            }
        }
        apolloConfig.addChangeListener(new BreakerConfigChangeListener());
    }

    /**
     * 更新breaker配置
     * @param breakerModel
     */
    private void updateRepository(ApolloBreakerModel breakerModel){
        String serviceId = breakerModel.getServiceId();
        CircuitBreaker breaker = buildCircuitBreaker(breakerModel);
        breakerCache.put(serviceId, breaker);
        noticeBreakerFilterUpdate(serviceId, breaker);
    }

    /**
     * 更新filter中目前使用的filter
     * @param serviceId
     * @param breaker
     */
    private void noticeBreakerFilterUpdate(String serviceId, CircuitBreaker breaker) {
        breakerGlobalGatewayFilter.updateBreakerMap(serviceId, breaker);
    }

    /**
     * 为每个route构建独立的熔断器
     * @param
     * @return
     */
    private CircuitBreaker buildCircuitBreaker(ApolloBreakerModel breakerModel) {
        String serviceId = breakerModel.getServiceId();
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
        CircuitBreaker breaker = CircuitBreakerRegistry.of(breakerConfig).circuitBreaker(breakerModel.getServiceId());


        LOGGER.info("build new breaker {} : {}", serviceId, breaker);

        return breaker;
    }


    private class BreakerConfigChangeListener implements ConfigChangeListener {

        @Override
        public void onChange(ConfigChangeEvent changeEvent) {
            try {
                for (String serverName : changeEvent.changedKeys()) {
                    String newValue = changeEvent.getChange(serverName).getNewValue();
                    String oldValue = changeEvent.getChange(serverName).getOldValue();

                    LOGGER.info("BreakerConfigChangeListener, serverId : {} changed, oldValue: {}, newValue: {}"
                            , serverName, oldValue, newValue);
                    ApolloBreakerModel newConfig = JackSonUtils.json2Bean(newValue, ApolloBreakerModel.class);

                    updateRepository(newConfig);

                }
            } catch (Exception e) {
                LOGGER.error("BreakerConfigChangeListener failed exception: {}", e);
            }
        }
    }

    private static class ApolloBreakerModel {

        //业务id
        private String serviceId;
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

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

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
