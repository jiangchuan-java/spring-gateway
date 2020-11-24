package com.ifeng.fhh.gateway.apollo;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ifeng.fhh.gateway.filter.loadbalance_filter.discover.NacosInstanceDiscoverer;
import com.ifeng.fhh.gateway.util.JackSonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import static org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory.PATTERN_KEY;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static org.springframework.cloud.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
import static org.springframework.cloud.gateway.support.RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


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

    private ConcurrentHashMap<String/*serviceId*/, RouteDefinition/*路由定义*/> routeDefinitionCache = new ConcurrentHashMap<>();

    private static final String ROUTE_DEFINITION_NAMESPACE = "route-definition";

    private ApplicationEventPublisher applicationEventPublisher;

    private static final List<PredicateDefinition> defalutPD = new ArrayList<>();
    private static final List<FilterDefinition> defaultFD = new ArrayList<>();


    public ApolloRouteDefinitionRepository(){
        apolloConfig = ConfigService.getConfig(ROUTE_DEFINITION_NAMESPACE);
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
    @PostConstruct
    private void initRepository() throws Exception{
        Set<String> serviceIdSet = apolloConfig.getPropertyNames();
        for(String serviceId : serviceIdSet){
            String routeDefinitionValue = apolloConfig.getProperty(serviceId, null);
            ApolloRouteModel routeModel = JackSonUtils.json2Bean(routeDefinitionValue, ApolloRouteModel.class);
            RouteDefinition routeDefinition = buildRouteDefinition(routeModel);
            if(Objects.nonNull(routeDefinition)){
                routeDefinitionCache.put(serviceId, routeDefinition);
                noticeNacos(routeDefinition.getUri().getHost());
            }
        }
        apolloConfig.addChangeListener(new RouteDefinitonChangeListener());
        noticeCachingRouteLocator();
    }

    private void updateRepository(ApolloRouteModel routeModel){
        String serviceId = routeModel.getServiceId();
        RouteDefinition routeDefinition = buildRouteDefinition(routeModel);
        routeDefinitionCache.put(serviceId, routeDefinition);
        noticeNacos(routeDefinition.getUri().getHost());
        noticeCachingRouteLocator();
    }

    //通知nacos监听新实例
    private void noticeNacos(String host) {
        nacosInstanceDiscoverer.initServerInstanceCache(host);
    }

    //通知路由缓存刷新
    private void noticeCachingRouteLocator() {
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
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
            routeDefinition.setFilters(defaultFD);
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

            LOGGER.info("build new route : {}", routeDefinition.toString());

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

                    LOGGER.info("RouteDefinitonChangeListener, serverName : {} changed, oldValue: {}, newValue: {}"
                            , serverName, oldValue, newValue);
                    ApolloRouteModel newConfig = JackSonUtils.json2Bean(newValue, ApolloRouteModel.class);

                    updateRepository(newConfig);

                }
            } catch (Exception e) {
                LOGGER.error("RouteDefinitonChangeListener failed exception: {}", e);
            }
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
