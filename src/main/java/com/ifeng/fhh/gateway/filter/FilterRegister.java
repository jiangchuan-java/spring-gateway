package com.ifeng.fhh.gateway.filter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
@Component
public class FilterRegister implements BeanFactoryAware {



    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if(beanFactory instanceof DefaultListableBeanFactory){
            registerFilter((DefaultListableBeanFactory) beanFactory);
        }
    }


    private void registerFilter(DefaultListableBeanFactory dflbf) {
        FilterManager[] values = FilterManager.values();
        for (FilterManager value : values) {
            Class clazz = value.getClazz();
        }
    }
}
