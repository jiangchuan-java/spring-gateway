package com.ifeng.fhh.gateway.apollo;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;

/**
 * @Des: apollo breaker配置中心
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-23
 */
public class ApolloBreakerConfigRepository {

    private static final String BREAKER_CONFIG_NAMESPACE = "breaker-config";

    private Config apolloConfig;

    public ApolloBreakerConfigRepository(){
        apolloConfig = ConfigService.getConfig(BREAKER_CONFIG_NAMESPACE);
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
    }
}
