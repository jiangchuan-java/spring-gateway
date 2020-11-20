package com.ifeng.fhh.gateway.filter.loadbalance_filter.discover;

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
public class NacosInstanceDiscoverer extends AbstractInstanceDiscover {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosInstanceDiscoverer.class);

    private NamingService namingService;


    private String serverAddr;

    private String namespace;


    private ConcurrentHashMap<String/*serverName*/, List<ServiceInstance>/*可用实例实例*/> serverInstanceCache = new ConcurrentHashMap<>();


    public NacosInstanceDiscoverer(@Value("${nacos.serverAddr}") String serverAddr, @Value("${nacos.namespace.gateway}") String namespace) {
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
    public void initServerInstanceCache(String host) {
        try {
            List<ServiceInstance> serverInstanceList = new ArrayList<>();
            List<Instance> nacosInstanceList = namingService.selectInstances(host, true);
            if(serverInstanceCache.containsKey(host)){
                return;
            }
            namingService.subscribe(host, new NacosEventListener(host));
            serverInstanceList = transferTo(nacosInstanceList);
            serverInstanceCache.put(host, serverInstanceList);

            LOGGER.info("init host: {} server instatnces : {}", host, serverInstanceList.size());
        } catch (NacosException e) {
            e.printStackTrace();
        }
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
                serverInstanceCache.put(serviceName, serviceInstanceList);
                LOGGER.info(" {} nacos update : {}", serviceName, serviceInstanceList.size());
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


    /**
     * 返回当前的实例列表
     *
     * @return
     */
    @Override
    public List<ServiceInstance> getCurrentServiceInstances(String host) {
        List<ServiceInstance> serverInstanceList = serverInstanceCache.get(host);
        if (Objects.isNull(serverInstanceList)) {
            try {
                List<Instance> nacosInstanceList = namingService.selectInstances(host, true);
                namingService.subscribe(host, new NacosEventListener(host));
                serverInstanceList = transferTo(nacosInstanceList);
                serverInstanceCache.put(host, serverInstanceList);
            } catch (NacosException e) {
                e.printStackTrace();
                return null;
            }
        }
        return serverInstanceList;
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
