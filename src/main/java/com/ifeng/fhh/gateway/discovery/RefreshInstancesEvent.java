package com.ifeng.fhh.gateway.discovery;

import org.springframework.context.ApplicationEvent;

/**
 * 当有实例发生变更时，主动发布此事件，由事件监听者统一处理
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-10
 */
public class RefreshInstancesEvent extends ApplicationEvent {

    private String host;
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public RefreshInstancesEvent(Object source, String host) {
        super(source);
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
