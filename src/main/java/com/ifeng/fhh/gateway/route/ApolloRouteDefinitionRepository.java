package com.ifeng.fhh.gateway.route;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.discover.NacosInstanceDiscoverer;
import com.ifeng.fhh.gateway.util.JackSonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import static org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory.PARTS_KEY;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import static org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory.PATTERN_KEY;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


import static org.springframework.cloud.gateway.support.NameUtils.normalizeFilterFactoryName;
import static org.springframework.cloud.gateway.support.NameUtils.normalizeRoutePredicateName;

/**
 * @Des: 动态路由仓库
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-3
 */
@Repository
public class ApolloRouteDefinitionRepository implements RouteDefinitionRepository, ApplicationEventPublisherAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloRouteDefinitionRepository.class);

    @Autowired
    private NacosInstanceDiscoverer nacosInstanceDiscoverer;

    private Config apolloConfig;

    private ConcurrentHashMap<String/*servername*/, RouteDefinition/*路由定义*/> routeDefinitionCache = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String/*serviveId*/, Route/*路由定义*/> routeCache = new ConcurrentHashMap<>();

    private static final String ROUTE_DEFINITION_NAMESPACE = "route-definition";

    private ApplicationEventPublisher applicationEventPublisher;

    private static final List<PredicateDefinition> defalutPD = new ArrayList<>();
    private static final List<FilterDefinition> defaultFD = new ArrayList<>();

    static {
        //去掉第一个请求uri
        FilterDefinition stripPrefix = new FilterDefinition();
        stripPrefix.setName(normalizeFilterFactoryName(StripPrefixGatewayFilterFactory.class));
        stripPrefix.addArg(PARTS_KEY, "1");
        defaultFD.add(stripPrefix);
    }

    public ApolloRouteDefinitionRepository(){
        apolloConfig = ConfigService.getConfig(ROUTE_DEFINITION_NAMESPACE);
        initCache();
    }

    /**
     *
     * 注意 serviceId 是请求时，用于区分业务的
     *     lb://xxx 中 xxx代表的是naocs 服务名称
     *
     * JSONObject jsonObject = new JSONObject();
     * jsonObject.put("serviceId","zmt-service");
     * jsonObject.put("uri","lb://fhh-test");
     * RouteDefinition routeDefinition = buildRouteDefinition(jsonObject.toJSONString());
     * routeDefinitionCache.put(routeDefinition.getId(), routeDefinition);
     *
     */
    private void initCache(){
        Set<String> serverNameSet = apolloConfig.getPropertyNames();
        for(String serverName : serverNameSet){
            String routeDefinitionValue = apolloConfig.getProperty(serverName, null);
            RouteDefinition routeDefinition = buildRouteDefinition(routeDefinitionValue);
            if(Objects.nonNull(routeDefinition)){
                routeDefinitionCache.put(serverName, routeDefinition);
                nacosInstanceDiscoverer.initServerInstanceCache(routeDefinition.getUri().getHost());
            }
        }

    }


    private RouteDefinition buildRouteDefinition(String routeDefinitionValue) {
        try {
            ApolloRouteModel model = JackSonUtils.json2Bean(routeDefinitionValue, ApolloRouteModel.class);

            ArrayList<PredicateDefinition> predicateDef = new ArrayList<>();

            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setName(normalizeRoutePredicateName(PathRoutePredicateFactory.class));
            predicate.addArg(PATTERN_KEY, "/"+model.getServiceId()+"/**");
            predicateDef.add(predicate);


            RouteDefinition routeDefinition = new RouteDefinition();
            routeDefinition.setUri(URI.create(model.getUri()));
            routeDefinition.setPredicates(predicateDef);
            routeDefinition.setFilters(defaultFD);
            routeDefinition.setId(model.getServiceId());

            LOGGER.info("build new route : {}", routeDefinition.toString());

            return routeDefinition;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(routeDefinitionCache.values());
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return null;
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return null;
    }


    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    private static class ApolloRouteModel {

        private String serviceId;

        private String uri;

        private List<AuthorityConfig> authorityConfigList;

        private static class AuthorityConfig {

            private String path;

            private String roleId;

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
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

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }





}
