package com.ifeng.fhh.gateway.spring.bean;

import org.springframework.context.ApplicationEvent;

/**
 * 熔断器配置变更事件,暂未实际使用
 * 使用思路：
 * 业务监听事件后，从所有仓库获取最新配置 ↑
 *              <- List<Repository>      ↑
 *                      <- ApolloRepository发布此事件 ↑
 *
 * apollo的配置变更后，发布对应事件，由关注具体事件的业务自己进行更新
 * 主要用于解耦
 *
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-25
 */
public class RefreshBreakerConfigEvent extends ApplicationEvent {
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public RefreshBreakerConfigEvent(Object source) {
        super(source);
    }
}
