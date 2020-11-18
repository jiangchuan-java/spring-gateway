package com.ifeng.fhh.gateway.filter.loadbalance_filter.lb_algorithm;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-10-29
 */
public abstract class AbstractLBAlgorithm {


    public ServiceInstance select(List<ServiceInstance> instances) {
        if(CollectionUtils.isEmpty(instances)) {
            return null;
        }
        if(instances.size() == 1){
            return instances.get(0);
        }
        return doSelect(instances);
    }

    protected abstract ServiceInstance doSelect(List<ServiceInstance> instances);


    /**
     * 默认0
     * @param instance
     * @return
     */
    int getWeight(ServiceInstance instance) {
        String weightValue = instance.getMetadata().get("ipWeight");
        if(Objects.isNull(weightValue)){
            return 0;
        } else {
            try {
                return Integer.valueOf(weightValue);
            }catch (Exception e){
                return 0;
            }
        }
    }
}
