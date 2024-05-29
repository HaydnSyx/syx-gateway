package cn.syx.gateway.web.filter;

import cn.syx.toolbox.base.StringTool;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GatewayPreWebFilter implements WebFilter {

    @NotNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        log.info("===>>> Syx Gateway web filter ...");
        String mockParam = exchange.getRequest().getQueryParams().getFirst("mock");

        if (StringTool.isBlank(mockParam)) {
            return chain.filter(exchange);
        }

        String mock = """
                {"result": "mock"}
                """;
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(mock.getBytes()))
        );
    }
}
