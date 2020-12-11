package com.ifeng.fhh.gateway.filter.security_filter.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将所有的权限配置聚合在一个类中，由此类统一提供服务
 *
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-11
 */
@Component
public class CompositeRoleInfoRepository implements ApplicationContextAware, ApplicationListener<RefreshRoleInfoEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeRoleInfoRepository.class);

    private ApplicationContext applicationContext;

    private ConcurrentHashMap<String/*serviceId*/, ConcurrentHashMap<String/*uri*/, String>/*roleId*/> roleInfoCache = new ConcurrentHashMap<>();

    private List<AbstractRoleInfoRepository> roleInfoRepositoryList = new ArrayList<>();

    public String matchRoleId(String serviceId, String uri) {
        if(!roleInfoCache.containsKey(serviceId)){
            return null;
        } else {
            ConcurrentHashMap<String, String> uriMap = roleInfoCache.get(serviceId);
            String roleId = uriMap.get(uri);
            if(Objects.nonNull(roleId)){
                LOGGER.info("********** matchRoleId, serverId : {}, uri : {}, roleId : {}", serviceId, uri, roleId);
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

    private synchronized void fetchRoleInfo(String serviceId) {
        roleInfoRepositoryList.forEach(repository->{
            ConcurrentHashMap roleInfoMap = repository.fetchRoleInfoMap(serviceId);
            if(Objects.nonNull(roleInfoMap)){
                roleInfoCache.put(serviceId, roleInfoMap);
                LOGGER.info("*********** update cache serviceId : {}, infoMap : {}", serviceId, roleInfoMap.toString());
            }
        });
    }

    @Override
    public void onApplicationEvent(RefreshRoleInfoEvent event) {
        String serviceId = event.getServiceId();
        fetchRoleInfo(serviceId);
        LOGGER.info("*********** RefreshRoleInfoEvent serviceId : {}", serviceId);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
