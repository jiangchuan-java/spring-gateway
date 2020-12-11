package com.ifeng.fhh.gateway.filter.security_filter.authorization;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ifeng.fhh.gateway.util.JackSonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;



/**
 * @Des: 动态权限仓库
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-3
 */
@Repository
public class ApolloRoleInfoRepository extends AbstractRoleInfoRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloRoleInfoRepository.class);

    private Config apolloConfig;

    @Value("${apollo.namespace.uri-role}")
    private String namespace;

    @PostConstruct
    private void initCache(){
        apolloConfig = ConfigService.getConfig(namespace);
        Set<String> serviceIdSet = apolloConfig.getPropertyNames();
        for(String serviceId : serviceIdSet){
            String config = apolloConfig.getProperty(serviceId, null);
            addRole(serviceId, config);
        }
        apolloConfig.addChangeListener(new UriRoleChangeListener());
    }

    private void addRole(String serviceId, String config) {
        try {
            ApolloUriRoleModel apolloRouteModel = JackSonUtils.json2Bean(config, ApolloUriRoleModel.class);

            ConcurrentHashMap<String, String> uriMap = new ConcurrentHashMap<>();
            List<ApolloUriRoleModel.UriRoleConfig> authorityConfigList = apolloRouteModel.getRoleConfig();
            Iterator<ApolloUriRoleModel.UriRoleConfig> iterator = authorityConfigList.iterator();
            while (iterator.hasNext()) {
                ApolloUriRoleModel.UriRoleConfig uriRoleConfig = iterator.next();
                String uri = uriRoleConfig.getUri();
                String roleId = uriRoleConfig.getRoleId();
                uriMap.put(uri, roleId);
                LOGGER.info("********** new uri role, serverId : {}, uri : {}, roleId : {}", serviceId, uri, roleId);
            }
            super.internalRefresh(serviceId, uriMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class UriRoleChangeListener implements ConfigChangeListener {

        @Override
        public void onChange(ConfigChangeEvent changeEvent) {
            try {
                for (String serviceId : changeEvent.changedKeys()) {
                    String newValue = changeEvent.getChange(serviceId).getNewValue();
                    String oldValue = changeEvent.getChange(serviceId).getOldValue();

                    LOGGER.info("**********  UriRoleChangeListener, serverId : {} changed, oldValue: {}, newValue: {}"
                            , serviceId, oldValue, newValue);
                    addRole(serviceId, newValue);

                }
            } catch (Exception e) {
                LOGGER.error("********** UriRoleChangeListener failed exception: {}", e);
            }
        }
    }


    private static class ApolloUriRoleModel {

        private List<UriRoleConfig> roleConfig;

        private static class UriRoleConfig {

            private String uri;

            private String roleId;

            public String getUri() {
                return uri;
            }

            public void setUri(String uri) {
                this.uri = uri;
            }

            public String getRoleId() {
                return roleId;
            }

            public void setRoleId(String roleId) {
                this.roleId = roleId;
            }
        }

        public List<UriRoleConfig> getRoleConfig() {
            return roleConfig;
        }

        public void setRoleConfig(List<UriRoleConfig> roleConfig) {
            this.roleConfig = roleConfig;
        }
    }

}
