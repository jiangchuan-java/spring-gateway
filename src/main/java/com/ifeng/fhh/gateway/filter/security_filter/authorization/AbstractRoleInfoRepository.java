package com.ifeng.fhh.gateway.filter.security_filter.authorization;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
public abstract class AbstractRoleInfoRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRoleInfoRepository.class);

    private ConcurrentHashMap<String/*serviceId*/, ConcurrentHashMap<String/*uri*/, String>/*roleId*/> roleInfoCache = new ConcurrentHashMap<>();

    public String matchRoleId(String serviceId, String uri) {
        String roleId = null;
        if(!roleInfoCache.containsKey(serviceId)){
            return null;
        } else {
            ConcurrentHashMap<String, String> uriMap = roleInfoCache.get(serviceId);
            roleId = uriMap.get(uri);
        }
        LOGGER.info("********** matchRoleId, serverId : {}, uri : {}, roleId : {}", serviceId, uri, roleId);
        return roleId;
    }

    /**
     * 内部刷新
     * @param serviceId
     * @param roleInfoMap
     */
    protected final void internalRefresh(String serviceId, ConcurrentHashMap<String, String> roleInfoMap) {
        if(Objects.nonNull(roleInfoMap)){
            roleInfoCache.put(serviceId, roleInfoMap);
            LOGGER.info("********** refresh, serverId : {}, map : {}", serviceId,roleInfoMap.toString());
        }
    }
}
