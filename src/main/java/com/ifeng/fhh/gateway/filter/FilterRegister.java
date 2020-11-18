package com.ifeng.fhh.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
@Component
public class FilterRegister implements BeanFactoryAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterRegister.class);


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if(beanFactory instanceof DefaultListableBeanFactory){
            try {
                registerFilter((DefaultListableBeanFactory) beanFactory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void registerFilter(DefaultListableBeanFactory dflbf) throws Exception{
        FilterConfiguration[] filters = FilterConfiguration.values();
        for (FilterConfiguration filter : filters) {
            Class clazz = filter.getClazz();
            int order = filter.getOrder();
            Object singleton = clazz.newInstance();

            Field orderField = clazz.getDeclaredField("order");
            orderField.setAccessible(true);
            orderField.setInt(singleton, order);

            dflbf.registerSingleton(clazz.getSimpleName(), singleton);
            LOGGER.info("register filter bean : {}, order: {}", clazz.getSimpleName(), order);
        }
    }
}
