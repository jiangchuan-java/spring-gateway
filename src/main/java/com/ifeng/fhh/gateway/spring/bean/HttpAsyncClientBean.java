package com.ifeng.fhh.gateway.spring.bean;

import com.ifeng.fhh.gateway.util.httpclient.ApacheAsyncHttpClient;
import com.ifeng.fhh.gateway.util.httpclient.HttpClientTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-9-15
 */
@Configuration
public class HttpAsyncClientBean {

    @Bean
    public HttpClientTemplate buildTemplate() throws Exception{
        return new ApacheAsyncHttpClient();
    }

}
