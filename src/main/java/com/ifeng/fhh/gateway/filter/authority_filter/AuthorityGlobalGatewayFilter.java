package com.ifeng.fhh.gateway.filter.authority_filter;

import com.ifeng.fhh.gateway.filter.OrderedGlobalFilter;
import com.ifeng.fhh.gateway.filter.authority_filter.authority.AuthorityValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Des:
 *
 * ${filterName}GatewayFilterFactory
 *
 * 网关内，最好还是做全局的配置，具体的权限，还是由业务自己去控制吧
 *
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
@Component
public class AuthorityGlobalGatewayFilter extends OrderedGlobalFilter {


    private final String TOKEN = "Authorization";

    private HttpStatus statusCode = HttpStatus.UNAUTHORIZED;

    @Autowired
    private AuthorityValidator validator;

    public AuthorityValidator getValidator() {
        return validator;
    }

    public void setValidator(AuthorityValidator validator) {
        this.validator = validator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        String serviceId = route.getId();

        HttpHeaders headers = exchange.getRequest().getHeaders();

        String token = headers.getFirst(TOKEN);
        String path = exchange.getRequest().getPath().value();

        return validator.validate(serviceId, path, token).flatMap(allowed -> {

            if (allowed) {
                return chain.filter(exchange);
            }

            setResponseStatus(exchange, statusCode);
            return exchange.getResponse().setComplete();
        });
    }
}
