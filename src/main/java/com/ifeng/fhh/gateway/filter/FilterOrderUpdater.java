package com.ifeng.fhh.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @Des: 用于调整filer的顺序，按filterOderDefine中的顺序设置
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-18
 */
@Component
public class FilterOrderUpdater implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterOrderUpdater.class);

    private Map<Class, Integer> filterClassOrderInfo = new HashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        if(filterClassOrderInfo.containsKey(beanClass)){
            int order = filterClassOrderInfo.get(beanClass);
            try {
                Field orderField = beanClass.getDeclaredField("order");
                orderField.setAccessible(true);
                orderField.set(bean, order);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bean;
    }


    @PostConstruct
    private void initOrderInfo() throws Exception{
        FilterOrderDefine[] filters = FilterOrderDefine.values();
        for (FilterOrderDefine filter : filters) {
            Class clazz = filter.getClazz();
            int order = filter.getOrder();
            filterClassOrderInfo.put(clazz, order);
            LOGGER.info("register filter bean : {}, order: {}", clazz.getSimpleName(), order);
        }
    }
}
