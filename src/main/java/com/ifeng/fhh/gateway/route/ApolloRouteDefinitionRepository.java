package com.ifeng.fhh.gateway.route;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ifeng.fhh.gateway.util.JackSonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import static org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory.PATTERN_KEY;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import static org.springframework.cloud.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
import static org.springframework.cloud.gateway.support.RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;


import static org.springframework.cloud.gateway.support.NameUtils.normalizeRoutePredicateName;

/**
 * @Des: 动态路由仓库
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-3
 */
@Repository
public class ApolloRouteDefinitionRepository extends AbstractRouteDefinitionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloRouteDefinitionRepository.class);

    @Value("${apollo.namespace.route-definition}")
    private String namespace;

    private Config apolloConfig;

    /**
     *
     * 注意 serviceId 是请求时，用于区分业务的
     *     lb://xxx 中 xxx代表的是naocs 服务名称
     */
    @PostConstruct
    private void initRepository() throws Exception{
        apolloConfig = ConfigService.getConfig(namespace);
        Set<String> serviceIdSet = apolloConfig.getPropertyNames();
        for(String serviceId : serviceIdSet){
            String routeDefinitionValue = apolloConfig.getProperty(serviceId, null);
            ApolloRouteModel routeModel = JackSonUtils.json2Bean(routeDefinitionValue, ApolloRouteModel.class);
            RouteDefinition routeDefinition = buildRouteDefinition(routeModel);
            if(Objects.nonNull(routeDefinition)){
                super.updateRepository(serviceId, routeDefinition);
            }
        }
        apolloConfig.addChangeListener(new RouteDefinitonChangeListener());

    }

    private void updateRepository(ApolloRouteModel routeModel){
        String serviceId = routeModel.getServiceId();
        RouteDefinition routeDefinition = buildRouteDefinition(routeModel);
        super.updateRepository(serviceId, routeDefinition);
    }

    private RouteDefinition buildRouteDefinition(ApolloRouteModel routeModel) {
        try {
            ArrayList<PredicateDefinition> predicateDef = new ArrayList<>();

            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setName(normalizeRoutePredicateName(PathRoutePredicateFactory.class));
            predicate.addArg(PATTERN_KEY, "/"+routeModel.getServiceId()+"/**");
            predicateDef.add(predicate);

            Long connectTimeout = routeModel.getConnectTimeout();
            Long responseTimeout = routeModel.getResponseTimeout();


            RouteDefinition routeDefinition = new RouteDefinition();
            routeDefinition.setUri(URI.create(routeModel.getUri()));
            routeDefinition.setPredicates(predicateDef);
            routeDefinition.setId(routeModel.getServiceId());
            /*
              NettyRoutingFilter中读取以下属性，生成netty httpclient
              route.getMetadata().get(RESPONSE_TIMEOUT_ATTR); //单位毫秒
              route.getMetadata().get(CONNECT_TIMEOUT_ATTR); //单位毫秒
             */
            if(connectTimeout != null){
                routeDefinition.getMetadata().put(CONNECT_TIMEOUT_ATTR, connectTimeout);
            }
            if(responseTimeout != null){
                routeDefinition.getMetadata().put(RESPONSE_TIMEOUT_ATTR, responseTimeout);
            }

            LOGGER.info("********** build new route : {}", routeDefinition.toString());

            return routeDefinition;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private class RouteDefinitonChangeListener implements ConfigChangeListener {

        @Override
        public void onChange(ConfigChangeEvent changeEvent) {
            try {
                for (String serverName : changeEvent.changedKeys()) {
                    String newValue = changeEvent.getChange(serverName).getNewValue();
                    String oldValue = changeEvent.getChange(serverName).getOldValue();

                    LOGGER.info("**********  RouteDefinitonChangeListener, serverId : {} changed, oldValue: {}, newValue: {}"
                            , serverName, oldValue, newValue);
                    ApolloRouteModel newConfig = JackSonUtils.json2Bean(newValue, ApolloRouteModel.class);

                    updateRepository(newConfig);

                }
            } catch (Exception e) {
                LOGGER.error("********** RouteDefinitonChangeListener failed exception: {}", e);
            }
        }
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return null;
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return null;
    }

    private static class ApolloRouteModel {

        private String serviceId;

        private String uri;

        private Long connectTimeout;

        private Long responseTimeout;


        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Long getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Long connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Long getResponseTimeout() {
            return responseTimeout;
        }

        public void setResponseTimeout(Long responseTimeout) {
            this.responseTimeout = responseTimeout;
        }
    }
}
