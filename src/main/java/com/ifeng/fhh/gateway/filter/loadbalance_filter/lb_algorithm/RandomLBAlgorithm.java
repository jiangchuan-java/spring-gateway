package com.ifeng.fhh.gateway.filter.loadbalance_filter.lb_algorithm;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Des: 权重+随机
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
public class RandomLBAlgorithm extends AbstractLBAlgorithm {

    public static final String NAME = "random";

    @Override
    protected ServiceInstance doSelect(List<ServiceInstance> instances) {
        // Number of invokers
        int length = instances.size();
        // Every invoker has the same weight?
        boolean sameWeight = true;
        // the weight of every invokers
        int[] weights = new int[length];
        // the first invoker's weight
        int firstWeight = getWeight(instances.get(0));
        weights[0] = firstWeight;
        // The sum of weights
        int totalWeight = firstWeight;
        for (int i = 1; i < length; i++) {
            int weight = getWeight(instances.get(i));
            // save for later use
            weights[i] = weight;
            // Sum
            totalWeight += weight;
            if (sameWeight && weight != firstWeight) {
                sameWeight = false;
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            // Return a invoker based on the random value.
            for (int i = 0; i < length; i++) {
                offset -= weights[i];
                if (offset < 0) {
                    return instances.get(i);
                }
            }
        }
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        return instances.get(ThreadLocalRandom.current().nextInt(length));
    }
}
