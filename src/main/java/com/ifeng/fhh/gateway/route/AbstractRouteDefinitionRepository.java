package com.ifeng.fhh.gateway.route;

import com.ifeng.fhh.gateway.discovery.RefreshInstancesEvent;
import com.ifeng.fhh.gateway.filter.security_filter.authorization.CompositeRoleInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由定义解耦层，使路由定义仓库与具体的存储层解耦
 *           gateway-route
 *        gateway-route-composite -> 这里进行了聚合，所以可以同时存在多种实现
 *     AbstractRouteDefinitionRepository
 * nacos     apollo      数据库       其他...
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-10
 */
public class AbstractRouteDefinitionRepository implements RouteDefinitionRepository, ApplicationEventPublisherAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRouteDefinitionRepository.class);

    //不用子类用什么存储，这里就是要根据id获取路由定义
    private ConcurrentHashMap<String/*serviceId*/, RouteDefinition/*路由定义*/> routeDefinitionCache = new ConcurrentHashMap<>();

    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(routeDefinitionCache.values());
    }

    /**
     * 更新路由定义仓库
     * @param serviceId 路由id
     * @param routeDefinition 路由定义
     */
    public final void updateRepository(String serviceId, RouteDefinition routeDefinition){
        routeDefinitionCache.put(serviceId, routeDefinition);
        publishRouteDefinitonRefreshEvent();
        String scheme = routeDefinition.getUri().getScheme();
        if(Objects.equals(scheme,"lb")){
            publishInstanceRefreshEvent(routeDefinition);
        }
    }

    private void publishInstanceRefreshEvent(RouteDefinition routeDefinition) {
        String host = routeDefinition.getUri().getHost();
        LOGGER.info("********** publishInstanceRefreshEvent host : {}", host);
        applicationEventPublisher.publishEvent(new RefreshInstancesEvent(this,host));
    }

    /**
     * 发送路由定义刷新事件，重要的监听者有
     * 1：CachingRouteDefinitionLocator -> spring gateway更新内路由
     * 2：CompositeInstanceDiscovery -> 监听路由变更后，同时去刷新注册中心，目的是根据最新路由定义获取实例列表
     */
    private void publishRouteDefinitonRefreshEvent() {
        LOGGER.info("********** publishRouteDefinitonRefreshEvent");
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
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
}
