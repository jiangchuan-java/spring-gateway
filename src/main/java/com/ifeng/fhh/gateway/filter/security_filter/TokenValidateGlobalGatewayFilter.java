package com.ifeng.fhh.gateway.filter.security_filter;

import com.ifeng.fhh.gateway.filter.OrderedGlobalFilter;
import com.ifeng.fhh.gateway.filter.security_filter.authorization.RoleInfoValidator;
import com.ifeng.fhh.gateway.util.GatewayPropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * 验证tokenFilter
 * @Author: jiangchuan
 * <p>
 * @Date: 20-12-11
 */
@Component
public class TokenValidateGlobalGatewayFilter extends OrderedGlobalFilter {


    private final HttpStatus statusCode = HttpStatus.UNAUTHORIZED;

    @Autowired
    private RoleInfoValidator validator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        String serviceId = route.getId();

        HttpHeaders headers = exchange.getRequest().getHeaders();

        String token = headers.getFirst(GatewayPropertyUtil.AUTHORITY_MANAGEMENT_SYSTEM_TOKEN);

        String uri = exchange.getRequest().getPath().value();

        return validator.validate(serviceId, uri, token).flatMap(allowed -> {

            if (allowed) {
                return chain.filter(exchange);
            }

            setResponseStatus(exchange, statusCode);
            return exchange.getResponse().setComplete();
        });
    }
}
