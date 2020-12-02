package com.ifeng.fhh.gateway.filter.loadbalance_filter.lb_factory;

import com.ifeng.fhh.gateway.filter.loadbalance_filter.instance_discover.AbstractInstanceDiscover;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.instance_discover.NacosInstanceDiscoverer;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.lb_algorithm.AbstractLBAlgorithm;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.lb_algorithm.RandomLBAlgorithm;
import org.springframework.beans.BeansException;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


/**
 * @Des: 随机算法 + nacos = 负载均衡器
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
@Component("randomLoadBalancerFactory")
public class RandomLoadBalancerFactory extends AbstractLoadBalancerFactory  {

    private ApplicationContext applicationContext;

    @Override
    protected AbstractLBAlgorithm buildLBAlgorithm() {
        return new RandomLBAlgorithm();
    }

    @Override
    protected AbstractInstanceDiscover buildInstanceDiscover() {
        NacosInstanceDiscoverer instanceDiscoverer = applicationContext.getBean(NacosInstanceDiscoverer.class);
        return instanceDiscoverer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


}
