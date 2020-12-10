package com.ifeng.fhh.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;

/**
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-10
 */
public abstract class OrderedGlobalFilter implements GlobalFilter, Ordered {

    private int order;

    @Override
    public int getOrder() {
        return order;
    }


    public void setOrder(int order) {
        this.order = order;
    }

}
