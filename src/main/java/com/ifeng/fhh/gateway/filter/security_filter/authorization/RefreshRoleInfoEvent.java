package com.ifeng.fhh.gateway.filter.security_filter.authorization;

import org.springframework.context.ApplicationEvent;

/**
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-10
 */
public class RefreshRoleInfoEvent extends ApplicationEvent {

    private String serviceId;
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public RefreshRoleInfoEvent(Object source, String serviceId) {
        super(source);
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
}
