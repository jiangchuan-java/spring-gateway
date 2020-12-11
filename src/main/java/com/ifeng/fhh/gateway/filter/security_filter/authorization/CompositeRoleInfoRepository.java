package com.ifeng.fhh.gateway.filter.security_filter.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 将所有的权限仓库聚合在一个类中，由此类统一提供服务
 * 仅聚合仓库，便于管理
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-11
 */
@Component
public class CompositeRoleInfoRepository implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeRoleInfoRepository.class);

    private ApplicationContext applicationContext;


    private List<AbstractRoleInfoRepository> roleInfoRepositoryList = new ArrayList<>();

    public String matchRoleId(String serviceId, String uri) {
        for(AbstractRoleInfoRepository repository : roleInfoRepositoryList) {
            String roleId = repository.matchRoleId(serviceId, uri);
            if(Objects.nonNull(roleId)){
                return roleId;
            }
        }
        return null;
    }



    @PostConstruct
    public void lookforRoleInfoRepository(){
        Map<String, AbstractRoleInfoRepository> beansOfType = applicationContext.getBeansOfType(AbstractRoleInfoRepository.class);
        for(AbstractRoleInfoRepository roleInfoRepository : beansOfType.values()){
            roleInfoRepositoryList.add(roleInfoRepository);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
