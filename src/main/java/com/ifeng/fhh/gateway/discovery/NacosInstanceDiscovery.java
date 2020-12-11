package com.ifeng.fhh.gateway.discovery;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Des: Nacos服务发现
 * @Author: jiangchuan
 * <p>
 * @Date: 20-6-15
 */
@Component
public class NacosInstanceDiscovery extends AbstractInstanceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosInstanceDiscovery.class);

    private NamingService namingService;


    private String serverAddr;

    private String namespace;

    private Set<String> subscribeList = new HashSet<>();


    public NacosInstanceDiscovery(@Value("${nacos.serverAddr}") String serverAddr, @Value("${nacos.namespace.gateway}") String namespace) {
        try {
            this.serverAddr = serverAddr;
            this.namespace = namespace;

            Properties properties = new Properties();
            properties.setProperty("serverAddr", serverAddr);
            properties.setProperty("namespace", namespace);
            properties.setProperty("username", "zmt");
            properties.setProperty("password", "zmtpwd");
            namingService = NamingFactory.createNamingService(properties);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化实例缓存
     *
     * @param host
     */
    @Override
    public List<ServiceInstance> doRefresh(String host) {
        try {
            List<ServiceInstance> serverInstanceList = new ArrayList<>();
            List<Instance> nacosInstanceList = namingService.selectInstances(host, true);
            serverInstanceList = transferTo(nacosInstanceList);
            LOGGER.info("********** init host: {} server instatnces : {}", host, serverInstanceList.size());
            if(subscribeList.contains(host)){
                return serverInstanceList;
            }
            synchronized (this) { //线程安全的订阅方式，仅订阅一次
                if(subscribeList.contains(host)){
                    return serverInstanceList;
                } else {
                    namingService.subscribe(host, new NacosEventListener(host));
                    subscribeList.add(host);
                }
            }
            return serverInstanceList;

        } catch (NacosException e) {
            e.printStackTrace();
        }
        return null;
    }




    /**
     * nacos 的配置变更监听
     */
    private class NacosEventListener implements EventListener {
        private String serviceName;
        public NacosEventListener(String serviceName) {
            this.serviceName = serviceName;
        }
        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                List<Instance> nacosInstanceList = ((NamingEvent) event).getInstances();
                List<ServiceInstance> serviceInstanceList = transferTo(nacosInstanceList);
                //DEFAULT_GROUP@@fhh-api
                LOGGER.info("********** {} nacos update : {}", serviceName, serviceInstanceList.size());
                internalRefresh(serviceName, serviceInstanceList);
            }
        }
    }


    private List<ServiceInstance> transferTo(List<Instance> instanceList) {
        Objects.requireNonNull(instanceList);
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (Instance instance : instanceList) {
            serviceInstanceList.add(transferTo(instance));
        }
        return serviceInstanceList;
    }

    private ServiceInstance transferTo(Instance nacosInstance) {
        Objects.requireNonNull(nacosInstance);

        String instanceId = nacosInstance.getInstanceId();
        String ip = nacosInstance.getIp();
        int port = nacosInstance.getPort();
        ServiceInstance serviceInstance = new DefaultServiceInstance(instanceId, ip, port, false);
        return serviceInstance;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

}
