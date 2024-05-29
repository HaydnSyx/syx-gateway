package cn.syx.gateway.filter;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface GatewayFilter {

    Mono<Void> filter(ServerWebExchange exchange);
}
