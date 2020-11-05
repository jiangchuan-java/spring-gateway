package com.ifeng.fhh.gateway.authority.apollo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ctrip.framework.apollo.Config;
import com.ifeng.fhh.gateway.authority.AbstractUriAuthorityRepository;
import com.ifeng.fhh.gateway.util.JackSonUtils;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;



/**
 * @Des: 动态路由仓库
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-3
 */
@Repository
public class ApolloUriAuthorityRepository extends AbstractUriAuthorityRepository {

    private Config apolloConfig;

    private ConcurrentHashMap<String/*serverId*/, ConcurrentHashMap<String/*uri*/, String>/*roleId*/> authorityCache = new ConcurrentHashMap<>();

    private static final String ROUTE_DEFINITION_NAMESPACE = "uri-authority";


    public ApolloUriAuthorityRepository(){
        //apolloConfig = ConfigService.getConfig(ROUTE_DEFINITION_NAMESPACE);
        initCache();
    }

    private void initCache(){
        /*Set<String> serverNameSet = apolloConfig.getPropertyNames();
        for(String serverName : serverNameSet){
            String routeDefinitionValue = apolloConfig.getProperty(serverName, null);
            RouteDefinition routeDefinition = buildRouteDefinition(routeDefinitionValue);
            if(Objects.nonNull(routeDefinition)){
                routeDefinitionCache.put(serverName, routeDefinition);
            }
        }*/
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serviceId","zmt-service");
        JSONArray jsonArray = new JSONArray();

        JSONObject jsonObject1  = new JSONObject();
        jsonObject1.put("uri","/operator/account/update");
        jsonObject1.put("roleId","manager_zmt_operator_account_edit");

        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("uri","/operator/account/insert");
        jsonObject2.put("roleId","manager_zmt_operator_account_add");

        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);
        jsonObject.put("authorityConfigList", jsonArray);

        buildAuthority(jsonObject.toJSONString());
    }

    private void buildAuthority(String config) {
        try {
            ApolloRouteModel apolloRouteModel = JackSonUtils.json2Bean(config, ApolloRouteModel.class);

            String serviceId = apolloRouteModel.getServiceId();
            ConcurrentHashMap<String, String> uriMap = authorityCache.get(apolloRouteModel.getServiceId());
            if(Objects.isNull(uriMap)){
                uriMap = new ConcurrentHashMap<>();
            }
            List<ApolloRouteModel.AuthorityConfig> authorityConfigList = apolloRouteModel.getAuthorityConfigList();
            Iterator<ApolloRouteModel.AuthorityConfig> iterator = authorityConfigList.iterator();
            while (iterator.hasNext()) {
                ApolloRouteModel.AuthorityConfig authorityConfig = iterator.next();
                uriMap.put(authorityConfig.getUri(), authorityConfig.getRoleId());
            }
            authorityCache.put(serviceId, uriMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String matchRoleId(String serverId, String uri) {
        if(!authorityCache.containsKey(serverId)){
            return null;
        } else {
            ConcurrentHashMap<String, String> uriMap = authorityCache.get(serverId);
            String roleId = uriMap.get(uri);
            if(Objects.nonNull(roleId)){
                return roleId;
            }
        }
        return null;
    }


    private static class ApolloRouteModel {

        private String serviceId;

        private List<AuthorityConfig> authorityConfigList;

        private static class AuthorityConfig {

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

        public List<AuthorityConfig> getAuthorityConfigList() {
            return authorityConfigList;
        }

        public void setAuthorityConfigList(List<AuthorityConfig> authorityConfigList) {
            this.authorityConfigList = authorityConfigList;
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

    }

}
