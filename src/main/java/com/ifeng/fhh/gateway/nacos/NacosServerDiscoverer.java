package com.ifeng.fhh.gateway.nacos;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Des: Nacos服务发现
 * @Author: jiangchuan
 * <p>
 * @Date: 20-6-15
 */
public abstract class NacosServerDiscoverer {


    private NamingService naming;

    private String serverName; /*要监听哪个服务*/

    private String clusterName; /*要监听哪个集群*/

    private String serverAddr; /*nacos地址*/

    private String namespace; /*整个相似服务的命名空间*/

    private AtomicReference<List<ServiceInstance>> availableIpList = new AtomicReference<>(new LinkedList<>());




    //订阅nacos
    public void subscribe(String serverName, String clusterName, String serverAddr, String namespace) throws Exception {
        this.serverName = serverName;
        this.clusterName = clusterName;
        this.serverAddr = serverAddr;
        this.namespace = namespace;

        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        properties.setProperty("namespace", namespace);
        properties.setProperty("clusterName", clusterName);
        properties.setProperty("username", "zmt");
        properties.setProperty("password", "zmtpwd");
        naming = NamingFactory.createNamingService(properties);
        List<Instance> instanceList = naming.selectInstances(serverName, true);
        availableIpList.set(transferTo(instanceList));
        naming.subscribe(serverName, new NacosEventListener());
    }


    /**
     * nacos 的配置变更监听
     */
    private class NacosEventListener implements EventListener {
        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                List<Instance> instanceList = ((NamingEvent) event).getInstances();
                availableIpList.set(transferTo(instanceList));
            }
        }
    }



    private List<ServiceInstance> transferTo(List<Instance> instanceList) {
        Objects.requireNonNull(instanceList);
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for(Instance instance : instanceList){
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
    public List<ServiceInstance> getCurrentServiceInstances() {
        return availableIpList.get();
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

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
}
