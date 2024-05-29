package cn.syx.gateway.web.handler;

import cn.syx.gateway.filter.GatewayFilter;
import cn.syx.gateway.plugin.DefaultGatewayPluginChain;
import cn.syx.gateway.plugin.GatewayPlugin;
import cn.syx.toolbox.base.CollectionTool;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component("gatewayWebHandler")
public class GatewayWebHandler implements WebHandler {

    @Autowired
    private List<GatewayPlugin> plugins;
    @Autowired
    private List<GatewayFilter> filters;

    @NotNull
    @Override
    public Mono<Void> handle(@NotNull ServerWebExchange exchange) {
        log.info("===> gateway web handler start");

        if (CollectionTool.isEmpty(plugins)) {
            String data = """
                    {"result": "no plugin"}
                    """;
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(data.getBytes(StandardCharsets.UTF_8))
            ));
        }

        for (GatewayFilter filter : filters) {
            filter.filter(exchange);
        }

        return new DefaultGatewayPluginChain(plugins).handle(exchange);
    }
}
